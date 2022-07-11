package net.william278.husksync.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BukkitSyncCompleteEvent extends BukkitPlayerEvent implements SyncCompleteEvent {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    protected BukkitSyncCompleteEvent(@NotNull Player player) {
        super(player);
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
