package net.william278.husksync.listener;

import net.william278.husksync.BukkitHuskSync;
import net.william278.husksync.data.BukkitSerializer;
import net.william278.husksync.data.DataDeserializationException;
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
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.stream.Collectors;

public class BukkitEventListener extends EventListener implements Listener {

    public BukkitEventListener(@NotNull BukkitHuskSync huskSync) {
        super(huskSync);
        Bukkit.getServer().getPluginManager().registerEvents(this, huskSync);
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        super.handlePlayerJoin(BukkitPlayer.adapt(event.getPlayer()));
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        super.handlePlayerQuit(BukkitPlayer.adapt(event.getPlayer()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onWorldSave(@NotNull WorldSaveEvent event) {
        super.handleWorldSave(event.getWorld().getPlayers().stream().map(BukkitPlayer::adapt)
                .collect(Collectors.toList()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            final OnlineUser user = BukkitPlayer.adapt(player);
            if (huskSync.getDataEditor().isEditingInventoryData(user)) {
                try {
                    BukkitSerializer.serializeItemStackArray(event.getInventory().getContents()).thenAccept(
                            serializedInventory -> super.handleMenuClose(user, new ItemData(serializedInventory)));
                } catch (DataDeserializationException e) {
                    huskSync.getLoggingAdapter().log(Level.SEVERE,
                            "Failed to serialize inventory data during menu close", e);
                }
            }
        }
    }

    /*
     * Events to cancel if the player has not been set yet
     */

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDropItem(@NotNull PlayerDropItemEvent event) {
        event.setCancelled(cancelPlayerEvent(BukkitPlayer.adapt(event.getPlayer())));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPickupItem(@NotNull EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            event.setCancelled(cancelPlayerEvent(BukkitPlayer.adapt(player)));
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
        event.setCancelled(cancelPlayerEvent(BukkitPlayer.adapt(event.getPlayer())));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(@NotNull BlockPlaceEvent event) {
        event.setCancelled(cancelPlayerEvent(BukkitPlayer.adapt(event.getPlayer())));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        event.setCancelled(cancelPlayerEvent(BukkitPlayer.adapt(event.getPlayer())));

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpen(@NotNull InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            event.setCancelled(cancelPlayerEvent(BukkitPlayer.adapt(player)));
        }
    }

}
