package net.william278.husksync.listener;

import net.william278.husksync.BukkitHuskSync;
import net.william278.husksync.data.BukkitInventoryMap;
import net.william278.husksync.data.BukkitSerializer;
import net.william278.husksync.data.ItemData;
import net.william278.husksync.player.BukkitPlayer;
import net.william278.husksync.player.OnlineUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class BukkitEventListener extends EventListener implements Listener {

    public BukkitEventListener(@NotNull BukkitHuskSync huskSync) {
        super(huskSync);
        Bukkit.getServer().getPluginManager().registerEvents(this, huskSync);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        super.handlePlayerJoin(BukkitPlayer.adapt(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        super.handlePlayerQuit(BukkitPlayer.adapt(event.getPlayer()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onWorldSave(@NotNull WorldSaveEvent event) {
        // Handle saving player data snapshots when the world saves
        if (!plugin.getSettings().saveOnWorldSave) return;

        CompletableFuture.runAsync(() -> super.saveOnWorldSave(event.getWorld().getPlayers()
                .stream().map(BukkitPlayer::adapt)
                .collect(Collectors.toList())));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        final OnlineUser user = BukkitPlayer.adapt(event.getEntity());

        // If the player is locked or the plugin disabling, clear their drops
        if (cancelPlayerEvent(user)) {
            event.getDrops().clear();
            return;
        }

        // Handle saving player data snapshots on death
        if (!plugin.getSettings().saveOnDeath) return;

        // Truncate the drops list to the inventory size and save the player's inventory
        final int maxInventorySize = BukkitInventoryMap.INVENTORY_SLOT_COUNT;
        if (event.getDrops().size() > maxInventorySize) {
            event.getDrops().subList(maxInventorySize, event.getDrops().size()).clear();
        }
        BukkitSerializer.serializeItemStackArray(event.getDrops().toArray(new ItemStack[0]))
                .thenAccept(serializedDrops -> super.saveOnPlayerDeath(user, new ItemData(serializedDrops)));
    }


    /*
     * Events to cancel if the player has not been set yet
     */

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDropItem(@NotNull PlayerDropItemEvent event) {
        event.setCancelled(cancelPlayerEvent(BukkitPlayer.adapt(event.getPlayer())));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickupItem(@NotNull EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            event.setCancelled(cancelPlayerEvent(BukkitPlayer.adapt(player)));
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
        event.setCancelled(cancelPlayerEvent(BukkitPlayer.adapt(event.getPlayer())));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(@NotNull BlockPlaceEvent event) {
        event.setCancelled(cancelPlayerEvent(BukkitPlayer.adapt(event.getPlayer())));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        event.setCancelled(cancelPlayerEvent(BukkitPlayer.adapt(event.getPlayer())));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryOpen(@NotNull InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            event.setCancelled(cancelPlayerEvent(BukkitPlayer.adapt(player)));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTakeDamage(@NotNull EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            event.setCancelled(cancelPlayerEvent(BukkitPlayer.adapt(player)));
        }
    }

}
