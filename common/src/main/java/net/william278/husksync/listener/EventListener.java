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

import net.william278.husksync.HuskSync;
import net.william278.husksync.data.Data;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.util.Task;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * Handles what should happen when events are fired
 */
public abstract class EventListener {

    // The plugin instance
    protected final HuskSync plugin;

    /**
     * Set of UUIDs of "locked players", for which events will be canceled.
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
     * Handle a player joining the server (including players switching from another server on the network)
     *
     * @param user The {@link OnlineUser} to handle
     */
    protected final void handlePlayerJoin(@NotNull OnlineUser user) {
        if (user.isNpc()) {
            return;
        }
        lockedPlayers.add(user.getUuid());

        plugin.runAsyncDelayed(() -> {
            // Fetch from the database if the user isn't changing servers
            if (!plugin.getRedisManager().getUserServerSwitch(user)) {
                this.setUserFromDatabase(user);
                return;
            }

            // Set the user as soon as the source server has set the data to redis
            final long MAX_ATTEMPTS = 16L;
            final AtomicLong timesRun = new AtomicLong(0L);
            final AtomicReference<Task.Repeating> task = new AtomicReference<>();
            final Runnable runnable = () -> {
                if (user.isOffline()) {
                    task.get().cancel();
                    return;
                }
                if (disabling || timesRun.getAndIncrement() > MAX_ATTEMPTS) {
                    task.get().cancel();
                    this.setUserFromDatabase(user);
                    return;
                }

                plugin.getRedisManager().getUserData(user).ifPresent(redisData -> {
                    task.get().cancel();
                    user.applySnapshot(redisData);
                });
            };
            task.set(plugin.getRepeatingTask(runnable, 10));
            task.get().run();

        }, Math.max(0, plugin.getSettings().getNetworkLatencyMilliseconds() / 50L));
    }

    /**
     * Set a user's data from the database
     *
     * @param user The user to set the data for
     */
    private void setUserFromDatabase(@NotNull OnlineUser user) {
        plugin.getDatabase().getLatestDataSnapshot(user).ifPresentOrElse(
                user::applySnapshot, () -> user.completeSync(true, plugin)
        );
    }

    /**
     * Handle a player leaving the server (including players switching to another proxied server)
     *
     * @param user The {@link OnlineUser} to handle
     */
    protected final void handlePlayerQuit(@NotNull OnlineUser user) {
        // Players quitting have their data manually saved when the plugin is disabled
        if (disabling) {
            return;
        }

        // Don't sync players awaiting synchronization
        if (lockedPlayers.contains(user.getUuid()) || user.isNpc()) {
            return;
        }

        // Handle disconnection
        try {
            lockedPlayers.add(user.getUuid());
            plugin.getRedisManager().setUserServerSwitch(user).thenRun(() -> {
                final DataSnapshot.Packed data = user.createSnapshot(DataSnapshot.SaveCause.DISCONNECT);
                plugin.getRedisManager().setUserData(user, data);
                plugin.getDatabase().setUserData(user, data);
            });
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
                .filter(user -> !lockedPlayers.contains(user.getUuid()) && !user.isNpc())
                .forEach(user -> plugin.getDatabase().setUserData(
                        user, user.createSnapshot(DataSnapshot.SaveCause.WORLD_SAVE)
                ));
    }

    /**
     * Handles the saving of data when a player dies
     *
     * @param user  The user who died
     * @param drops The items that this user would have dropped
     */
    protected void saveOnPlayerDeath(@NotNull OnlineUser user, @NotNull Data.Items drops) {
        if (disabling || !plugin.getSettings().doSaveOnDeath() || lockedPlayers.contains(user.getUuid()) || user.isNpc()
                || (!plugin.getSettings().doSaveEmptyDropsOnDeath() && drops.isEmpty())) {
            return;
        }

        final DataSnapshot.Packed snapshot = user.createSnapshot(DataSnapshot.SaveCause.DEATH);
        snapshot.edit(plugin, (data -> data.getInventory().ifPresent(inventory -> inventory.setContents(drops))));
        plugin.getDatabase().setUserData(user, snapshot);
    }

    /**
     * Determine whether a player event should be canceled
     *
     * @param userUuid The UUID of the user to check
     * @return Whether the event should be canceled
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
                .filter(user -> !lockedPlayers.contains(user.getUuid()) && !user.isNpc())
                .forEach(user -> {
                    lockedPlayers.add(user.getUuid());
                    plugin.getDatabase().setUserData(user, user.createSnapshot(DataSnapshot.SaveCause.SERVER_SHUTDOWN));
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
