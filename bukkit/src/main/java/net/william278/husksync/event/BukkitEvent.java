package net.william278.husksync.event;

import net.william278.husksync.BukkitHuskSync;
import net.william278.husksync.player.BukkitPlayer;
import net.william278.husksync.player.OnlineUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public abstract class BukkitEvent extends Event implements net.william278.husksync.event.Event {

    protected BukkitEvent() {
    }

    @Override
    public CompletableFuture<net.william278.husksync.event.Event> fire() {
        final CompletableFuture<net.william278.husksync.event.Event> eventFireFuture = new CompletableFuture<>();
        // Don't fire events while the server is shutting down
        if (!BukkitHuskSync.getInstance().isEnabled()) {
            eventFireFuture.complete(this);
        } else {
            Bukkit.getScheduler().runTask(BukkitHuskSync.getInstance(), () -> {
                Bukkit.getServer().getPluginManager().callEvent(this);
                eventFireFuture.complete(this);
            });
        }
        return eventFireFuture;
    }

}
