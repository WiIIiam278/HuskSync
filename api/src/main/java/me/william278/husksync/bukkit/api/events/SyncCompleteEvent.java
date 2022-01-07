package me.william278.husksync.bukkit.api.events;

import me.william278.husksync.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an event that will be fired when a {@link Player} has finished being synchronised with the correct {@link PlayerData}.
 */
public class SyncCompleteEvent extends PlayerEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final PlayerData data;

    public SyncCompleteEvent(Player player, PlayerData data) {
        super(player);
        this.data = data;
    }

    /**
     * Returns the {@link PlayerData} which has just been set on the {@link Player}
     * @return The {@link PlayerData} that has been set
     */
    public PlayerData getData() {
        return data;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
