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
        if (handleEvent(EventListener.ListenerType.QUIT_LISTENER, EventListener.Priority.HIGHEST)) {
            handlePlayerQuit(BukkitUser.adapt(event.getPlayer(), getPlugin()));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    default void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        if (handleEvent(EventListener.ListenerType.QUIT_LISTENER, EventListener.Priority.NORMAL)) {
            handlePlayerQuit(BukkitUser.adapt(event.getPlayer(), getPlugin()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    default void onPlayerQuitLowest(@NotNull PlayerQuitEvent event) {
        if (handleEvent(EventListener.ListenerType.QUIT_LISTENER, EventListener.Priority.LOWEST)) {
            handlePlayerQuit(BukkitUser.adapt(event.getPlayer(), getPlugin()));
        }
    }

    void handlePlayerQuit(@NotNull BukkitUser player);

    @NotNull
    HuskSync getPlugin();

}
