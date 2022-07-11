package net.william278.husksync.listener;

import net.william278.husksync.BukkitHuskSync;
import net.william278.husksync.data.BukkitSerializer;
import net.william278.husksync.data.DataSerializationException;
import net.william278.husksync.data.ItemData;
import net.william278.husksync.editor.ItemEditorMenuType;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
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
        CompletableFuture.runAsync(() -> super.handleAsyncWorldSave(event.getWorld().getPlayers().stream()
                .map(BukkitPlayer::adapt).collect(Collectors.toList())));
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        CompletableFuture.runAsync(() -> {
            if (event.getPlayer() instanceof Player player) {
                final OnlineUser user = BukkitPlayer.adapt(player);
                plugin.getDataEditor().getEditingInventoryData(user).ifPresent(menu -> {
                    try {
                        BukkitSerializer.serializeItemStackArray(Arrays.copyOf(event.getInventory().getContents(),
                                menu.itemEditorMenuType == ItemEditorMenuType.INVENTORY_VIEWER
                                        ? player.getInventory().getSize()
                                        : player.getEnderChest().getSize())).thenAccept(
                                serializedInventory -> super.handleMenuClose(user, new ItemData(serializedInventory)));
                    } catch (DataSerializationException e) {
                        plugin.getLoggingAdapter().log(Level.SEVERE,
                                "Failed to serialize inventory data during menu close", e);
                    }
                });
            }
        });
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
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            event.setCancelled(cancelInventoryClick(BukkitPlayer.adapt(player)));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryOpen(@NotNull InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            event.setCancelled(cancelPlayerEvent(BukkitPlayer.adapt(player)));
        }
    }

}
