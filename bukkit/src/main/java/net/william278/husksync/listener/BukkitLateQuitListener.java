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

import net.william278.husksync.BukkitHuskSync;
import net.william278.husksync.user.BukkitUser;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Saves player data on quit at {@code MONITOR} priority, after other plugins have handled the quit.
 * <p>
 * Bukkit runs listeners of the same priority in the order they're registered, so this is registered a tick after
 * HuskSync enables, once every plugin has registered its own listeners. That way it also runs after other plugins'
 * {@code MONITOR} listeners, such as combat loggers that kill players when they quit.
 */
public class BukkitLateQuitListener implements Listener {

    private final BukkitHuskSync plugin;
    private final BukkitEventListener eventListener;
    private final Set<String> warnedPlugins = ConcurrentHashMap.newKeySet();

    BukkitLateQuitListener(@NotNull BukkitHuskSync plugin, @NotNull BukkitEventListener eventListener) {
        this.plugin = plugin;
        this.eventListener = eventListener;
    }

    void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.debug("MONITOR quit listener order: " + getMonitorQuitListeners());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        warnLaterMonitorListeners();
        eventListener.handlePlayerQuitMonitor(BukkitUser.adapt(event.getPlayer(), plugin));
    }

    // Plugins enabled or reloaded at runtime register their listeners after this one
    private void warnLaterMonitorListeners() {
        boolean foundSelf = false;
        for (RegisteredListener listener : PlayerQuitEvent.getHandlerList().getRegisteredListeners()) {
            if (listener.getListener() == this) {
                foundSelf = true;
            } else if (foundSelf && listener.getPriority() == EventPriority.MONITOR
                    && listener.getPlugin() != plugin && warnedPlugins.add(listener.getPlugin().getName())) {
                plugin.log(Level.WARNING, ("%s handles player quits after HuskSync saves player data, so any changes "
                        + "it makes on quit (e.g. killing players) won't be saved. Restart the server rather than "
                        + "loading or reloading plugins at runtime.").formatted(listener.getPlugin().getName()));
            }
        }
    }

    @NotNull
    private String getMonitorQuitListeners() {
        final List<String> order = new ArrayList<>();
        for (RegisteredListener listener : PlayerQuitEvent.getHandlerList().getRegisteredListeners()) {
            if (listener.getPriority() == EventPriority.MONITOR) {
                order.add(listener.getListener() == this ? "HuskSync (save)" : listener.getPlugin().getName());
            }
        }
        return String.join(" -> ", order);
    }

}
