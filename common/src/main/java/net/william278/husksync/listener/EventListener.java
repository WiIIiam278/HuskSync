package net.william278.husksync.listener;

import net.william278.husksync.HuskSync;
import net.william278.husksync.config.Settings;
import net.william278.husksync.data.ItemData;
import net.william278.husksync.data.DataSaveCause;
import net.william278.husksync.data.UserData;
import net.william278.husksync.player.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public abstract class EventListener {

    /**
     * The plugin instance
     */
    protected final HuskSync huskSync;

    /**
     * Set of UUIDs current awaiting item synchronization. Events will be cancelled for these users
     */
    private final HashSet<UUID> usersAwaitingSync;

    /**
     * Whether the plugin is currently being disabled
     */
    private boolean disabling;

    protected EventListener(@NotNull HuskSync huskSync) {
        this.huskSync = huskSync;
        this.usersAwaitingSync = new HashSet<>();
        this.disabling = false;
    }

    public final void handlePlayerJoin(@NotNull OnlineUser user) {
        if (user.isDead()) {
            return;
        }
        usersAwaitingSync.add(user.uuid);
        CompletableFuture.runAsync(() -> {
            try {
                // Hold reading data for the network latency threshold, to ensure the source server has set the redis key
                Thread.sleep(Math.min(0, huskSync.getSettings().getIntegerValue(Settings.ConfigOption.SYNCHRONIZATION_NETWORK_LATENCY_MILLISECONDS)));
            } catch (InterruptedException e) {
                huskSync.getLoggingAdapter().log(Level.SEVERE, "An exception occurred handling a player join", e);
            } finally {
                huskSync.getRedisManager().getUserServerSwitch(user).thenAccept(changingServers -> {
                    huskSync.getLoggingAdapter().info("Handling server change check " + ((changingServers) ? "true" : "false"));
                    if (!changingServers) {
                        huskSync.getLoggingAdapter().info("User is not changing servers");
                        // Fetch from the database if the user isn't changing servers
                        setUserFromDatabase(user).thenRun(() -> handleSynchronisationCompletion(user));
                    } else {
                        huskSync.getLoggingAdapter().info("User is changing servers, setting from db");
                        final int TIME_OUT_MILLISECONDS = 3200;
                        CompletableFuture.runAsync(() -> {
                            final AtomicInteger currentMilliseconds = new AtomicInteger(0);
                            final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

                            // Set the user as soon as the source server has set the data to redis
                            executor.scheduleAtFixedRate(() -> {
                                if (user.isOffline()) {
                                    executor.shutdown();
                                    huskSync.getLoggingAdapter().info("Cancelled sync, user gone offline!");
                                    return;
                                }
                                if (disabling || currentMilliseconds.get() > TIME_OUT_MILLISECONDS) {
                                    executor.shutdown();
                                    setUserFromDatabase(user).thenRun(() -> handleSynchronisationCompletion(user));
                                    huskSync.getLoggingAdapter().info("Setting user from db as fallback");
                                    return;
                                }
                                huskSync.getRedisManager().getUserData(user).thenAccept(redisUserData ->
                                        redisUserData.ifPresent(redisData -> {
                                            huskSync.getLoggingAdapter().info("Setting user from redis!");
                                            user.setData(redisData, huskSync.getSettings(), huskSync.getEventCannon())
                                                    .thenRun(() -> handleSynchronisationCompletion(user)).join();
                                            executor.shutdown();
                                        })).join();
                                currentMilliseconds.addAndGet(200);
                            }, 0, 200L, TimeUnit.MILLISECONDS);
                        });
                    }
                });
            }
        });
    }

    private CompletableFuture<Void> setUserFromDatabase(@NotNull OnlineUser user) {
        return huskSync.getDatabase().getCurrentUserData(user)
                .thenAccept(databaseUserData -> databaseUserData.ifPresent(databaseData ->
                        user.setData(databaseData.userData(), huskSync.getSettings(),
                                huskSync.getEventCannon()).join()));
    }

    private void handleSynchronisationCompletion(@NotNull OnlineUser user) {
        huskSync.getLocales().getLocale("synchronisation_complete").ifPresent(user::sendActionBar);
        usersAwaitingSync.remove(user.uuid);
        huskSync.getDatabase().ensureUser(user).join();
        huskSync.getEventCannon().fireSyncCompleteEvent(user);
    }

    public final void handlePlayerQuit(@NotNull OnlineUser user) {
        // Players quitting have their data manually saved by the plugin disable hook
        if (disabling) {
            return;
        }
        // Don't sync players awaiting synchronization
        if (usersAwaitingSync.contains(user.uuid)) {
            return;
        }
        huskSync.getRedisManager().setUserServerSwitch(user).thenRun(() -> user.getUserData().thenAccept(
                userData -> huskSync.getRedisManager().setUserData(user, userData).thenRun(
                        () -> huskSync.getDatabase().setUserData(user, userData, DataSaveCause.DISCONNECT).join())));
        usersAwaitingSync.remove(user.uuid);
    }

    public final void handleWorldSave(@NotNull List<OnlineUser> usersInWorld) {
        if (disabling || !huskSync.getSettings().getBooleanValue(Settings.ConfigOption.SYNCHRONIZATION_SAVE_ON_WORLD_SAVE)) {
            return;
        }
        CompletableFuture.runAsync(() -> usersInWorld.forEach(user ->
                huskSync.getDatabase().setUserData(user, user.getUserData().join(), DataSaveCause.WORLD_SAVE).join()));
    }

    public final void handlePluginDisable() {
        disabling = true;

        huskSync.getOnlineUsers().stream().filter(user -> !usersAwaitingSync.contains(user.uuid)).forEach(user ->
                huskSync.getDatabase().setUserData(user, user.getUserData().join(), DataSaveCause.SERVER_SHUTDOWN).join());

        huskSync.getDatabase().close();
        huskSync.getRedisManager().close();
    }

    public final void handleMenuClose(@NotNull OnlineUser user, @NotNull ItemData menuInventory) {
        if (disabling) {
            return;
        }
        huskSync.getDataEditor().closeInventoryMenu(user, menuInventory);
    }

    public final boolean cancelMenuClick(@NotNull OnlineUser user) {
        if (disabling) {
            return true;
        }
        return huskSync.getDataEditor().cancelInventoryEdit(user);
    }

    public final boolean cancelPlayerEvent(@NotNull OnlineUser user) {
        return disabling || usersAwaitingSync.contains(user.uuid);
    }

}
