package net.william278.husksync.event;

import net.william278.husksync.BukkitHuskSync;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public abstract class BukkitEvent<T> extends Event implements net.william278.husksync.event.Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

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

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

}
