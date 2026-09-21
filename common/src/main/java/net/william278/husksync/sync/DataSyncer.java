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

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Handles the synchronization of data when a player changes servers or logs in
 *
 * @since 3.1
 */
public abstract class DataSyncer {
    private static final long SHUTDOWN_CRITICAL_DB_ATTEMPTS = 3;
    private static final long SHUTDOWN_CRITICAL_DB_RETRY_BACKOFF_MILLIS = 250;
    private static final long USER_LISTEN_ATTEMPTS = 16;
    private static final long USER_LISTEN_DELAY = 10;

    private static final long MIN_SHUTDOWN_SAVE_TIMEOUT_MILLIS = 5000;
    private static final long MAX_SHUTDOWN_SAVE_TIMEOUT_MILLIS = 50000;

    // Save causes whose failure around a restart can roll a player back and duplicate items:
    // the database write they perform is the only copy of a player's latest state once a
    // pre-restart Redis cache goes stale, so it is verified on write (#persistSnapshot)
    // and preferred on a stale-Redis rejoin (#applyNewestSnapshotFromDB).
    private static final Set<String> SHUTDOWN_CRITICAL_CAUSES = Set.of(
            DataSnapshot.SaveCause.SERVER_SHUTDOWN.name(),
            DataSnapshot.SaveCause.DISCONNECT.name()
    );

