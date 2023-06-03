/*
 * This file is part of HuskSync by William278. Do not redistribute!
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  All rights reserved.
 *
 *  This source code is provided as reference to licensed individuals that have purchased the HuskSync
 *  plugin once from any of the official sources it is provided. The availability of this code does
 *  not grant you the rights to modify, re-distribute, compile or redistribute this source code or
 *  "plugin" outside this intended purpose. This license does not cover libraries developed by third
 *  parties that are utilised in the plugin.
 */

package net.william278.husksync.listener;

import net.william278.husksync.player.BukkitPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public interface BukkitQuitEventListener extends Listener {

    boolean handleEvent(@NotNull EventListener.ListenerType type, @NotNull EventListener.Priority priority);

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    default void onPlayerQuitHighest(@NotNull PlayerQuitEvent event) {
        if (handleEvent(EventListener.ListenerType.QUIT_LISTENER, EventListener.Priority.HIGHEST)) {
            handlePlayerQuit(BukkitPlayer.adapt(event.getPlayer()));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    default void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        if (handleEvent(EventListener.ListenerType.QUIT_LISTENER, EventListener.Priority.NORMAL)) {
            handlePlayerQuit(BukkitPlayer.adapt(event.getPlayer()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    default void onPlayerQuitLowest(@NotNull PlayerQuitEvent event) {
        if (handleEvent(EventListener.ListenerType.QUIT_LISTENER, EventListener.Priority.LOWEST)) {
            handlePlayerQuit(BukkitPlayer.adapt(event.getPlayer()));
        }
    }

    void handlePlayerQuit(@NotNull BukkitPlayer player);

}
