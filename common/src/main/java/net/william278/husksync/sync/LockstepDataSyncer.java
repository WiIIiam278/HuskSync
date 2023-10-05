package net.william278.husksync.sync;

import net.william278.husksync.HuskSync;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

public class LockstepDataSyncer extends DataSyncer {

    public LockstepDataSyncer(@NotNull HuskSync plugin) {
        super(plugin);
    }

    @Override
    public void initialize() {
        plugin.getRedisManager().clearUsersCheckedOutOnServer();
    }

    @Override
    public void terminate() {
        plugin.getRedisManager().clearUsersCheckedOutOnServer();
    }

    @Override
    public void setUserData(@NotNull OnlineUser user) {
        plugin.getRedisManager().getUserCheckedOut(user).thenAccept(checked -> {
            if (checked.isEmpty()) {
                setUserFromDatabase(user);
                plugin.getRedisManager().setUserCheckedOut(user, true);
            } else {
                consumeUserData(user, () -> plugin.getRedisManager().setUserCheckedOut(user, true));
            }
        });
    }

    @Override
    public void saveUserData(@NotNull OnlineUser user) {
        plugin.runAsync(() -> {
            final DataSnapshot.Packed data = user.createSnapshot(DataSnapshot.SaveCause.DISCONNECT);
            plugin.getRedisManager().setUserData(user, data).thenRun(
                    () -> plugin.getRedisManager().setUserCheckedOut(user, false)
            );
        });
    }
}
