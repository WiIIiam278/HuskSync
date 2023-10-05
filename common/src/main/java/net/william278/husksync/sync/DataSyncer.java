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
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.user.OnlineUser;
import net.william278.husksync.util.Task;
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
    private final long maxListenAttempts;

    protected final HuskSync plugin;

    protected DataSyncer(@NotNull HuskSync plugin) {
        this.plugin = plugin;
        this.maxListenAttempts = getMaxListenAttempts();
    }

    public void initialize() {
    }

    public void terminate() {
    }

    public abstract void setUserData(@NotNull OnlineUser user);

    public abstract void saveUserData(@NotNull OnlineUser user);

    private long getMaxListenAttempts() {
        return BASE_LISTEN_ATTEMPTS + ((plugin.getSettings().getNetworkLatencyMilliseconds() / 1000) * 20 / LISTEN_DELAY);
    }

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
