package net.william278.husksync.listener;

import net.william278.husksync.config.Settings;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

public interface BukkitDeathEventListener extends Listener {

    boolean handleEvent(@NotNull Settings.EventType type, @NotNull Settings.EventPriority priority);

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    default void onPlayerDeathHighest(@NotNull PlayerDeathEvent event) {
        if (handleEvent(Settings.EventType.DEATH_LISTENER, Settings.EventPriority.HIGHEST)) {
            handlePlayerDeath(event);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    default void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        if (handleEvent(Settings.EventType.DEATH_LISTENER, Settings.EventPriority.NORMAL)) {
            handlePlayerDeath(event);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    default void onPlayerDeathLowest(@NotNull PlayerDeathEvent event) {
        if (handleEvent(Settings.EventType.DEATH_LISTENER, Settings.EventPriority.LOWEST)) {
            handlePlayerDeath(event);
        }
    }

    void handlePlayerDeath(@NotNull PlayerDeathEvent player);

}
