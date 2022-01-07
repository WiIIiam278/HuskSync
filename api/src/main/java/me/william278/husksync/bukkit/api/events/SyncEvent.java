package me.william278.husksync.bukkit.api.events;

import me.william278.husksync.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an event that will be fired before a {@link Player} is about to be synchronised with their {@link PlayerData}.
 */
public class SyncEvent extends PlayerEvent implements Cancellable {

    private boolean cancelled;
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private PlayerData data;

    public SyncEvent(Player player, PlayerData data) {
        super(player);
        this.data = data;
    }

    /**
     * Returns the {@link PlayerData} which has just been set on the {@link Player}
     *
     * @return The {@link PlayerData} that has been set
     */
    public PlayerData getData() {
        return data;
    }

    /**
     * Sets the {@link PlayerData} to be synchronised to this player
     *
     * @param data The {@link PlayerData} to set to the player
     */
    public void setData(PlayerData data) {
        this.data = data;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    /**
     * Gets the cancellation state of this event. A cancelled event will not be executed in the server, but will still pass to other plugins
     *
     * @return true if this event is cancelled
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets the cancellation state of this event. A cancelled event will not be executed in the server, but will still pass to other plugins.
     *
     * @param cancel true if you wish to cancel this event
     */
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
