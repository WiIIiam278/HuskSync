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

import net.william278.husksync.BukkitHuskSync;
import net.william278.husksync.config.Settings;
import net.william278.husksync.data.BukkitInventoryMap;
import net.william278.husksync.data.BukkitSerializer;
import net.william278.husksync.data.ItemData;
import net.william278.husksync.player.BukkitPlayer;
import net.william278.husksync.player.OnlineUser;
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
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
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
    public boolean handleEvent(@NotNull Settings.EventType type, @NotNull Settings.EventPriority priority) {
        return plugin.getSettings().getEventPriority(type).equals(priority);
    }

    @Override
    public void handlePlayerQuit(@NotNull BukkitPlayer bukkitPlayer) {
        final Player player = bukkitPlayer.getPlayer();
        if (!bukkitPlayer.isLocked() && !player.getItemOnCursor().getType().isAir()) {
            player.getWorld().dropItem(player.getLocation(), player.getItemOnCursor());
            player.setItemOnCursor(null);
        }
        super.handlePlayerQuit(bukkitPlayer);
    }

    @Override
    public void handlePlayerJoin(@NotNull BukkitPlayer bukkitPlayer) {
        super.handlePlayerJoin(bukkitPlayer);
    }

    @Override
    public void handlePlayerDeath(@NotNull PlayerDeathEvent event) {
        final OnlineUser user = BukkitPlayer.adapt(event.getEntity());

        // If the player is locked or the plugin disabling, clear their drops
        if (cancelPlayerEvent(user.uuid)) {
            event.getDrops().clear();
            return;
        }

        // Handle saving player data snapshots on death
        if (!plugin.getSettings().doSaveOnDeath()) return;

        // Truncate the drops list to the inventory size and save the player's inventory
        final int maxInventorySize = BukkitInventoryMap.INVENTORY_SLOT_COUNT;
        if (event.getDrops().size() > maxInventorySize) {
            event.getDrops().subList(maxInventorySize, event.getDrops().size()).clear();
        }
        BukkitSerializer.serializeItemStackArray(event.getDrops().toArray(new ItemStack[0]))
                .thenAccept(serializedDrops -> super.saveOnPlayerDeath(user, new ItemData(serializedDrops)));
    }

    @EventHandler(ignoreCancelled = true)
    public void onWorldSave(@NotNull WorldSaveEvent event) {
        // Handle saving player data snapshots when the world saves
        if (!plugin.getSettings().doSaveOnWorldSave()) return;

        CompletableFuture.runAsync(() -> super.saveOnWorldSave(event.getWorld().getPlayers()
                .stream().map(BukkitPlayer::adapt)
                .collect(Collectors.toList())));
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
    public void onPlayerTakeDamage(@NotNull EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            event.setCancelled(cancelPlayerEvent(player.getUniqueId()));
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPermissionCommand(@NotNull PlayerCommandPreprocessEvent event) {
        String[] commandArgs = event.getMessage().substring(1).split(" ");
        String commandLabel = commandArgs[0].toLowerCase(Locale.ENGLISH);

        if (blacklistedCommands.contains("*") || blacklistedCommands.contains(commandLabel)) {
            event.setCancelled(cancelPlayerEvent(event.getPlayer().getUniqueId()));
        }
    }

}