    protected final HuskSync plugin;
    private final long maxListenAttempts;
    private final Map<CompletableFuture<Void>, User> pendingSaves = new ConcurrentHashMap<>();

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
     * Save a user's current data, tracking the save so {@link #awaitPendingSaves} can wait for it to
     * complete during shutdown rather than losing it if the plugin disables mid-save
     *
     * @param onlineUser the user to save data for
     * @param cause      the save cause
     * @return A future which will complete once the save (and any resulting Redis write) has finished
     * @since 4.1.0
     */
    public CompletableFuture<Void> saveCurrentUserData(@NotNull OnlineUser onlineUser, @NotNull DataSnapshot.SaveCause cause) {
        return runTrackedAsync(onlineUser, () -> saveData(onlineUser, onlineUser.createSnapshot(cause), (user, data) -> {
            if (!getRedis().setUserData(user, data) && isShutdownCritical(data.getSaveCause())) {
                // Fresh snapshot could not be confirmed on Redis, drop the stale LATEST_SNAPSHOT key. Next login
                // uses the verified db snapshot instead of resurrecting pre-save data, to avoid duplicating items
                getRedis().clearUserData(user);
            }
        }));
    }

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
     * {@code fireDataSaveEvent()} is {@code false} (e.g., with the SERVER_SHUTDOWN cause).
     * @since 3.3.2
     */
    @Blocking
    public void saveData(@NotNull User user, @NotNull DataSnapshot.Packed data,
                         @Nullable BiConsumer<User, DataSnapshot.Packed> after) {
        plugin.debug(String.format("[%s] Saving data (save cause: %s, timestamp: %s, id: %s)",
                user.getName(), data.getSaveCause(), data.getTimestamp(), data.getId()));
        // While disabling, write directly instead of routing through the (fire-and-forget) DataSaveEvent
        // dispatch: that dispatch defers the actual write via further scheduled tasks, so the future
        // returned by #runTrackedAsync would complete before the write happened, letting #awaitPendingSaves
        // close the database/Redis connections too early.
        if (!data.getSaveCause().fireDataSaveEvent() || plugin.isDisabling()) {
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
     * {@code fireDataSaveEvent()} is {@code false} (e.g., with the SERVER_SHUTDOWN cause).
     * @since 3.3.3
     */
    public void saveData(@NotNull User user, @NotNull DataSnapshot.Packed data) {
        saveData(user, data, null);
    }

    // Adds a snapshot to the database and runs the after consumer
    @Blocking
    private void addSnapshotToDatabase(@NotNull User user, @NotNull DataSnapshot.Packed data,
                                       @Nullable BiConsumer<User, DataSnapshot.Packed> after) {
        persistSnapshot(user, data);
        if (after != null) {
            after.accept(user, data);
        }
    }

    // Writes a snapshot to the database. This write is verified and retried in case of a db error
    // during a DISCONNECT save made while the plugin is disabling, to prevent silently dropping a
    // player's latest data, rolling them back and leading to duplicated items on their next login.
    //
    // N.B: a DISCONNECT save gets queued (via #runTrackedAsync) at quit time, when isDisabling() is
    // usually still false, and only runs later on an async thread - so whether this code is reached
    // depends on whether the queued save is still in flight by the time the plugin starts disabling,
    // which is more likely the more that the async queue and shutdown are both under load (e.g. many
    // players quitting at once, any database contention, or other plugins' onDisable() running first).
    @Blocking
    private void persistSnapshot(@NotNull User user, @NotNull DataSnapshot.Packed data) {
        final boolean verifyAndRetry = plugin.isDisabling()
                && DataSnapshot.SaveCause.DISCONNECT.name().equals(data.getSaveCause().name());
        if (!verifyAndRetry) {
            getDatabase().addSnapshot(user, data);
            return;
        }
        for (int attempt = 1; attempt <= SHUTDOWN_CRITICAL_DB_ATTEMPTS; attempt++) {
            final boolean alreadyPersisted = attempt > 1 && getDatabase().getSnapshot(user, data.getId()).isPresent();
            if (!alreadyPersisted) {
                // Only the first attempt runs a normal #addSnapshot and rotates out a previous backup
                if (attempt == 1) {
                    getDatabase().addSnapshot(user, data);
                } else {
                    getDatabase().addSnapshotWithoutRotation(user, data);
                }
            }
            if (alreadyPersisted || getDatabase().getSnapshot(user, data.getId()).isPresent()) {
                return;
            }
            plugin.log(Level.WARNING, "Database save for %s (%s) unconfirmed on attempt %d/%d; retrying".formatted(
                    user.getName(), data.getSaveCause().name(), attempt, SHUTDOWN_CRITICAL_DB_ATTEMPTS));
            if (attempt < SHUTDOWN_CRITICAL_DB_ATTEMPTS) {
                try {
                    Thread.sleep(SHUTDOWN_CRITICAL_DB_RETRY_BACKOFF_MILLIS);
                } catch (InterruptedException ignored) {
                }
            }
        }
        plugin.log(Level.SEVERE, ("Could not confirm %s's data reached the database after %d attempts "
                + "(it may still have been written); check database health").formatted(
                user.getName(), SHUTDOWN_CRITICAL_DB_ATTEMPTS));
    }

    // Calculates the max attempts the system should listen for user data for based on the latency value
    private long getMaxListenAttempts() {
        return USER_LISTEN_ATTEMPTS + (
                (Math.max(100, plugin.getSettings().getSynchronization().getNetworkLatencyMilliseconds()) / 1000)
                        * 20 / USER_LISTEN_DELAY
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
            plugin.log(Level.WARNING, "Failed to set %s's data from the database".formatted(user.getName()), e);
            user.completeSync(false, DataSnapshot.UpdateCause.SYNCHRONIZED, plugin);
        }
    }

    /**
     * Apply the newest database snapshot for a user during sync, given a snapshot read from Redis.
     * <p>
     * On rejoin, the Redis {@code LATEST_SNAPSHOT} key is read and consumed before the database.
     * That key has a long TTL, so if a shutdown/restart could not refresh it (e.g. Redis was
     * unreachable during the shutdown save), an older stale snapshot can survive and would
     * otherwise be applied over the newer, correct database snapshot - rolling the player back
     * and duplicating items.
     * <p>
     * The Redis snapshot is only overridden when the database holds a strictly newer snapshot,
     * whose cause is shutdown-critical (DISCONNECT / SERVER_SHUTDOWN) - i.e. exactly the save
     * that a restart writes but may fail to push to Redis. In every other situation, the Redis
     * snapshot is applied unchanged: a normal cross-server hand-off has the database and Redis
     * snapshots as the same save (with an equal timestamp) since the leaving server writes the
     * database before releasing its checkout, and a newer database snapshot from a non-shutdown
     * cause (e.g. a DB-only DEATH) is ignored, so it can't be applied over a synced Redis state.
     *
     * @param user      the user to apply data to
     * @param redisData the snapshot consumed from Redis
     * @since 4.1.0
     */
    @ApiStatus.Internal
    protected void applyNewestSnapshotFromDB(@NotNull OnlineUser user, @NotNull DataSnapshot.Packed redisData) {
        try {
            final Optional<DataSnapshot.Packed> dbData = getDatabase().getLatestSnapshot(user, SHUTDOWN_CRITICAL_CAUSES);
            if (dbData.isPresent() && dbData.get().getTimestamp().isAfter(redisData.getTimestamp())) {
                plugin.debug(("[%s] Applying newer database snapshot (%s, %s) over older Redis snapshot (%s) "
                        + "to avoid a stale restart rollback").formatted(user.getName(), dbData.get().getTimestamp(),
                        dbData.get().getSaveCause(), redisData.getTimestamp()));
                user.applySnapshot(dbData.get(), DataSnapshot.UpdateCause.SYNCHRONIZED);
                return;
            }
        } catch (Throwable e) {
            plugin.log(Level.WARNING, "[%s] Failed to compare Redis and database snapshots; applying the Redis snapshot"
                    .formatted(user.getName()), e);
        }
        user.applySnapshot(redisData, DataSnapshot.UpdateCause.SYNCHRONIZED);
    }

    // Whether a save cause must be reliably persisted around a restart. A failed save with one of these
    // causes would likely result in player inventory rollback (and item duplication) on the next login.
    protected static boolean isShutdownCritical(@NotNull DataSnapshot.SaveCause cause) {
        return SHUTDOWN_CRITICAL_CAUSES.contains(cause.name());
    }

    // Continuously listen for data from Redis
    @ApiStatus.Internal
    protected void listenForRedisData(@NotNull OnlineUser user, @NotNull Supplier<Boolean> completionSupplier) {
        final AtomicLong timesRun = new AtomicLong(0L);
        final AtomicReference<Task.Repeating> task = new AtomicReference<>();
        final AtomicBoolean processing = new AtomicBoolean(false);
        final Runnable runnable = () -> {
            if (user.cannotApplySnapshot()) {
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
                        user.getName(), timesRun.get()));
                setUserFromDatabase(user);
                return;
            }

            // Fire the completion supplier
            if (completionSupplier.get()) {
                task.get().cancel();
            }
            processing.set(false);
        };
        task.set(plugin.getRepeatingTask(runnable, USER_LISTEN_DELAY));
        task.get().run();
    }

