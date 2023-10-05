package net.william278.husksync.sync;

import net.william278.husksync.HuskSync;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * A data syncer which applies a network delay before checking the presence of user data
 */
public class DelayDataSyncer extends DataSyncer {

    public DelayDataSyncer(@NotNull HuskSync plugin) {
        super(plugin);
    }

    @Override
    public void setUserData(@NotNull OnlineUser user) {
        plugin.runAsyncDelayed(
                () -> {
                    // Fetch from the database if the user isn't changing servers
                    if (!plugin.getRedisManager().getUserServerSwitch(user)) {
                        setUserFromDatabase(user);
                        return;
                    }
                    consumeUserData(user);
                },
                Math.max(0, plugin.getSettings().getNetworkLatencyMilliseconds() / 50L)
        );
    }

    @Override
    public void saveUserData(@NotNull OnlineUser user) {
        try {
            plugin.getRedisManager().setUserServerSwitch(user).thenRun(() -> {
                final DataSnapshot.Packed data = user.createSnapshot(DataSnapshot.SaveCause.DISCONNECT);
                plugin.getRedisManager().setUserData(user, data);
                plugin.getDatabase().addSnapshot(user, data);
            });
        } catch (Throwable e) {
            plugin.log(Level.SEVERE, "An exception occurred handling a player disconnection", e);
        }
    }

}
