package me.william278.husksync.bukkit.listener;

import me.william278.husksync.HuskSyncBukkit;
import me.william278.husksync.PlayerData;
import me.william278.husksync.Settings;
import me.william278.husksync.bukkit.data.DataSerializer;
import me.william278.husksync.bukkit.data.DataViewer;
import me.william278.husksync.bukkit.PlayerSetter;
import me.william278.husksync.redis.RedisMessage;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;

public class EventListener implements Listener {

    private static final HuskSyncBukkit plugin = HuskSyncBukkit.getInstance();

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // When a player leaves a Bukkit server
        final Player player = event.getPlayer();

        // If the player was awaiting data fetch, remove them and prevent data from being overwritten
        if (HuskSyncBukkit.bukkitCache.isAwaitingDataFetch(player.getUniqueId())) {
            HuskSyncBukkit.bukkitCache.removeAwaitingDataFetch(player.getUniqueId());
            return;
        }

        if (!plugin.isEnabled() || !HuskSyncBukkit.handshakeCompleted || HuskSyncBukkit.isMySqlPlayerDataBridgeInstalled) return; // If the plugin has not been initialized correctly

        // Update the player's data
        PlayerSetter.updatePlayerData(player);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.isEnabled()) return; // If the plugin has not been initialized correctly

        // When a player joins a Bukkit server
        final Player player = event.getPlayer();

        // Mark the player as awaiting data fetch
        HuskSyncBukkit.bukkitCache.setAwaitingDataFetch(player.getUniqueId());

        if (!HuskSyncBukkit.handshakeCompleted || HuskSyncBukkit.isMySqlPlayerDataBridgeInstalled) return; // If the data handshake has not been completed yet (or MySqlPlayerDataBridge is installed)

        // Send a redis message requesting the player data (if they need to)
        if (HuskSyncBukkit.bukkitCache.isPlayerRequestingOnJoin(player.getUniqueId())) {
            try {
                PlayerSetter.requestPlayerData(player.getUniqueId());
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to send a PlayerData fetch request", e);
            }
        } else {
            // If the player's data wasn't set after 10 ticks, ensure it will be
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                if (player.isOnline()) {
                    try {
                        if (HuskSyncBukkit.bukkitCache.isAwaitingDataFetch(player.getUniqueId())) {
                            PlayerSetter.requestPlayerData(player.getUniqueId());
                        }
                    } catch (IOException e) {
                        plugin.getLogger().log(Level.SEVERE, "Failed to send a PlayerData fetch request", e);
                    }
                }
            }, 5);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!plugin.isEnabled() || !HuskSyncBukkit.handshakeCompleted || HuskSyncBukkit.bukkitCache.isAwaitingDataFetch(event.getPlayer().getUniqueId())) return; // If the plugin has not been initialized correctly

        // When a player closes an Inventory
        final Player player = (Player) event.getPlayer();

        // Handle a player who has finished viewing a player's item data
        if (HuskSyncBukkit.bukkitCache.isViewing(player.getUniqueId())) {
            try {
                DataViewer.stopShowing(player, event.getInventory());
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to serialize updated item data", e);
            }
        }
    }

    /*
     * Events to cancel if the player has not been set yet
     */

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDropItem(PlayerDropItemEvent event) {
        if (!plugin.isEnabled() || !HuskSyncBukkit.handshakeCompleted || HuskSyncBukkit.bukkitCache.isAwaitingDataFetch(event.getPlayer().getUniqueId())) {
            event.setCancelled(true); // If the plugin / player has not been set
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (!plugin.isEnabled() || !HuskSyncBukkit.handshakeCompleted || HuskSyncBukkit.bukkitCache.isAwaitingDataFetch(player.getUniqueId())) {
                event.setCancelled(true); // If the plugin / player has not been set
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.isEnabled() || !HuskSyncBukkit.handshakeCompleted || HuskSyncBukkit.bukkitCache.isAwaitingDataFetch(event.getPlayer().getUniqueId())) {
            event.setCancelled(true); // If the plugin / player has not been set
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.isEnabled() || !HuskSyncBukkit.handshakeCompleted || HuskSyncBukkit.bukkitCache.isAwaitingDataFetch(event.getPlayer().getUniqueId())) {
            event.setCancelled(true); // If the plugin / player has not been set
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.isEnabled() || !HuskSyncBukkit.handshakeCompleted || HuskSyncBukkit.bukkitCache.isAwaitingDataFetch(event.getPlayer().getUniqueId())) {
            event.setCancelled(true); // If the plugin / player has not been set
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!plugin.isEnabled() || !HuskSyncBukkit.handshakeCompleted || HuskSyncBukkit.bukkitCache.isAwaitingDataFetch(event.getPlayer().getUniqueId())) {
            event.setCancelled(true); // If the plugin / player has not been set
        }
    }
}