    /**
     * Run a task asynchronously and track it, along with the user it's saving data for, so
     * {@link #awaitPendingSaves} can wait for completion and report which player failed if it does.
     * Subclasses should use this instead of {@code plugin.runAsync()} for disconnect saves.
     *
     * @since 4.1.0
     */
    protected CompletableFuture<Void> runTrackedAsync(@NotNull User user, @NotNull Runnable task) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        plugin.runAsync(() -> {
            try {
                task.run();
                future.complete(null);
            } catch (Throwable t) {
                plugin.log(Level.WARNING, "Failed to save disconnect data for player %s (%s): %s".formatted(
                        user.getName(), user.getUuid(), t.getMessage()), t);
                future.completeExceptionally(t);
            }
        });
        pendingSaves.put(future, user);
        future.whenComplete((v, t) -> pendingSaves.remove(future));
        return future;
    }

    /**
     * Wait for all pending disconnect saves to complete, up to the configured shutdown save timeout.
     * Kept between {@value #MIN_SHUTDOWN_SAVE_TIMEOUT_MILLIS}ms and {@value #MAX_SHUTDOWN_SAVE_TIMEOUT_MILLIS}ms)
     * to leave budget within the server's watchdog. Called during plugin shutdown before connections are closed.
     *
     * @implNote Runs on the main thread - keep the configured timeout below the server's watchdog timeout.
     * @since 4.1.0
     */
    public void awaitPendingSaves() {
        if (pendingSaves.isEmpty()) {
            return;
        }
        final long timeoutMillis = Math.min(MAX_SHUTDOWN_SAVE_TIMEOUT_MILLIS, Math.max(MIN_SHUTDOWN_SAVE_TIMEOUT_MILLIS,
            plugin.getSettings().getSynchronization().getShutdownSaveTimeoutMilliseconds()
        ));
        final Map<CompletableFuture<Void>, User> tracked = Map.copyOf(pendingSaves);
        try {
            CompletableFuture.allOf(tracked.keySet().toArray(CompletableFuture[]::new))
                    .get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            final String unfinished = tracked.entrySet().stream()
                    .filter(entry -> !entry.getKey().isDone())
                    .map(entry -> "%s (%s)".formatted(entry.getValue().getName(), entry.getValue().getUuid()))
                    .collect(Collectors.joining(", "));
            plugin.log(Level.WARNING, ("Timed out during shutdown after %dms waiting for player saves to complete. "
                    + "Data for the following player(s) could appear reverted the next time they join: %s").formatted(
                    timeoutMillis, unfinished.isEmpty() ? "(unknown - completed just after timing out)" : unfinished));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            plugin.log(Level.WARNING, "Interrupted during shutdown while waiting for player saves to complete; "
                    + "some player data may not have been correctly saved to the database or Redis");
        } catch (ExecutionException e) {
            // Failures for pending saves are logged per player in runTrackedAsync()
        }
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
