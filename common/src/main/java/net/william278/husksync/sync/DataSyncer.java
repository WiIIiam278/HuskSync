package net.william278.husksync.sync;

import net.william278.husksync.HuskSync;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Handles the synchronization of data when a player changes servers or logs in
 * @since 3.1
 */
public abstract class DataSyncer {

    protected final HuskSync plugin;

    protected DataSyncer(@NotNull HuskSync plugin) {
        this.plugin = plugin;
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
     * Represents a type of {@link DataSyncer}
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
