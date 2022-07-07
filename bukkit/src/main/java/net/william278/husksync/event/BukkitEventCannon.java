package net.william278.husksync.event;

import net.william278.husksync.data.DataSaveCause;
import net.william278.husksync.data.UserData;
import net.william278.husksync.player.BukkitPlayer;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.player.User;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class BukkitEventCannon extends EventCannon {

    public BukkitEventCannon() {
    }

    @Override
    public CompletableFuture<Event> firePreSyncEvent(@NotNull OnlineUser user, @NotNull UserData userData) {
        return new BukkitPreSyncEvent(((BukkitPlayer) user).getPlayer(), userData).fire();
    }

    @Override
    public CompletableFuture<Event> fireDataSaveEvent(@NotNull User user, @NotNull UserData userData,
                                                      @NotNull DataSaveCause saveCause) {
        return new BukkitDataSavePlayerEvent(user, userData, saveCause).fire();
    }

    @Override
    public void fireSyncCompleteEvent(@NotNull OnlineUser user) {
        new BukkitSyncCompletePlayerEvent(((BukkitPlayer) user).getPlayer()).fire();
    }

}
