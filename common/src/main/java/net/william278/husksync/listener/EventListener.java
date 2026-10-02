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
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static net.william278.husksync.config.Settings.SynchronizationSettings.SaveOnDeathSettings;

/**
 * Handles what should happen when events are fired
 */
public abstract class EventListener {

    // The plugin instance
    protected final HuskSync plugin;

    // Players to save once other plugins have handled them quitting
    private final Set<UUID> quitSaves = ConcurrentHashMap.newKeySet();

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
        if (user.isNpc() || plugin.isDisabling()) {
            return;
        }
        plugin.getDisconnectingPlayers().add(user.getUuid());
        lockAndSaveOnQuit(user);
    }

    /**
     * Mark a user as disconnecting as early as possible - as soon as {@code PlayerQuitEvent} starts firing,
     * regardless of what priority {@link ListenerType#QUIT_LISTENER} is configured for.
     * <p>
     * This lets other code reliably tell that a quit is already in progress for a user - in particular,
     * {@link #forceRespawnIfDeadMidQuit}'s {@code PlayerDeathEvent} backstop uses this to recognise a kill that
     * happens as part of the same quit (e.g. from a PvP/anti-combat-logout plugin), no matter what priority
     * that plugin's own listener runs at.
     *
     * @param user the user who is disconnecting
     * @since 4.1.0
     */
    protected final void markDisconnecting(@NotNull OnlineUser user) {
        if (user.isNpc() || plugin.isDisabling()) {
            return;
        }
        plugin.getDisconnectingPlayers().add(user.getUuid());
        plugin.debug(String.format(
                "[%s] respawnAtDisconnectIfDead: marked as disconnecting early (quit in progress)", user.getName()));
    }

    /**
     * Lock a user and dispatch their disconnect-save, unless they're already locked.
     * <p>
     * While {@code respawnAtDisconnectIfDead} is enabled, this is only ever called once per quit - from
     * {@code onPlayerQuitRespawnWorkaround} at Bukkit's {@code MONITOR} priority instead of from the normal,
     * configurable {@link ListenerType#QUIT_LISTENER} priority - specifically so it runs after any
     * PvP/anti-combat-logout plugin's own kill-on-quit listener, whatever priority that uses (with one
     * exception: a killer that itself uses {@code MONITOR}, which {@link #forceRespawnIfDeadMidQuit} backstops
     * separately - see there for why). The {@code isLocked()} check below is therefore mostly a defensive
     * guard against unrelated double-fires (e.g. plugin reloads), not a race this method is expected to lose.
     * <p>
     * If the workaround is enabled and the user is still dead at this point, this captures their (dead)
     * snapshot before saving, then forces a local respawn - the snapshot must be captured first, since
     * respawning would otherwise cause a snapshot built afterward to incorrectly reflect them as alive.
     *
     * @param user the user who is disconnecting
     * @since 4.1.0
     */
    protected final void lockAndSaveOnQuit(@NotNull OnlineUser user) {
        if (plugin.isLocked(user.getUuid())) {
            plugin.debug(String.format("[%s] disconnected while locked - data will NOT be saved!",
                    user.getName()));
            return;
        }
        plugin.lockPlayer(user.getUuid());
        final DataSnapshot.Packed precomputedSnapshot = respawnAtDisconnectIfDead(user);
        plugin.debug(String.format("[%s] lockAndSaveOnQuit: locked, dispatching disconnect-save (precomputed "
                + "dead/respawned snapshot: %s)", user.getName(), precomputedSnapshot != null));
        plugin.getDataSyncer().syncSaveUserData(user, precomputedSnapshot);
    }

    /**
     * Workaround for PvP/anti-combat-logout plugins (e.g., CombatLogX or PvPManager's "kill on quit" punishment)
     * that kill a player as part of quit handling, but never give them the chance to respawn locally before they
     * disconnect. Left alone, this server's own local player data would stay stuck mid-death, so reconnecting
     * to *this* server later shows the death screen again.
     * <p>
     * Called from {@link #lockAndSaveOnQuit}, right before that method's own disconnect-save, so this is the
     * primary path: by the time it runs (Bukkit {@code MONITOR} priority), any kill applied at a lower priority
     * has already happened. If enabled and the user is still dead at this point, this captures their (dead)
     * snapshot now, then forces a local respawn immediately - the snapshot must be captured first, since
     * respawning would otherwise cause a snapshot built afterward to incorrectly reflect them as alive.
     * <p>
     * If a same-tier {@code MONITOR} killer's kill hasn't happened yet when this runs, {@link
     * #forceRespawnIfDeadMidQuit} will already have handled the respawn once that kill's {@code
     * PlayerDeathEvent} fires - see that method for why the two don't conflict.
     *
     * @param user the user who is disconnecting
     * @return the pre-respawn snapshot to save, or {@code null} if the workaround did not apply
     */
    @Nullable
    private DataSnapshot.Packed respawnAtDisconnectIfDead(@NotNull OnlineUser user) {
        if (!plugin.getSettings().getSynchronization().isRespawnAtDisconnectIfDead()) {
            return null;
        }
        if (!user.isDead()) {
            plugin.debug(String.format(
                    "[%s] respawnAtDisconnectIfDead: not dead at disconnect, nothing to do", user.getName()));
            return null;
        }
        plugin.debug(String.format(
                "[%s] respawnAtDisconnectIfDead: dead at disconnect - capturing snapshot before forcing local respawn",
                user.getName()));
        final DataSnapshot.Packed snapshot = user.createSnapshot(DataSnapshot.SaveCause.DISCONNECT);
        user.respawn();
        plugin.debug(String.format(
                "[%s] respawnAtDisconnectIfDead: forced local respawn (still dead afterward: %s)",
                user.getName(), user.isDead()));
        return snapshot;
    }

    /**
     * Backstop for {@link #respawnAtDisconnectIfDead}, for a PvP/anti-combat-logout plugin that kills the player
     * at Bukkit's {@code MONITOR} priority itself (e.g. CombatLogX) - the same tier {@code
     * onPlayerQuitRespawnWorkaround} uses for {@link #lockAndSaveOnQuit}. Same-tier ordering between two
     * different plugins' listeners isn't guaranteed by Bukkit, so that kill may fire before or after the
     * disconnect-save's own dead-check.
     * <p>
     * Reacting to the {@code PlayerDeathEvent} the kill itself causes sidesteps the ordering problem entirely:
     * the kill is what causes this event to fire, so by the time this runs, the kill has necessarily already
     * happened, regardless of listener priority or plugin load order. This deliberately does <b>not</b> check
     * {@code isLocked()} the way {@link #lockAndSaveOnQuit} does - fixing the player's local (mid-death) state
     * doesn't need to wait its turn the way the one-shot network save does, and calling this after {@link
     * #respawnAtDisconnectIfDead} already ran is harmless, since {@link OnlineUser#respawn()} is a no-op once
     * they're no longer dead.
     * <p>
     * One known gap: if this runs first (i.e. the disconnect-save's own {@code MONITOR} handler hasn't fired
     * yet), the player's local data still ends up correctly respawned, but the network-synced snapshot taken
     * moments later will reflect them as alive again rather than reflecting this kill - the same as it would
     * without this workaround at all. Only the local "stuck mid-death" corruption is guaranteed fixed here.
     *
     * @param user the user who died while already disconnecting
     * @since 4.1.0
     */
    protected final void forceRespawnIfDeadMidQuit(@NotNull OnlineUser user) {
        if (!plugin.getSettings().getSynchronization().isRespawnAtDisconnectIfDead() || !user.isDead()) {
            return;
        }
        plugin.debug(String.format(
                "[%s] respawnAtDisconnectIfDead: dead mid-quit (PlayerDeathEvent backstop) - forcing local respawn",
                user.getName()));
        user.respawn();
        plugin.debug(String.format(
                "[%s] respawnAtDisconnectIfDead: backstop forced local respawn (still dead afterward: %s)",
                user.getName(), user.isDead()));
    }

    /**
     * Save the data of a player who quit, once other plugins have handled them quitting
     *
     * @param user The {@link OnlineUser} who quit
     */
    protected final void saveOnPlayerQuit(@NotNull OnlineUser user) {
        if (quitSaves.remove(user.getUuid())) {
            plugin.getDataSyncer().syncSaveUserData(user);
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
        // Save for all online players that haven't been processed by PlayerQuitEvent yet.
        plugin.getOnlineUsers().stream()
                .filter(user -> !plugin.isLocked(user.getUuid()) && !user.isNpc())
                .forEach(user -> {
                    plugin.lockPlayer(user.getUuid());
                    plugin.getDataSyncer().saveCurrentUserData(user, DataSnapshot.SaveCause.SERVER_SHUTDOWN);
                });

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
