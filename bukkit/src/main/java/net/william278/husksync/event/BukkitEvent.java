package net.william278.husksync.event;

import net.william278.husksync.BukkitHuskSync;
import net.william278.husksync.player.BukkitPlayer;
import net.william278.husksync.player.OnlineUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public abstract class BukkitEvent extends Event implements net.william278.husksync.event.Event {

    @Override
    public CompletableFuture<net.william278.husksync.event.Event> fire() {
        final CompletableFuture<net.william278.husksync.event.Event> eventFireFuture = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(BukkitHuskSync.getInstance(), () -> {
            Bukkit.getServer().getPluginManager().callEvent(this);
            eventFireFuture.complete(this);
        });
        return eventFireFuture;
    }

}
