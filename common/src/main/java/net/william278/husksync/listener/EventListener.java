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
import net.william278.husksync.data.Identifier;
import net.william278.husksync.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import static net.william278.husksync.config.Settings.SynchronizationSettings.SaveOnDeathSettings;

/**
 * Handles what should happen when events are fired
 */
public abstract class EventListener {

    // The plugin instance
    protected final HuskSync plugin;

    // Players to save once other plugins have handled them quitting
    private final Set<UUID> quitSaves = ConcurrentHashMap.newKeySet();

    // Players whose data was saved on quit, to catch them dying afterwards
    private final Set<UUID> savedOnQuit = ConcurrentHashMap.newKeySet();

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
        savedOnQuit.remove(user.getUuid());
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
        // Check the user is a user, the plugin isn't disabling, then mark as disconnecting
        if (user.isNpc() || plugin.isDisabling()) {
            return;
        }
        plugin.getDisconnectingPlayers().add(user.getUuid());

        // Lock, then mark their data to be saved if the user is unlocked
        if (!plugin.isLocked(user.getUuid())) {
            plugin.lockPlayer(user.getUuid());
            quitSaves.add(user.getUuid());
        } else {
            plugin.debug(String.format("[%s] disconnected while locked - data will NOT be saved!",
                    user.getName()));
        }
    }

    /**
     * Save the data of a player who quit, once other plugins have handled them quitting
     * <p>
     * If the player is dead and health is synced, their death is cleared from this server's own player data after
     * the snapshot is taken. Otherwise, rejoining this server later would kill them again, even with alive synced
     * data.
     *
     * @param user The {@link OnlineUser} who quit
     */
    protected final void saveOnPlayerQuit(@NotNull OnlineUser user) {
        if (user.isNpc()) {
            return;
        }
        if (quitSaves.remove(user.getUuid())) {
            plugin.getDataSyncer().syncSaveUserData(user);
        }

        // Any death after this is part of the same quit, so stop tracking them once it's finished
        savedOnQuit.add(user.getUuid());
        plugin.runSync(() -> savedOnQuit.remove(user.getUuid()));
        clearLocalDeathState(user);
    }

    /**
     * Handle a player dying while they quit, after their data was saved by {@link #saveOnPlayerQuit}, e.g. killed
     * by a combat logging plugin that handles quits after HuskSync. They're locked by then, so the death drops
     * nothing, but their saved data won't include the death either. Their local death state is cleared, as it
     * would have been had they died before the save.
     *
     * @param user The {@link OnlineUser} who died
     */
    protected final void handlePlayerDeathAfterQuitSave(@NotNull OnlineUser user) {
        if (!savedOnQuit.contains(user.getUuid()) || !user.isDead()) {
            return;
        }
        plugin.log(Level.WARNING, String.format("[%s] was killed after their data was saved on quit, so their "
                + "synced data won't include this death. Another plugin is handling quits after HuskSync, "
                + "enable debug logging to see which.", user.getName()));
        clearLocalDeathState(user);
    }

    /**
     * Clears a player's death from the server's player data, so they don't die again when they next join
     * <p>
     * Must be cleared after health is synced, otherwise the death isn't saved and the player would skip respawning
     *
     * @param user the {@link OnlineUser} to clear a death for
     */
    private void clearLocalDeathState(@NotNull OnlineUser user) {
        if (!user.isDead() || !plugin.getSettings().getSynchronization().isFeatureEnabled(Identifier.HEALTH)) {
            return;
        }
        plugin.debug(String.format("[%s] left while dead, clearing their local death state", user.getName()));
        user.clearLocalDeathState();
        if (user.isDead()) {
            plugin.log(Level.WARNING, String.format("[%s] failed to clear local death state, they may be stuck on "
                    + "the death screen when they next join this server", user.getName()));
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
     * Handle the plugin disabling
     */
    public void handlePluginDisable() {
        // Save all online players that haven't been processed by PlayerQuitEvent yet
        plugin.getOnlineUsers().stream()
                .filter(user -> !plugin.isLocked(user.getUuid()) && !user.isNpc())
                .forEach(user -> {
                    plugin.lockPlayer(user.getUuid());
                    plugin.getDataSyncer().saveCurrentUserData(user, DataSnapshot.SaveCause.SERVER_SHUTDOWN);
                });

        // Clear any in-limbo deaths, as players are saved by the server after plugins disable
        plugin.getOnlineUsers().stream()
                .filter(user -> !user.isNpc())
                .forEach(this::clearLocalDeathState);

        // Wait for the in-progress async saves queued during shutdown:
        // - DISCONNECT saves for players leaving before the server stopped
        // - SERVER_SHUTDOWN saves queued above, for players still online
        // - WORLD_SAVE saves still in queue
        // These saves run asynchronously and must complete before closing DB/Redis connections
        plugin.getDataSyncer().awaitPendingSaves();
    }

    /**
     * Close database and Redis connections. Must run AFTER {@link #handlePluginDisable()}
     * and AFTER {@code dataSyncer.terminate()}, since the latter clears checkout keys via Redis.
     */
    public void closeConnections() {
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
        LOWEST,
        /**
         * Listens and processes the event execution last, after all other priorities. Not recommended, as other
         * plugins may only expect to observe the event here, and won't see any changes HuskSync makes
         *
         * @since 4.1.0
         */
        MONITOR
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
