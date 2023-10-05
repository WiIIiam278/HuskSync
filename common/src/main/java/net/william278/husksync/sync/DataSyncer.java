package net.william278.husksync.sync;

import net.william278.husksync.HuskSync;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.user.OnlineUser;
import net.william278.husksync.util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Handles the synchronization of data when a player changes servers or logs in
 *
 * @since 3.1
 */
public abstract class DataSyncer {

    private final long MAX_LISTEN_ATTEMPTS = 16L;

    protected final HuskSync plugin;

    protected DataSyncer(@NotNull HuskSync plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
    }

    public void terminate() {
    }

    public abstract void setUserData(@NotNull OnlineUser user);

    public abstract void saveUserData(@NotNull OnlineUser user);


    /**
     * Set a user's data from the database
     *
     * @param user The user to set the data for
     */
    protected void setUserFromDatabase(@NotNull OnlineUser user) {
        plugin.getDatabase().getLatestSnapshot(user).ifPresentOrElse(
                snapshot -> user.applySnapshot(snapshot, DataSnapshot.UpdateCause.SYNCHRONIZED),
                () -> user.completeSync(true, DataSnapshot.UpdateCause.NEW_USER, plugin)
        );
    }

    /**
     * Set the user as soon as the source server has set the data to redis
     *
     * @param user       The user to set the data for
     * @param onConsumed The runnable to run when the data has been fetched
     * @since 3.1
     */
    protected void consumeUserData(@NotNull OnlineUser user, @Nullable Runnable onConsumed) {
        final AtomicLong timesRun = new AtomicLong(0L);
        final AtomicReference<Task.Repeating> task = new AtomicReference<>();
        final Runnable runnable = () -> {
            if (user.isOffline()) {
                task.get().cancel();
                return;
            }
            if (plugin.isDisabling() || timesRun.getAndIncrement() > MAX_LISTEN_ATTEMPTS) {
                task.get().cancel();
                setUserFromDatabase(user);
                if (onConsumed != null) {
                    onConsumed.run();
                }
                return;
            }

            plugin.getRedisManager().getUserData(user).ifPresent(redisData -> {
                task.get().cancel();
                user.applySnapshot(redisData, DataSnapshot.UpdateCause.SYNCHRONIZED);
                if (onConsumed != null) {
                    onConsumed.run();
                }
            });
        };
        task.set(plugin.getRepeatingTask(runnable, 10));
        task.get().run();
    }

    /**
     * Set the user as soon as the source server has set the data to redis
     *
     * @param user The user to set the data for
     * @since 3.1
     */
    protected void consumeUserData(@NotNull OnlineUser user) {
        this.consumeUserData(user, null);
    }

    /**
     * Represents a type of {@link DataSyncer}
     *
     * @since 3.1
     */
    public enum Type {
        DELAY(DelayDataSyncer::new),
        LOCKSTEP(LockstepDataSyncer::new);

        private final Function<HuskSync, ? extends DataSyncer> supplier;

        Type(@NotNull Function<HuskSync, ? extends DataSyncer> supplier) {
            this.supplier = supplier;
        }

        @NotNull
        public DataSyncer create(@NotNull HuskSync plugin) {
            return supplier.apply(plugin);
        }

    }


}
