package net.william278.husksync.event;

import net.william278.husksync.data.DataSaveCause;
import net.william278.husksync.data.UserData;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.player.User;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public abstract class EventCannon {

    protected EventCannon() {
    }

    public abstract CompletableFuture<Event> firePreSyncEvent(@NotNull OnlineUser user, @NotNull UserData userData);

    public abstract CompletableFuture<Event> fireDataSaveEvent(@NotNull User user, @NotNull UserData userData,
                                                                     @NotNull DataSaveCause saveCause);

    public abstract void fireSyncCompleteEvent(@NotNull OnlineUser user);

}
