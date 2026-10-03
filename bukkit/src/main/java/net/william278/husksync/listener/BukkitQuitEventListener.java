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
                && !isQuitSaveDeferredToLateListener()) {
            handlePlayerQuit(BukkitUser.adapt(event.getPlayer(), getPlugin()));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    default void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        if (handleEvent(EventListener.ListenerType.QUIT_LISTENER, EventListener.Priority.NORMAL)
                && !isQuitSaveDeferredToLateListener()) {
            handlePlayerQuit(BukkitUser.adapt(event.getPlayer(), getPlugin()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    default void onPlayerQuitLowest(@NotNull PlayerQuitEvent event) {
        if (handleEvent(EventListener.ListenerType.QUIT_LISTENER, EventListener.Priority.LOWEST)
                && !isQuitSaveDeferredToLateListener()) {
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
     * {@code PlayerDeathEvent} backstop (see {@code EventListener#clearDeathStateIfKilledMidQuit}) can reliably
     * recognise a death that happens later in the same quit, no matter what priority the killing reason has.
     *
     * @since 4.1.0
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    default void onPlayerQuitMarkDisconnecting(@NotNull PlayerQuitEvent event) {
        if (isClearDeathStateOnDisconnectEnabled()) {
            markDisconnecting(BukkitUser.adapt(event.getPlayer(), getPlugin()));
        }
    }

    private boolean isClearDeathStateOnDisconnectEnabled() {
        return getPlugin().getSettings().getSynchronization().isClearDeathStateOnDisconnect();
    }

    /**
     * Whether the normal, configurable-priority quit handlers above should leave the disconnect-save
     * to {@link BukkitClearDeathStateQuitListener} instead. Only true once that listener has been 
     * registered, so there's never a window where neither saves (e.g. the tick between HuskSync
     * enabling and the late listener registering, if HuskSync is loaded after server startup).
     */
    private boolean isQuitSaveDeferredToLateListener() {
        return isClearDeathStateOnDisconnectEnabled() && isClearDeathStateQuitListenerRegistered();
    }

    boolean isClearDeathStateQuitListenerRegistered();

    void markDisconnecting(@NotNull BukkitUser player);

    void handlePlayerQuit(@NotNull BukkitUser player);

    void saveOnPlayerQuit(@NotNull BukkitUser player);

    @NotNull
    HuskSync getPlugin();

}
