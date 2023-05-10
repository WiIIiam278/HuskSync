package net.william278.husksync.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

public interface BukkitDeathEventListener extends Listener {

    boolean handleEvent(@NotNull EventListener.ListenerType type, @NotNull EventListener.Priority priority);

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    default void onPlayerDeathHighest(@NotNull PlayerDeathEvent event) {
        if (handleEvent(EventListener.ListenerType.DEATH_LISTENER, EventListener.Priority.HIGHEST)) {
            handlePlayerDeath(event);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    default void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        if (handleEvent(EventListener.ListenerType.DEATH_LISTENER, EventListener.Priority.NORMAL)) {
            handlePlayerDeath(event);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    default void onPlayerDeathLowest(@NotNull PlayerDeathEvent event) {
        if (handleEvent(EventListener.ListenerType.DEATH_LISTENER, EventListener.Priority.LOWEST)) {
            handlePlayerDeath(event);
        }
    }

    void handlePlayerDeath(@NotNull PlayerDeathEvent player);

}
