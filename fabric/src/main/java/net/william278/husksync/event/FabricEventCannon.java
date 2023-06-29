package net.william278.husksync.event;

import net.william278.husksync.data.DataSaveCause;
import net.william278.husksync.data.UserData;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.player.User;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

// TODO: FabricEventCannon
public class FabricEventCannon extends EventCannon {
    @Override
    public CompletableFuture<Event> firePreSyncEvent(@NotNull OnlineUser user, @NotNull UserData userData) {
        return null;
    }

    @Override
    public CompletableFuture<Event> fireDataSaveEvent(@NotNull User user, @NotNull UserData userData, @NotNull DataSaveCause saveCause) {
        return null;
    }

    @Override
    public void fireSyncCompleteEvent(@NotNull OnlineUser user) {

    }
}
