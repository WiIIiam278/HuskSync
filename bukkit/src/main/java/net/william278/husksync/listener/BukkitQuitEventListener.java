package net.william278.husksync.listener;

import net.william278.husksync.config.Settings;
import net.william278.husksync.player.BukkitPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public interface BukkitQuitEventListener extends Listener {

    boolean handleEvent(@NotNull Settings.EventType type, @NotNull Settings.EventPriority priority);

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    default void onPlayerQuitHighest(@NotNull PlayerQuitEvent event) {
        if (handleEvent(Settings.EventType.QUIT_LISTENER, Settings.EventPriority.HIGHEST)) {
            handlePlayerQuit(BukkitPlayer.adapt(event.getPlayer()));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    default void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        if (handleEvent(Settings.EventType.QUIT_LISTENER, Settings.EventPriority.NORMAL)) {
            handlePlayerQuit(BukkitPlayer.adapt(event.getPlayer()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    default void onPlayerQuitLowest(@NotNull PlayerQuitEvent event) {
        if (handleEvent(Settings.EventType.QUIT_LISTENER, Settings.EventPriority.LOWEST)) {
            handlePlayerQuit(BukkitPlayer.adapt(event.getPlayer()));
        }
    }

    void handlePlayerQuit(@NotNull BukkitPlayer player);

}
