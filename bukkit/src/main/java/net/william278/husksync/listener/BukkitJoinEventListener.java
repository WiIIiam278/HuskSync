package net.william278.husksync.listener;

import net.william278.husksync.player.BukkitPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

public interface BukkitJoinEventListener extends Listener {

    boolean handleEvent(@NotNull EventListener.ListenerType type, @NotNull EventListener.Priority priority);

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    default void onPlayerJoinHighest(@NotNull PlayerJoinEvent event) {
        if (handleEvent(EventListener.ListenerType.JOIN_LISTENER, EventListener.Priority.HIGHEST)) {
            handlePlayerJoin(BukkitPlayer.adapt(event.getPlayer()));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    default void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        if (handleEvent(EventListener.ListenerType.JOIN_LISTENER, EventListener.Priority.NORMAL)) {
            handlePlayerJoin(BukkitPlayer.adapt(event.getPlayer()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    default void onPlayerJoinLowest(@NotNull PlayerJoinEvent event) {
        if (handleEvent(EventListener.ListenerType.JOIN_LISTENER, EventListener.Priority.LOWEST)) {
            handlePlayerJoin(BukkitPlayer.adapt(event.getPlayer()));
        }
    }

    void handlePlayerJoin(@NotNull BukkitPlayer player);

}
