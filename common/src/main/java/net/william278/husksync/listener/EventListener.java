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
import net.william278.husksync.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles what should happen when events are fired
 */
public abstract class EventListener {

    // The plugin instance
    protected final HuskSync plugin;

    protected EventListener(@NotNull HuskSync plugin) {
        this.plugin = plugin;
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
        plugin.lockPlayer(user.getUuid());
        plugin.getDataSyncer().setUserData(user);
    }

    /**
     * Handle a player leaving the server (including players switching to another proxied server)
     *
     * @param user The {@link OnlineUser} to handle
     */
    protected final void handlePlayerQuit(@NotNull OnlineUser user) {
        if (user.isNpc() || plugin.isDisabling() || plugin.isLocked(user.getUuid())) {
            return;
        }
        plugin.lockPlayer(user.getUuid());
        plugin.runAsync(() -> plugin.getDataSyncer().saveUserData(user));
    }

    /**
     * Handles the saving of data when the world save event is fired
     *
     * @param usersInWorld a list of users in the world that is being saved
     */
    protected final void saveOnWorldSave(@NotNull List<OnlineUser> usersInWorld) {
        if (plugin.isDisabling() || !plugin.getSettings().doSaveOnWorldSave()) {
            return;
        }
        usersInWorld.stream()
                .filter(user -> !plugin.isLocked(user.getUuid()) && !user.isNpc())
                .forEach(user -> plugin.getDatabase().addSnapshot(
                        user, user.createSnapshot(DataSnapshot.SaveCause.WORLD_SAVE)
                ));
    }

    /**
     * Handles the saving of data when a player dies
     *
     * @param user  The user who died
     * @param items The items that should be saved for this user on their death
     */
    protected void saveOnPlayerDeath(@NotNull OnlineUser user, @NotNull Data.Items items) {
        if (plugin.isDisabling() || !plugin.getSettings().doSaveOnDeath() || plugin.isLocked(user.getUuid())
                || user.isNpc() || (!plugin.getSettings().doSaveEmptyDeathItems() && items.isEmpty())) {
            return;
        }

        final DataSnapshot.Packed snapshot = user.createSnapshot(DataSnapshot.SaveCause.DEATH);
        snapshot.edit(plugin, (data -> data.getInventory().ifPresent(inventory -> inventory.setContents(items))));
        plugin.getDatabase().addSnapshot(user, snapshot);
    }

    /**
     * Determine whether a player event should be canceled
     *
     * @param userUuid The UUID of the user to check
     * @return Whether the event should be canceled
     */
    protected final boolean cancelPlayerEvent(@NotNull UUID userUuid) {
        return plugin.isDisabling() || plugin.isLocked(userUuid);
    }

    /**
     * Handle the plugin disabling
     */
    public final void handlePluginDisable() {
        // Save for all online players
        plugin.getOnlineUsers().stream()
                .filter(user -> !plugin.isLocked(user.getUuid()) && !user.isNpc())
                .forEach(user -> {
                    plugin.lockPlayer(user.getUuid());
                    plugin.getDatabase().addSnapshot(user, user.createSnapshot(DataSnapshot.SaveCause.SERVER_SHUTDOWN));
                });

        // Close outstanding connections
        plugin.getDatabase().terminate();
        plugin.getRedisManager().terminate();
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
