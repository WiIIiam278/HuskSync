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
import net.william278.husksync.user.OnlineUser;
import net.william278.husksync.util.Task;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

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
     * Called when a user's data should be fetched and applied to them
     *
     * @param user the user to fetch data for
     */
    public abstract void setUserData(@NotNull OnlineUser user);

    /**
     * Called when a user's data should be serialized and saved
     *
     * @param user the user to save
     */
    public abstract void saveUserData(@NotNull OnlineUser user);

    // Calculates the max attempts the system should listen for user data for based on the latency value
    private long getMaxListenAttempts() {
        return BASE_LISTEN_ATTEMPTS + (
                (Math.max(100, plugin.getSettings().getNetworkLatencyMilliseconds()) / 1000) * 20 / LISTEN_DELAY
        );
    }

    // Set a user's data from the database, or set them as a new user
    @ApiStatus.Internal
    protected void setUserFromDatabase(@NotNull OnlineUser user) {
        plugin.getDatabase().getLatestSnapshot(user).ifPresentOrElse(
                snapshot -> user.applySnapshot(snapshot, DataSnapshot.UpdateCause.SYNCHRONIZED),
                () -> user.completeSync(true, DataSnapshot.UpdateCause.NEW_USER, plugin)
        );
    }

    // Continuously listen for data from Redis
    @ApiStatus.Internal
    protected void listenForRedisData(@NotNull OnlineUser user, @NotNull Supplier<Boolean> completionSupplier) {
        final AtomicLong timesRun = new AtomicLong(0L);
        final AtomicReference<Task.Repeating> task = new AtomicReference<>();
        final Runnable runnable = () -> {
            if (user.isOffline()) {
                task.get().cancel();
                return;
            }
            if (plugin.isDisabling() || timesRun.getAndIncrement() > maxListenAttempts) {
                task.get().cancel();
                setUserFromDatabase(user);
                return;
            }

            if (completionSupplier.get()) {
                task.get().cancel();
            }
        };
        task.set(plugin.getRepeatingTask(runnable, LISTEN_DELAY));
        task.get().run();
    }

    /**
     * Represents the different available default modes of {@link DataSyncer}
     *
     * @since 3.1
     */
    public enum Mode {
        DELAY(DelayDataSyncer::new),
        LOCKSTEP(LockstepDataSyncer::new);

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
