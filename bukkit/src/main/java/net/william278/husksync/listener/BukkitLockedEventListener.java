package net.william278.husksync.listener;

import lombok.Getter;
import net.william278.husksync.BukkitHuskSync;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.*;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter
public class BukkitLockedEventListener implements LockedHandler, Listener {

    private final BukkitHuskSync plugin;

    protected BukkitLockedEventListener(@NotNull BukkitHuskSync plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileLaunch(@NotNull ProjectileLaunchEvent event) {
        final Projectile projectile = event.getEntity();
        if (projectile.getShooter() instanceof Player player) {
            cancelPlayerEvent(player.getUniqueId(), event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDropItem(@NotNull PlayerDropItemEvent event) {
        cancelPlayerEvent(event.getPlayer().getUniqueId(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickupItem(@NotNull EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            cancelPlayerEvent(player.getUniqueId(), event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
        cancelPlayerEvent(event.getPlayer().getUniqueId(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteractEntity(@NotNull PlayerInteractEntityEvent event) {
        cancelPlayerEvent(event.getPlayer().getUniqueId(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteractArmorStand(@NotNull PlayerArmorStandManipulateEvent event) {
        cancelPlayerEvent(event.getPlayer().getUniqueId(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(@NotNull BlockPlaceEvent event) {
        cancelPlayerEvent(event.getPlayer().getUniqueId(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        cancelPlayerEvent(event.getPlayer().getUniqueId(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryOpen(@NotNull InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            cancelPlayerEvent(player.getUniqueId(), event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        cancelPlayerEvent(event.getWhoClicked().getUniqueId(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraftItem(@NotNull PrepareItemCraftEvent event) {
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTakeDamage(@NotNull EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            cancelPlayerEvent(player.getUniqueId(), event);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPermissionCommand(@NotNull PlayerCommandPreprocessEvent event) {
        if (isCommandDisabled(event.getMessage().substring(1).split(" ")[0])) {
            cancelPlayerEvent(event.getPlayer().getUniqueId(), event);
        }
    }

    private void cancelPlayerEvent(@NotNull UUID uuid, @NotNull Cancellable event) {
        if (cancelPlayerEvent(uuid)) {
            event.setCancelled(true);
        }
    }

}
