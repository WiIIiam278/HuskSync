package net.william278.husksync.sync;

import net.william278.husksync.HuskSync;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.user.OnlineUser;
import net.william278.husksync.util.Task;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
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
        plugin.runAsyncDelayed(() -> {
            // Fetch from the database if the user isn't changing servers
            if (!plugin.getRedisManager().getUserServerSwitch(user)) {
                setUserFromDatabase(user);
                return;
            }

            // Set the user as soon as the source server has set the data to redis
            final long MAX_ATTEMPTS = 16L;
            final AtomicLong timesRun = new AtomicLong(0L);
            final AtomicReference<Task.Repeating> task = new AtomicReference<>();
            final Runnable runnable = () -> {
                if (user.isOffline()) {
                    task.get().cancel();
                    return;
                }
                if (plugin.isDisabling() || timesRun.getAndIncrement() > MAX_ATTEMPTS) {
                    task.get().cancel();
                    setUserFromDatabase(user);
                    return;
                }

                plugin.getRedisManager().getUserData(user).ifPresent(redisData -> {
                    task.get().cancel();
                    user.applySnapshot(redisData, DataSnapshot.UpdateCause.SYNCHRONIZED);
                });
            };
            task.set(plugin.getRepeatingTask(runnable, 10));
            task.get().run();

        }, Math.max(0, plugin.getSettings().getNetworkLatencyMilliseconds() / 50L));
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
