package net.william278.husksync.listener;

import net.william278.husksync.HuskSync;
import net.william278.husksync.data.DataSaveCause;
import net.william278.husksync.data.ItemData;
import net.william278.husksync.player.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * Handles what should happen when events are fired
 */
public abstract class EventListener {

    /**
     * The plugin instance
     */
    protected final HuskSync plugin;

    /**
     * Set of UUIDs of "locked players", for which events will be cancelled.
     * </p>
     * Players are locked while their items are being set (on join) or saved (on quit)
     */
    private final Set<UUID> lockedPlayers;

    /**
     * Whether the plugin is currently being disabled
     */
    private boolean disabling;

    protected EventListener(@NotNull HuskSync plugin) {
        this.plugin = plugin;
        this.lockedPlayers = new HashSet<>();
        this.disabling = false;
    }

    /**
     * Handle a player joining the server (including players switching from another proxied server)
     *
     * @param user The {@link OnlineUser} to handle
     */
    protected final void handlePlayerJoin(@NotNull OnlineUser user) {
        if (user.isNpc()) {
            return;
        }

        lockedPlayers.add(user.uuid);
        CompletableFuture.runAsync(() -> {
            try {
                // Hold reading data for the network latency threshold, to ensure the source server has set the redis key
                Thread.sleep(Math.max(0, plugin.getSettings().getNetworkLatencyMilliseconds()));
            } catch (InterruptedException e) {
                plugin.log(Level.SEVERE, "An exception occurred handling a player join", e);
            } finally {

                if (!plugin.getRedisManager().getUserServerSwitch(user)) {
                    // Fetch from the database if the user isn't changing servers
                    setUserFromDatabase(user);
                } else {
                    final int TIME_OUT_MILLISECONDS = 3200;
                    CompletableFuture.runAsync(() -> {
                        final AtomicInteger currentMilliseconds = new AtomicInteger(0);
                        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

                        // Set the user as soon as the source server has set the data to redis
                        executor.scheduleAtFixedRate(() -> {
                            if (user.isOffline()) {
                                executor.shutdown();
                                return;
                            }
                            if (disabling || currentMilliseconds.get() > TIME_OUT_MILLISECONDS) {
                                executor.shutdown();
                                setUserFromDatabase(user);
                                return;
                            }
                            plugin.getRedisManager().getUserData(user).ifPresent(redisData -> {
                                user.setData(redisData, plugin);
                                executor.shutdown();
                            });
                            currentMilliseconds.addAndGet(200);
                        }, 0, 200L, TimeUnit.MILLISECONDS);
                    });
                }
            }
        });
    }

    /**
     * Set a user's data from the database
     *
     * @param user The user to set the data for
     * @return Whether the data was successfully set
     */
    private boolean setUserFromDatabase(@NotNull OnlineUser user) {
        return plugin.getDatabase().getCurrentUserData(user)
                .map(userDataSnapshot -> {
                    user.setData(userDataSnapshot.userData(), plugin);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Handle a player leaving the server (including players switching to another proxied server)
     *
     * @param user The {@link OnlineUser} to handle
     */
    protected final void handlePlayerQuit(@NotNull OnlineUser user) {
        // Players quitting have their data manually saved by the plugin disable hook
        if (disabling) {
            return;
        }
        // Don't sync players awaiting synchronization
        if (lockedPlayers.contains(user.uuid) || user.isNpc()) {
            return;
        }

        // Handle disconnection
        try {
            lockedPlayers.add(user.uuid);
            plugin.getRedisManager().setUserServerSwitch(user)
                    .thenRun(() -> user.getUserData(plugin).ifPresent(userData -> plugin.getRedisManager()
                            .setUserData(user, userData).thenRun(() -> plugin.getDatabase()
                                    .setUserData(user, userData, DataSaveCause.DISCONNECT))));
        } catch (Throwable e) {
            plugin.log(Level.SEVERE, "An exception occurred handling a player disconnection", e);
        }
    }

    /**
     * Handles the saving of data when the world save event is fired
     *
     * @param usersInWorld a list of users in the world that is being saved
     */
    protected final void saveOnWorldSave(@NotNull List<OnlineUser> usersInWorld) {
        if (disabling || !plugin.getSettings().doSaveOnWorldSave()) {
            return;
        }
        usersInWorld.stream()
                .filter(user -> !lockedPlayers.contains(user.uuid) && !user.isNpc())
                .forEach(user -> user.getUserData(plugin)
                        .ifPresent(userData -> plugin.getDatabase()
                                .setUserData(user, userData, DataSaveCause.WORLD_SAVE)));
    }

    /**
     * Handles the saving of data when a player dies
     *
     * @param user  The user who died
     * @param drops The items that this user would have dropped
     */
    protected void saveOnPlayerDeath(@NotNull OnlineUser user, @NotNull ItemData drops) {
        if (disabling || !plugin.getSettings().doSaveOnDeath() || lockedPlayers.contains(user.uuid) || user.isNpc()
                || (!plugin.getSettings().doSaveEmptyDropsOnDeath() && drops.isEmpty())) {
            return;
        }

        user.getUserData(plugin)
                .ifPresent(userData -> {
                    userData.getInventory().orElse(ItemData.empty()).serializedItems = drops.serializedItems;
                    plugin.getDatabase().setUserData(user, userData, DataSaveCause.DEATH);
                });
    }

    /**
     * Determine whether a player event should be cancelled
     *
     * @param userUuid The UUID of the user to check
     * @return Whether the event should be cancelled
     */
    protected final boolean cancelPlayerEvent(@NotNull UUID userUuid) {
        return disabling || lockedPlayers.contains(userUuid);
    }

    /**
     * Handle the plugin disabling
     */
    public final void handlePluginDisable() {
        disabling = true;

        // Save data for all online users
        plugin.getOnlineUsers().stream()
                .filter(user -> !lockedPlayers.contains(user.uuid) && !user.isNpc())
                .forEach(user -> {
                    lockedPlayers.add(user.uuid);
                    user.getUserData(plugin)
                            .ifPresent(userData -> plugin.getDatabase()
                                    .setUserData(user, userData, DataSaveCause.SERVER_SHUTDOWN));
                });

        // Close outstanding connections
        plugin.getDatabase().terminate();
        plugin.getRedisManager().terminate();
    }

    public final Set<UUID> getLockedPlayers() {
        return this.lockedPlayers;
    }

    /**
     * Represents priorities for events that HuskSync listens to
     */
    public enum Priority {
        /**
         * Listens and processes the event execution last
         */
        HIGHEST,
        /**
         * Listens in between {@link #HIGHEST} and {@link #LOWEST} priority marked
         */
        NORMAL,
        /**
         * Listens and processes the event execution first
         */
        LOWEST
    }

    /**
     * Represents events that HuskSync listens to, with a configurable priority listener
     */
    public enum ListenerType {
        JOIN_LISTENER(Priority.LOWEST),
        QUIT_LISTENER(Priority.LOWEST),
        DEATH_LISTENER(Priority.NORMAL);

        private final Priority defaultPriority;

        ListenerType(@NotNull EventListener.Priority defaultPriority) {
            this.defaultPriority = defaultPriority;
        }

        @NotNull
        private Map.Entry<String, String> toEntry() {
            return Map.entry(name().toLowerCase(), defaultPriority.name());
        }


        @SuppressWarnings("unchecked")
        @NotNull
        public static Map<String, String> getDefaults() {
            return Map.ofEntries(Arrays.stream(values())
                    .map(ListenerType::toEntry)
                    .toArray(Map.Entry[]::new));
        }
    }
}
