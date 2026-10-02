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
import net.william278.husksync.user.BukkitUser;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public interface BukkitQuitEventListener extends Listener {

    boolean handleEvent(@NotNull EventListener.ListenerType type, @NotNull EventListener.Priority priority);

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    default void onPlayerQuitHighest(@NotNull PlayerQuitEvent event) {
        if (handleEvent(EventListener.ListenerType.QUIT_LISTENER, EventListener.Priority.HIGHEST)
                && !isRespawnAtDisconnectIfDeadEnabled()) {
            handlePlayerQuit(BukkitUser.adapt(event.getPlayer(), getPlugin()));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    default void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        if (handleEvent(EventListener.ListenerType.QUIT_LISTENER, EventListener.Priority.NORMAL)
                && !isRespawnAtDisconnectIfDeadEnabled()) {
            handlePlayerQuit(BukkitUser.adapt(event.getPlayer(), getPlugin()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    default void onPlayerQuitLowest(@NotNull PlayerQuitEvent event) {
        if (handleEvent(EventListener.ListenerType.QUIT_LISTENER, EventListener.Priority.LOWEST)
                && !isRespawnAtDisconnectIfDeadEnabled()) {
            handlePlayerQuit(BukkitUser.adapt(event.getPlayer(), getPlugin()));
        }
    }

    // Runs after other plugins handle the quit and potentially change player
    // data, apart from any other MONITOR handlers registered after this one.
    // Runs regardless of listener type priorities configured via config.yml.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    default void onPlayerQuitMonitor(@NotNull PlayerQuitEvent event) {
        saveOnPlayerQuit(BukkitUser.adapt(event.getPlayer(), getPlugin()));
    }

    /**
     * Marks a user as disconnecting as early as possible (LOWEST priority, always - not gated behind the
     * configurable {@link EventListener.ListenerType#QUIT_LISTENER} priority), so that the disconnect-save's
     * {@code PlayerDeathEvent} backstop (see {@code EventListener#forceRespawnIfDeadMidQuit}) can reliably
     * recognise a kill that happens later in the same quit, no matter what priority the killing plugin uses.
     *
     * @since 4.1.0
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    default void onPlayerQuitMarkDisconnecting(@NotNull PlayerQuitEvent event) {
        if (isRespawnAtDisconnectIfDeadEnabled()) {
            markDisconnecting(BukkitUser.adapt(event.getPlayer(), getPlugin()));
        }
    }

    /**
     * While {@code respawnAtDisconnectIfDead} is enabled, this - not {@link #onPlayerQuitHighest}/{@link
     * #onPlayerQuit}/{@link #onPlayerQuitLowest}, which skip firing in that case - is what actually locks and
     * saves the disconnecting player. Running at Bukkit's {@code MONITOR} priority, the last tier there is,
     * guarantees it fires after every other plugin's own {@code PlayerQuitEvent} handler regardless of what
     * priority they use or what order plugins load in - so a kill-on-quit plugin like PvPManager (which doesn't
     * itself use {@code MONITOR}) is reliably seen as already dead here with no admin configuration needed.
     * A plugin that (like CombatLogX) kills at {@code MONITOR} itself is the one case Bukkit's priority tiers
     * can't order against; see {@code EventListener#forceRespawnIfDeadMidQuit} for how that's still handled.
     *
     * @since 4.1.0
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    default void onPlayerQuitRespawnWorkaround(@NotNull PlayerQuitEvent event) {
        if (isRespawnAtDisconnectIfDeadEnabled()) {
            handlePlayerQuit(BukkitUser.adapt(event.getPlayer(), getPlugin()));
        }
    }

    private boolean isRespawnAtDisconnectIfDeadEnabled() {
        return getPlugin().getSettings().getSynchronization().isRespawnAtDisconnectIfDead();
    }

    void markDisconnecting(@NotNull BukkitUser player);

    void handlePlayerQuit(@NotNull BukkitUser player);

    void saveOnPlayerQuit(@NotNull BukkitUser player);

    @NotNull
    HuskSync getPlugin();

}
