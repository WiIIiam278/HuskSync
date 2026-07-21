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

import java.time.Duration;
import java.util.*;
import java.util.logging.Level;

import static net.william278.husksync.config.Settings.SynchronizationSettings.SaveOnDeathSettings;

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
        plugin.getDisconnectingPlayers().remove(user.getUuid());
        if (user.isNpc()) {
            return;
        }
        plugin.lockPlayer(user.getUuid());
        plugin.getDataSyncer().syncApplyUserData(user);
    }

    /**
     * Handle a player leaving the server (including players switching to another proxied server)
     *
     * @param user The {@link OnlineUser} to handle
     */
    protected final void handlePlayerQuit(@NotNull OnlineUser user) {
        if (user.isNpc()) {
            return;
        }
        plugin.getDisconnectingPlayers().add(user.getUuid());

        if (!plugin.isLocked(user.getUuid())) {
            plugin.lockPlayer(user.getUuid());
            if (plugin.isDisabling()) {
                plugin.log(Level.INFO, String.format(
                        "Saving data for %s synchronously during server shutdown",
                        user.getName()));
                plugin.getDataSyncer().saveCurrentUserData(user, DataSnapshot.SaveCause.SERVER_SHUTDOWN);
                plugin.unlockPlayer(user.getUuid());
            } else {
                plugin.getDataSyncer().syncSaveUserData(user);
            }
        } else {
            final String message = String.format("[%s] disconnected while locked - data will NOT be saved!",
                    user.getName());
            if (plugin.isDisabling()) {
                plugin.log(Level.WARNING, message);
            } else {
                plugin.debug(message);
            }
        }
    }

    /**
     * Handles the saving of data when the world save event is fired
     *
     * @param usersInWorld a list of users in the world that is being saved
     */
    protected final void saveOnWorldSave(@NotNull List<OnlineUser> usersInWorld) {
        if (plugin.isDisabling() || !plugin.getSettings().getSynchronization().isSaveOnWorldSave()) {
            return;
        }
        usersInWorld.stream()
                .filter(user -> !user.isNpc() && !user.hasDisconnected() && !plugin.isLocked(user.getUuid()))
                .forEach(user -> plugin.getDataSyncer().saveCurrentUserData(
                        user, DataSnapshot.SaveCause.WORLD_SAVE
                ));
    }

    /**
     * Handles the saving of data when a player dies
     *
     * @param user  The user who died
     * @param items The items that should be saved for this user on their death
     */
    protected void saveOnPlayerDeath(@NotNull OnlineUser user, @NotNull Data.Items items) {
        final SaveOnDeathSettings settings = plugin.getSettings().getSynchronization().getSaveOnDeath();
        if (plugin.isDisabling() || !settings.isEnabled() || plugin.isLocked(user.getUuid())
                || user.isNpc() || (!settings.isSaveEmptyItems() && items.isEmpty())) {
            return;
        }

        // We don't persist this to Redis for syncing, as this snapshot is from a state they won't be in post-respawn
        final DataSnapshot.Packed snapshot = user.createSnapshot(DataSnapshot.SaveCause.DEATH);
        snapshot.edit(plugin, (data -> data.getInventory().ifPresent(inv -> inv.setContents(items))));
        plugin.getDataSyncer().saveData(user, snapshot);
    }


    /**
     * Handle the plugin disabling.
     * <p>
     * Waits for in-flight async saves to complete, then saves all online players synchronously.
     * Resource cleanup (DB/Redis) is handled by the platform {@code onDisable()} method.
     */
    public void handlePluginDisable() {
        // Await any pending async saves from players who disconnected before shutdown
        plugin.getDataSyncer().awaitPendingSaves(Duration.ofSeconds(
                plugin.getSettings().getSynchronization().getShutdownSaveTimeoutSeconds()
        ));

        // Save all online players that are not currently locked
        final List<UUID> lockedPlayers = new ArrayList<>();
        plugin.getOnlineUsers().stream()
                .filter(user -> !user.isNpc())
                .forEach(user -> {
                    if (!plugin.isLocked(user.getUuid())) {
                        plugin.lockPlayer(user.getUuid());
                        try {
                            plugin.getDataSyncer().saveCurrentUserData(user,
                                    DataSnapshot.SaveCause.SERVER_SHUTDOWN);
                        } catch (Exception e) {
                            plugin.log(Level.WARNING, String.format(
                                    "Failed to save data for %s during shutdown: %s",
                                    user.getName(), e.getMessage()));
                        }
                    } else {
                        lockedPlayers.add(user.getUuid());
                    }
                });

        // Retry locked players with a short spin-wait before giving up
        if (!lockedPlayers.isEmpty()) {
            plugin.log(Level.WARNING, String.format(
                    "%d player(s) were locked during shutdown; retrying...", lockedPlayers.size()));
            for (UUID uuid : lockedPlayers) {
                for (int i = 0; i < 3; i++) {
                    if (!plugin.isLocked(uuid)) {
                        plugin.lockPlayer(uuid);
                        plugin.getOnlineUser(uuid).ifPresent(user -> {
                            try {
                                plugin.getDataSyncer().saveCurrentUserData(user,
                                        DataSnapshot.SaveCause.SERVER_SHUTDOWN);
                            } catch (Exception e) {
                                plugin.log(Level.WARNING, String.format(
                                        "Failed to save data for %s during locked-player retry: %s",
                                        user.getName(), e.getMessage()));
                            }
                        });
                        break;
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                if (plugin.isLocked(uuid)) {
                    plugin.log(Level.WARNING, String.format(
                            "Player %s was still locked during shutdown and could not be saved. Data may not have persisted!",
                            uuid));
                }
            }
        }
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
