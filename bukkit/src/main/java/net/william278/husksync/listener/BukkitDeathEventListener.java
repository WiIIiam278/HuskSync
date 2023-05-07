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

import net.william278.husksync.config.Settings;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

public interface BukkitDeathEventListener extends Listener {

    boolean handleEvent(@NotNull Settings.EventType type, @NotNull Settings.EventPriority priority);

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    default void onPlayerDeathHighest(@NotNull PlayerDeathEvent event) {
        if (handleEvent(Settings.EventType.DEATH_LISTENER, Settings.EventPriority.HIGHEST)) {
            handlePlayerDeath(event);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    default void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        if (handleEvent(Settings.EventType.DEATH_LISTENER, Settings.EventPriority.NORMAL)) {
            handlePlayerDeath(event);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    default void onPlayerDeathLowest(@NotNull PlayerDeathEvent event) {
        if (handleEvent(Settings.EventType.DEATH_LISTENER, Settings.EventPriority.LOWEST)) {
            handlePlayerDeath(event);
        }
    }

    void handlePlayerDeath(@NotNull PlayerDeathEvent player);

}
