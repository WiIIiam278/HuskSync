package net.william278.husksync.listener;

import net.william278.husksync.BukkitHuskSync;
import net.william278.husksync.player.BukkitPlayer;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.jetbrains.annotations.NotNull;

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
        BukkitPlayer.remove(event.getPlayer());
    }

    @EventHandler
    public void onWorldSave(@NotNull WorldSaveEvent event) {
        super.handleWorldSave(event.getWorld().getPlayers().stream().map(BukkitPlayer::adapt)
                .collect(Collectors.toList()));
    }

    /*@EventHandler(ignoreCancelled = true)
    public void onGenericPlayerEvent(@NotNull PlayerEvent event) {
        if (event instanceof Cancellable) {
            ((Cancellable) event).setCancelled(cancelPlayerEvent(BukkitPlayer.adapt(event.getPlayer())));
        }
    }*/

}
