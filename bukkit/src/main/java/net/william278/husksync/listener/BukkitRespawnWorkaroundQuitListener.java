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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * While {@code respawnAtDisconnectIfDead} is enabled, this - not the configurable-priority handlers in {@link
 * BukkitQuitEventListener}, which stand aside in that case - is what actually locks and saves the disconnecting
 * player.
 * <p>
 * It runs at Bukkit's {@code MONITOR} priority, the last tier, so it fires after every other plugin's
 * lower-priority {@code PlayerQuitEvent} handler. Within the same priority, Bukkit runs handlers in registration
 * order, so this is registered a tick after HuskSync enables (i.e. once the server has finished loading every
 * plugin) rather than alongside HuskSync's other listeners. That puts it after a kill-on-quit plugin which itself
 * uses {@code MONITOR} (e.g. CombatLogX) too, so the dead check here sees that kill and the respawn happens here,
 * outside of {@code ServerPlayer#die()} - leaving the {@code PlayerDeathEvent} backstop (see {@code
 * EventListener#forceRespawnIfDeadMidQuit}) for plugins that register their own {@code MONITOR} listener even
 * later than this, such as one loaded or reloaded at runtime.
 *
 * @since 4.1.0
 */
public class BukkitRespawnWorkaroundQuitListener implements Listener {

    private final BukkitHuskSync plugin;
    private final BukkitEventListener eventListener;
    private final Set<String> warnedLaterPlugins = new HashSet<>();

    BukkitRespawnWorkaroundQuitListener(@NotNull BukkitHuskSync plugin, @NotNull BukkitEventListener eventListener) {
        this.plugin = plugin;
        this.eventListener = eventListener;
    }

    void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        if (!plugin.getSettings().getSynchronization().isRespawnAtDisconnectIfDead()) {
            return;
        }
        plugin.log(Level.INFO, "respawnAtDisconnectIfDead is enabled: disconnect-saves now run from a late "
                + "MONITOR PlayerQuitEvent listener (configured quit_listener priority is ignored)");
        plugin.debug("respawnAtDisconnectIfDead: MONITOR PlayerQuitEvent listener order: "
                + describeMonitorQuitListeners());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        if (!plugin.getSettings().getSynchronization().isRespawnAtDisconnectIfDead()) {
            return;
        }
        checkForLaterMonitorListeners(event.getPlayer().getName());
        eventListener.handlePlayerQuit(BukkitUser.adapt(event.getPlayer(), plugin));
    }

    /**
     * Warn (once per plugin) if another plugin's {@code MONITOR} quit listener now runs after this one - e.g.
     * because it was loaded or reloaded at runtime. Such a plugin killing players on quit can only be caught by
     * the {@code PlayerDeathEvent} backstop, which respawns from inside {@code ServerPlayer#die()} and can't
     * record the death in the network snapshot.
     */
    private void checkForLaterMonitorListeners(@NotNull String playerName) {
        final List<String> later = getLaterMonitorQuitPlugins();
        if (later.isEmpty()) {
            return;
        }
        plugin.debug(("[%s] respawnAtDisconnectIfDead: MONITOR PlayerQuitEvent listeners running AFTER the "
                + "disconnect-save: %s").formatted(playerName, later));
        for (String name : later) {
            if (warnedLaterPlugins.add(name)) {
                plugin.log(Level.WARNING, ("respawnAtDisconnectIfDead: %s has a MONITOR PlayerQuitEvent listener "
                        + "that runs after HuskSync's disconnect-save (was it loaded or reloaded at runtime?). If it "
                        + "kills players on quit, those deaths fall back to the PlayerDeathEvent backstop and won't "
                        + "be reflected in synced data. Restart the server to restore listener order.")
                        .formatted(name));
            }
        }
    }

    @NotNull
    private List<String> getLaterMonitorQuitPlugins() {
        final List<String> later = new ArrayList<>();
        boolean foundSelf = false;
        for (RegisteredListener listener : PlayerQuitEvent.getHandlerList().getRegisteredListeners()) {
            if (listener.getListener() == this) {
                foundSelf = true;
            } else if (foundSelf && listener.getPriority() == EventPriority.MONITOR
                    && listener.getPlugin() != plugin) {
                later.add(listener.getPlugin().getName());
            }
        }
        return later;
    }

    @NotNull
    private String describeMonitorQuitListeners() {
        final List<String> order = new ArrayList<>();
        for (RegisteredListener listener : PlayerQuitEvent.getHandlerList().getRegisteredListeners()) {
            if (listener.getPriority() != EventPriority.MONITOR) {
                continue;
            }
            order.add(listener.getListener() == this
                    ? "[HuskSync disconnect-save]"
                    : listener.getPlugin().getName() + " (" + listener.getListener().getClass().getSimpleName() + ")");
        }
        return String.join(" -> ", order);
    }

}
