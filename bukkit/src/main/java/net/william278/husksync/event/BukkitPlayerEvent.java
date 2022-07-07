package net.william278.husksync.event;

import net.william278.husksync.BukkitHuskSync;
import net.william278.husksync.player.BukkitPlayer;
import net.william278.husksync.player.OnlineUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public abstract class BukkitPlayerEvent extends org.bukkit.event.player.PlayerEvent implements PlayerEvent {


    public BukkitPlayerEvent(@NotNull Player who) {
        super(who);
    }

    @Override
    public OnlineUser getUser() {
        return BukkitPlayer.adapt(player);
    }

    @Override
    public CompletableFuture<Event> fire() {
        final CompletableFuture<Event> eventFireFuture = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(BukkitHuskSync.getInstance(), () -> {
            Bukkit.getServer().getPluginManager().callEvent(this);
            eventFireFuture.complete(this);
        });
        return eventFireFuture;
    }

}
