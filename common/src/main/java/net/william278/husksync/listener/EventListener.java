/*
 * This file is part of HuskSync, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.husksync.listener;

import de.themoep.minedown.adventure.MineDown;
import net.william278.husksync.HuskSync;
import net.william278.husksync.data.DataSaveCause;
import net.william278.husksync.data.ItemData;
import net.william278.husksync.player.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
                plugin.getRedisManager().getUserServerSwitch(user).thenAccept(changingServers -> {
                    if (!changingServers) {
                        // Fetch from the database if the user isn't changing servers
                        setUserFromDatabase(user).thenAccept(succeeded -> handleSynchronisationCompletion(user, succeeded));
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
                                    setUserFromDatabase(user).thenAccept(
                                            succeeded -> handleSynchronisationCompletion(user, succeeded));
                                    return;
                                }
                                plugin.getRedisManager().getUserData(user).thenAccept(redisUserData ->
                                        redisUserData.ifPresent(redisData -> {
                                            user.setData(redisData, plugin)
                                                    .thenAccept(succeeded -> handleSynchronisationCompletion(user, succeeded)).join();
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

    /**
     * Set a user's data from the database
     *
     * @param user The user to set the data for
     * @return Whether the data was successfully set
     */
    private CompletableFuture<Boolean> setUserFromDatabase(@NotNull OnlineUser user) {
        return plugin.getDatabase().getCurrentUserData(user).thenApply(databaseUserData -> {
            if (databaseUserData.isPresent()) {
                return user.setData(databaseUserData.get().userData(), plugin).join();
            }
            return true;
        });
    }

    /**
     * Handle a player's synchronization completion
     *
     * @param user      The {@link OnlineUser} to handle
     * @param succeeded Whether the synchronization succeeded
     */
    private void handleSynchronisationCompletion(@NotNull OnlineUser user, boolean succeeded) {
        if (succeeded) {
            switch (plugin.getSettings().getNotificationDisplaySlot()) {
                case CHAT -> plugin.getLocales().getLocale("synchronisation_complete")
                        .ifPresent(user::sendMessage);
                case ACTION_BAR -> plugin.getLocales().getLocale("synchronisation_complete")
                        .ifPresent(user::sendActionBar);
                case TOAST -> plugin.getLocales().getLocale("synchronisation_complete")
                        .ifPresent(locale -> user.sendToast(locale, new MineDown(""),
                                "minecraft:bell", "TASK"));
            }
            plugin.getDatabase().ensureUser(user).join();
            lockedPlayers.remove(user.uuid);
            plugin.getEventCannon().fireSyncCompleteEvent(user);
        } else {
            plugin.getLocales().getLocale("synchronisation_failed")
                    .ifPresent(user::sendMessage);
            plugin.getDatabase().ensureUser(user).join();
        }
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

        // Handle asynchronous disconnection
        lockedPlayers.add(user.uuid);
        CompletableFuture.runAsync(() -> plugin.getRedisManager().setUserServerSwitch(user)
                .thenRun(() -> user.getUserData(plugin).thenAccept(
                        optionalUserData -> optionalUserData.ifPresent(userData -> plugin.getRedisManager()
                                .setUserData(user, userData).thenRun(() -> plugin.getDatabase()
                                        .setUserData(user, userData, DataSaveCause.DISCONNECT)))))
                .exceptionally(throwable -> {
                    plugin.log(Level.SEVERE,
                            "An exception occurred handling a player disconnection");
                    throwable.printStackTrace();
                    return null;
                }).join());
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
                        .thenAccept(data -> data.ifPresent(userData -> plugin.getDatabase()
                                .setUserData(user, userData, DataSaveCause.WORLD_SAVE))));
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
                .thenAccept(data -> data.ifPresent(userData -> {
                    userData.getInventory().orElse(ItemData.empty()).serializedItems = drops.serializedItems;
                    plugin.getDatabase().setUserData(user, userData, DataSaveCause.DEATH);
                }));
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
                    user.getUserData(plugin).join()
                            .ifPresent(userData -> plugin.getDatabase()
                                    .setUserData(user, userData, DataSaveCause.SERVER_SHUTDOWN).join());
                });

        // Close outstanding connections
        plugin.getDatabase().close();
        plugin.getRedisManager().close();
    }

    public final Set<UUID> getLockedPlayers() {
        return this.lockedPlayers;
    }

}
