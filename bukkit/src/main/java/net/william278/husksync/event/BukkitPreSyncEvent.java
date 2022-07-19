package net.william278.husksync.event;

import net.william278.husksync.data.UserData;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class BukkitPreSyncEvent extends BukkitPlayerEvent implements PreSyncEvent, Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private boolean cancelled = false;
    private UserData userData;

    protected BukkitPreSyncEvent(@NotNull Player player, @NotNull UserData userData) {
        super(player);
        this.userData = userData;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public @NotNull UserData getUserData() {
        return userData;
    }

    @Override
    public void setUserData(@NotNull UserData userData) {
        this.userData = userData;
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
