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

import lombok.Getter;
import net.william278.husksync.BukkitHuskSync;
import net.william278.husksync.data.BukkitData;
import net.william278.husksync.user.BukkitUser;
import net.william278.husksync.user.OnlineUser;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

@Getter
public class BukkitEventListener extends EventListener implements BukkitJoinEventListener, BukkitQuitEventListener,
        BukkitDeathEventListener, Listener {

    protected LockedHandler lockedHandler;
    private volatile boolean clearDeathStateQuitListenerRegistered;

    public BukkitEventListener(@NotNull BukkitHuskSync plugin) {
        super(plugin);
    }

    public void onLoad() {
        this.lockedHandler = createLockedHandler((BukkitHuskSync) plugin);
    }

    public void onEnable() {
        getPlugin().getServer().getPluginManager().registerEvents(this, getPlugin());
        lockedHandler.onEnable();
        scheduleClearDeathStateQuitListener();
    }

    /**
     * Register {@link BukkitClearDeathStateQuitListener} on the first tick after enabling - by which point every
     * plugin loaded at startup has registered its own listeners - so its {@code MONITOR} quit handler runs after
     * theirs. Until then, the configurable-priority quit handlers keep saving as normal.
     */
    protected final void scheduleClearDeathStateQuitListener() {
        getPlugin().runSync(() -> {
            new BukkitClearDeathStateQuitListener(getPlugin(), this).register();
            clearDeathStateQuitListenerRegistered = true;
        });
    }

    @Override
    public boolean isClearDeathStateQuitListenerRegistered() {
        return clearDeathStateQuitListenerRegistered;
    }

    public void handlePluginDisable() {
        super.handlePluginDisable();
        lockedHandler.onDisable();
    }

    @NotNull
    private LockedHandler createLockedHandler(@NotNull BukkitHuskSync plugin) {
        if (!getPlugin().getSettings().isCancelPackets()) {
            return new BukkitLockedEventListener(plugin);
        }
        if (getPlugin().isDependencyLoaded("PacketEvents")) {
            return new BukkitPacketEventsLockedPacketListener(plugin);
        } else if (getPlugin().isDependencyLoaded("ProtocolLib")) {
            return new BukkitProtocolLibLockedPacketListener(plugin);
        }

        return new BukkitLockedEventListener(plugin);
    }

    @Override
    public boolean handleEvent(@NotNull ListenerType type, @NotNull Priority priority) {
        return plugin.getSettings().getSynchronization().getEventPriority(type).equals(priority);
    }

    @Override
    public void markDisconnecting(@NotNull BukkitUser bukkitUser) {
        super.markDisconnecting(bukkitUser);
    }

    @Override
    public void handlePlayerQuit(@NotNull BukkitUser bukkitUser) {
        final Player player = bukkitUser.getPlayer();
        final ItemStack itemOnCursor = player.getItemOnCursor();
        if (!bukkitUser.isLocked() && !itemOnCursor.getType().isAir()) {
            player.setItemOnCursor(null);
            player.getWorld().dropItem(player.getLocation(), itemOnCursor);
            plugin.debug("Dropped " + itemOnCursor + " for " + player.getName() + " on quit");
        }
        super.handlePlayerQuit(bukkitUser);
    }

    /**
     * Backstop for a player dying while already mid-disconnect (per {@link #markDisconnecting}) - see {@link
     * EventListener#clearDeathStateIfKilledMidQuit} for why this is needed specifically for a PvP/anti-combat-logout
     * plugin that, like CombatLogX, kills the player at Bukkit's {@code MONITOR} priority itself.
     * <p>
     * No-op when {@code clearDeathStateOnDisconnect} is disabled, or when the death isn't mid-quit (an
     * ordinary death - not logged, since it's hit on every normal death once the setting is on). Note this runs
     * from inside {@code ServerPlayer#die()}, so {@link EventListener#clearDeathStateIfKilledMidQuit} only restores
     * health here when the disconnect-save has already run and can't do it itself.
     *
     * @since 4.1.0
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeathClearDeathStateBackstop(@NotNull PlayerDeathEvent event) {
        final Player player = event.getEntity();
        if (!plugin.getSettings().getSynchronization().isClearDeathStateOnDisconnect()
                || !plugin.getDisconnectingPlayers().contains(player.getUniqueId())) {
            return;
        }
        plugin.debug("[%s] clearDeathStateOnDisconnect: PlayerDeathEvent while disconnecting (health=%s, cause=%s)"
                .formatted(player.getName(), player.getHealth(), player.getLastDamageCause() == null
                        ? "unknown" : player.getLastDamageCause().getCause()));
        clearDeathStateIfKilledMidQuit(BukkitUser.adapt(player, plugin));
    }

    @Override
    public void saveOnPlayerQuit(@NotNull BukkitUser bukkitUser) {
        super.saveOnPlayerQuit(bukkitUser);
    }

    @Override
    public void handlePlayerJoin(@NotNull BukkitUser bukkitUser) {
        super.handlePlayerJoin(bukkitUser);
    }

    @Override
    public void handlePlayerDeath(@NotNull PlayerDeathEvent event) {
        final OnlineUser user = BukkitUser.adapt(event.getEntity(), plugin);

        // If the player is locked or the plugin disabling, clear their drops
        if (lockedHandler.cancelPlayerEvent(user.getUuid())) {
            event.getDrops().clear();
            return;
        }

        // Handle saving player data snapshots on death
        if (!plugin.getSettings().getSynchronization().getSaveOnDeath().isEnabled()) {
            return;
        }

        // Truncate the dropped items list to the inventory size and save the player's inventory
        final int maxInventorySize = BukkitData.Items.Inventory.INVENTORY_SLOT_COUNT;
        if (event.getDrops().size() > maxInventorySize) {
            event.getDrops().subList(maxInventorySize, event.getDrops().size()).clear();
        }
        super.saveOnPlayerDeath(user, BukkitData.Items.ItemArray.adapt(event.getDrops()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onWorldSave(@NotNull WorldSaveEvent event) {
        if (!plugin.getSettings().getSynchronization().isSaveOnWorldSave()) {
            return;
        }

        // Handle saving player data snapshots when the world saves
        super.saveOnWorldSave(event.getWorld().getPlayers()
                .stream().map(player -> BukkitUser.adapt(player, plugin))
                .collect(Collectors.toList()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onMapInitialize(@NotNull MapInitializeEvent event) {
        if (plugin.getSettings().getSynchronization().isPersistLockedMaps() && event.getMap().isLocked()) {
            getPlugin().runAsync(() -> ((BukkitHuskSync) plugin).renderInitializingLockedMap(event.getMap()));
        }
    }

    // We handle commands here to allow specific command handling on ProtocolLib servers
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCommandProcessed(@NotNull PlayerCommandPreprocessEvent event) {
        if (!lockedHandler.isCommandDisabled(event.getMessage().substring(1).split(" ")[0])) {
            return;
        }
        if (lockedHandler.cancelPlayerEvent(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @NotNull
    @Override
    public BukkitHuskSync getPlugin() {
        return (BukkitHuskSync) plugin;
    }

}
