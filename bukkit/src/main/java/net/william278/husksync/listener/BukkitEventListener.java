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
import net.william278.husksync.HuskSync;
import net.william278.husksync.data.BukkitData;
import net.william278.husksync.user.BukkitUser;
import net.william278.husksync.user.OnlineUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class BukkitEventListener extends EventListener implements BukkitJoinEventListener, BukkitQuitEventListener,
        BukkitDeathEventListener, Listener {
    protected final List<String> blacklistedCommands;

    public BukkitEventListener(@NotNull BukkitHuskSync huskSync) {
        super(huskSync);
        this.blacklistedCommands = huskSync.getSettings().getBlacklistedCommandsWhileLocked();
        Bukkit.getServer().getPluginManager().registerEvents(this, huskSync);
    }

    @Override
    public boolean handleEvent(@NotNull ListenerType type, @NotNull Priority priority) {
        return plugin.getSettings().getEventPriority(type).equals(priority);
    }

    @Override
    public void handlePlayerQuit(@NotNull BukkitUser bukkitUser) {
        final Player player = bukkitUser.getPlayer();
        if (!bukkitUser.isLocked() && !player.getItemOnCursor().getType().isAir()) {
            player.getWorld().dropItem(player.getLocation(), player.getItemOnCursor());
            player.setItemOnCursor(null);
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
        if (cancelPlayerEvent(user.getUuid())) {
            event.getDrops().clear();
            return;
        }

        // Handle saving player data snapshots on death
        if (!plugin.getSettings().doSaveOnDeath()) {
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
        if (!plugin.getSettings().doSaveOnWorldSave()) {
            return;
        }

        // Handle saving player data snapshots when the world saves
        plugin.runAsync(() -> super.saveOnWorldSave(event.getWorld().getPlayers()
                .stream().map(player -> BukkitUser.adapt(player, plugin))
                .collect(Collectors.toList())));
    }

    @EventHandler(ignoreCancelled = true)
    public void onMapInitialize(@NotNull MapInitializeEvent event) {
        if (plugin.getSettings().doPersistLockedMaps() && event.getMap().isLocked()) {
            getPlugin().runAsync(() -> ((BukkitHuskSync) plugin).renderMapFromFile(event.getMap()));
        }
    }


    /*
     * Events to cancel if the player has not been set yet
     */

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileLaunch(@NotNull ProjectileLaunchEvent event) {
        final Projectile projectile = event.getEntity();
        if (projectile.getShooter() instanceof Player player) {
            event.setCancelled(cancelPlayerEvent(player.getUniqueId()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDropItem(@NotNull PlayerDropItemEvent event) {
        event.setCancelled(cancelPlayerEvent(event.getPlayer().getUniqueId()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickupItem(@NotNull EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            event.setCancelled(cancelPlayerEvent(player.getUniqueId()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
        event.setCancelled(cancelPlayerEvent(event.getPlayer().getUniqueId()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteractEntity(@NotNull PlayerInteractEntityEvent event) {
        event.setCancelled(cancelPlayerEvent(event.getPlayer().getUniqueId()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(@NotNull BlockPlaceEvent event) {
        event.setCancelled(cancelPlayerEvent(event.getPlayer().getUniqueId()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        event.setCancelled(cancelPlayerEvent(event.getPlayer().getUniqueId()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryOpen(@NotNull InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            event.setCancelled(cancelPlayerEvent(player.getUniqueId()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        event.setCancelled(cancelPlayerEvent(event.getWhoClicked().getUniqueId()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraftItem(@NotNull PrepareItemCraftEvent event) {
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTakeDamage(@NotNull EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            event.setCancelled(cancelPlayerEvent(player.getUniqueId()));
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPermissionCommand(@NotNull PlayerCommandPreprocessEvent event) {
        final String[] commandArgs = event.getMessage().substring(1).split(" ");
        final String commandLabel = commandArgs[0].toLowerCase(Locale.ENGLISH);

        if (blacklistedCommands.contains("*") || blacklistedCommands.contains(commandLabel)) {
            event.setCancelled(cancelPlayerEvent(event.getPlayer().getUniqueId()));
        }
    }

    @NotNull
    @Override
    public HuskSync getPlugin() {
        return plugin;
    }

}
