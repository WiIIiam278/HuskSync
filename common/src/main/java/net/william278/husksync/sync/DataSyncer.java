/*
 * This file is part of HuskSync, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.husksync.sync;

import net.william278.husksync.HuskSync;
import net.william278.husksync.api.HuskSyncAPI;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.database.Database;
import net.william278.husksync.redis.RedisManager;
import net.william278.husksync.user.OnlineUser;
import net.william278.husksync.user.User;
import net.william278.husksync.util.Task;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Handles the synchronization of data when a player changes servers or logs in
 *
 * @since 3.1
 */
public abstract class DataSyncer {
    private static final long BASE_LISTEN_ATTEMPTS = 16;
    private static final long LISTEN_DELAY = 10;

    protected final HuskSync plugin;
    private final long maxListenAttempts;

    @ApiStatus.Internal
    protected DataSyncer(@NotNull HuskSync plugin) {
        this.plugin = plugin;
        this.maxListenAttempts = getMaxListenAttempts();
    }

    /**
     * API-exposed constructor for a {@link DataSyncer}
     *
     * @param api instance of the {@link HuskSyncAPI}
     */
    @SuppressWarnings("unused")
    public DataSyncer(@NotNull HuskSyncAPI api) {
        this(api.getPlugin());
    }

    /**
     * Called when the plugin is enabled
     */
    public void initialize() {
    }

    /**
     * Called when the plugin is disabled
     */
    public void terminate() {
    }

    /**
     * Called when a user's data should be fetched and applied to them as part of a synchronization process
     *
     * @param user the user to fetch data for
     */
    public abstract void syncApplyUserData(@NotNull OnlineUser user);

    /**
     * Called when a user's data should be serialized and saved as part of a synchronization process
     *
     * @param user the user to save
     */
    public abstract void syncSaveUserData(@NotNull OnlineUser user);

    /**
     * Save a {@link DataSnapshot.Packed user's data snapshot} to the database,
     * first firing the {@link net.william278.husksync.event.DataSaveEvent}. This will not update data on Redis.
     *
     * @param user  the user to save the data for
     * @param data  the data to save
     * @param after a consumer to run after data has been saved. Will be run async (off the main thread).
     * @apiNote Data will not be saved if the {@link net.william278.husksync.event.DataSaveEvent} is canceled.
     * Note that this method can also edit the data before saving it.
     * @implNote Note that the {@link net.william278.husksync.event.DataSaveEvent} will <b>not</b> be fired if
     * {@link DataSnapshot.SaveCause#fireDataSaveEvent()} is {@code false} (e.g., with the SERVER_SHUTDOWN cause).
     * @since 3.3.2
     */
    @Blocking
    public void saveData(@NotNull User user, @NotNull DataSnapshot.Packed data,
                         @Nullable BiConsumer<User, DataSnapshot.Packed> after) {
        if (!data.getSaveCause().fireDataSaveEvent()) {
            addSnapshotToDatabase(user, data, after);
            return;
        }
        plugin.fireEvent(
                plugin.getDataSaveEvent(user, data),
                (event) -> addSnapshotToDatabase(user, data, after)
        );
    }

    /**
     * Save a {@link DataSnapshot.Packed user's data snapshot} to the database,
     * first firing the {@link net.william278.husksync.event.DataSaveEvent}. This will not update data on Redis.
     *
     * @param user the user to save the data for
     * @param data the data to save
     * @apiNote Data will not be saved if the {@link net.william278.husksync.event.DataSaveEvent} is canceled.
     * Note that this method can also edit the data before saving it.
     * @implNote Note that the {@link net.william278.husksync.event.DataSaveEvent} will <b>not</b> be fired if
     * {@link DataSnapshot.SaveCause#fireDataSaveEvent()} is {@code false} (e.g., with the SERVER_SHUTDOWN cause).
     * @since 3.3.3
     */
    public void saveData(@NotNull User user, @NotNull DataSnapshot.Packed data) {
        saveData(user, data, null);
    }

    // Adds a snapshot to the database and runs the after consumer
    @Blocking
    private void addSnapshotToDatabase(@NotNull User user, @NotNull DataSnapshot.Packed data,
                                       @Nullable BiConsumer<User, DataSnapshot.Packed> after) {
        getDatabase().addSnapshot(user, data);
        if (after != null) {
            after.accept(user, data);
        }
    }

    // Calculates the max attempts the system should listen for user data for based on the latency value
    private long getMaxListenAttempts() {
        return BASE_LISTEN_ATTEMPTS + (
                (Math.max(100, plugin.getSettings().getSynchronization().getNetworkLatencyMilliseconds()) / 1000)
                * 20 / LISTEN_DELAY
        );
    }

    // Set a user's data from the database, or set them as a new user
    @ApiStatus.Internal
    protected void setUserFromDatabase(@NotNull OnlineUser user) {
        try {
            getDatabase().getLatestSnapshot(user).ifPresentOrElse(
                    snapshot -> user.applySnapshot(snapshot, DataSnapshot.UpdateCause.SYNCHRONIZED),
                    () -> user.completeSync(true, DataSnapshot.UpdateCause.NEW_USER, plugin)
            );
        } catch (Throwable e) {
            plugin.log(Level.WARNING, "Failed to set %s's data from the database".formatted(user.getUsername()), e);
            user.completeSync(false, DataSnapshot.UpdateCause.SYNCHRONIZED, plugin);
        }
    }

    // Continuously listen for data from Redis
    @ApiStatus.Internal
    protected void listenForRedisData(@NotNull OnlineUser user, @NotNull Supplier<Boolean> completionSupplier) {
        final AtomicLong timesRun = new AtomicLong(0L);
        final AtomicReference<Task.Repeating> task = new AtomicReference<>();
        final AtomicBoolean processing = new AtomicBoolean(false);
        final Runnable runnable = () -> {
            if (user.isOffline()) {
                task.get().cancel();
                return;
            }
            // Ensure only one task is running at a time
            if (processing.getAndSet(true)) {
                return;
            }

            // Timeout if the plugin is disabling or the max attempts have been reached
            if (plugin.isDisabling() || timesRun.getAndIncrement() > maxListenAttempts) {
                task.get().cancel();
                plugin.debug(String.format("[%s] Redis timed out after %s attempts; setting from database",
                        user.getUsername(), timesRun.get()));
                setUserFromDatabase(user);
                return;
            }

            // Fire the completion supplier
            if (completionSupplier.get()) {
                task.get().cancel();
            }
            processing.set(false);
        };
        task.set(plugin.getRepeatingTask(runnable, LISTEN_DELAY));
        task.get().run();
    }

    @NotNull
    protected RedisManager getRedis() {
        return plugin.getRedisManager();
    }

    @NotNull
    protected Database getDatabase() {
        return plugin.getDatabase();
    }

    /**
     * Represents the different available default modes of {@link DataSyncer}
     *
     * @since 3.1
     */
    public enum Mode {
        LOCKSTEP(LockstepDataSyncer::new),
        DELAY(DelayDataSyncer::new);

        private final Function<HuskSync, ? extends DataSyncer> supplier;

        Mode(@NotNull Function<HuskSync, ? extends DataSyncer> supplier) {
            this.supplier = supplier;
        }

        @NotNull
        public DataSyncer create(@NotNull HuskSync plugin) {
            return supplier.apply(plugin);
        }

    }


}
