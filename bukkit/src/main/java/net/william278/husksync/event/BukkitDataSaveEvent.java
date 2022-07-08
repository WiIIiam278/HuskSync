package net.william278.husksync.event;

import net.william278.husksync.data.DataSaveCause;
import net.william278.husksync.data.UserData;
import net.william278.husksync.player.User;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BukkitDataSaveEvent extends BukkitEvent implements DataSaveEvent, Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private boolean cancelled = false;
    private UserData userData;
    private final User user;
    private final DataSaveCause saveCause;

    protected BukkitDataSaveEvent(@NotNull User user, @NotNull UserData userData,
                                  @NotNull DataSaveCause saveCause) {
        this.user = user;
        this.userData = userData;
        this.saveCause = saveCause;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @NotNull
    @Override
    public User getUser() {
        return user;
    }

    @Override
    public @NotNull UserData getUserData() {
        return userData;
    }

    @Override
    public void setUserData(@NotNull UserData userData) {
        this.userData = userData;
    }

    @Override
    public @NotNull DataSaveCause getSaveCause() {
        return saveCause;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
