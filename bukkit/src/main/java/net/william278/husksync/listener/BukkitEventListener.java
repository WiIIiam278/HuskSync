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

public class BukkitEventListener extends EventListener implements BukkitJoinEventListener, BukkitQuitEventListener,
        BukkitDeathEventListener, Listener {

    protected LockedHandler lockedHandler;

    public BukkitEventListener(@NotNull BukkitHuskSync plugin) {
        super(plugin);
    }

    public void onLoad() {
        this.lockedHandler = createLockedHandler((BukkitHuskSync) plugin);
    }

    public void onEnable() {
        getPlugin().getServer().getPluginManager().registerEvents(this, getPlugin());
        lockedHandler.onEnable();
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
        plugin.runAsync(() -> super.saveOnWorldSave(event.getWorld().getPlayers()
                .stream().map(player -> BukkitUser.adapt(player, plugin))
                .collect(Collectors.toList())));
    }

    @EventHandler(ignoreCancelled = true)
    public void onMapInitialize(@NotNull MapInitializeEvent event) {
        if (plugin.getSettings().getSynchronization().isPersistLockedMaps() && event.getMap().isLocked()) {
            getPlugin().runAsync(() -> ((BukkitHuskSync) plugin).renderMapFromFile(event.getMap()));
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
