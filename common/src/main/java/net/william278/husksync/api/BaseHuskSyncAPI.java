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

package net.william278.husksync.api;

import net.william278.husksync.HuskSync;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.user.OnlineUser;
import net.william278.husksync.user.User;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * The base implementation of the HuskSync API, containing cross-platform API calls.
 * </p>
 * This class should not be used directly, but rather through platform-specific extending API classes.
 */
@SuppressWarnings("unused")
public abstract class BaseHuskSyncAPI {

    /**
     * <b>(Internal use only)</b> - Instance of the implementing plugin.
     */
    protected final HuskSync plugin;

    /**
     * <b>(Internal use only)</b> - Constructor, instantiating the base API class.
     */
    @ApiStatus.Internal
    protected BaseHuskSyncAPI(@NotNull HuskSync plugin) {
        this.plugin = plugin;
    }

    /**
     * Create a new data snapshot for a user
     *
     * @param user The user to create the snapshot of
     * @return The snapshot
     * @since 3.0
     */
    @NotNull
    public DataSnapshot.Packed createSnapshot(@NotNull OnlineUser user) {
        return snapshotBuilder().saveCause(DataSnapshot.SaveCause.API).buildAndPack();
    }

    /**
     * Get a {@link User}'s current data, as a {@link DataSnapshot.Unpacked}
     * <p>
     * If the user is online, this will create a new snapshot of their data with the {@code API} data save cause.
     * </p>
     * If the user is offline, this will return the latest snapshot of their data if that exists
     * (an empty optional will be returned otherwise).
     *
     * @param user The user to get the data of
     * @return A future containing the user's current data, or an empty optional if the user has no data
     * @since 3.0
     */
    public CompletableFuture<Optional<DataSnapshot.Unpacked>> getCurrentData(@NotNull User user) {
        final CompletableFuture<Optional<DataSnapshot.Unpacked>> future = new CompletableFuture<>();
        plugin.runAsync(
                () -> plugin.getRedisManager()
                        .getUserData(UUID.randomUUID(), user)
                        .thenApply(data -> data.or(() -> plugin.getDatabase().getLatestSnapshot(user)))
                        .thenApply(data -> data.map(snapshot -> snapshot.unpack(plugin)))
                        .thenAccept(future::complete)
        );
        return future;
    }

    public void setCurrentData(@NotNull User user, @NotNull DataSnapshot data) {
        plugin.runAsync(() -> {
            final DataSnapshot.Packed packed = data instanceof DataSnapshot.Unpacked unpacked
                    ? unpacked.pack(plugin) : (DataSnapshot.Packed) data;
            saveSnapshot(user, packed);
            plugin.getRedisManager().sendUserDataUpdate(user, packed);
        });
    }

    /**
     * Get a list of all saved data snapshots for a user
     *
     * @param user The user to get the data snapshots of
     * @return The user's data snapshots
     * @since 3.0
     */
    public CompletableFuture<List<DataSnapshot.Unpacked>> getSnapshots(@NotNull User user) {
        return plugin.supplyAsync(
                () -> plugin.getDatabase().getAllSnapshots(user).stream()
                        .map(snapshot -> snapshot.unpack(plugin))
                        .toList()
        );
    }

    /**
     * Get a specific data snapshot for a user
     *
     * @param user      The user to get the data snapshot of
     * @param versionId The version ID of the snapshot to get
     * @return The user's data snapshot, or an empty optional if the user has no data
     * @see #getSnapshots(User)
     * @since 3.0
     */
    public CompletableFuture<List<DataSnapshot.Unpacked>> getSnapshot(@NotNull User user, @NotNull UUID versionId) {
        return plugin.supplyAsync(
                () -> plugin.getDatabase().getSnapshot(user, versionId).stream()
                        .map(snapshot -> snapshot.unpack(plugin))
                        .toList()
        );
    }

    /**
     * Edit a data snapshot for a user
     *
     * @param user      The user to edit the snapshot of
     * @param versionId The version ID of the snapshot to edit
     * @param editor    The editor function
     * @since 3.0
     */
    public void editSnapshot(@NotNull User user, @NotNull UUID versionId,
                             @NotNull Consumer<DataSnapshot.Unpacked> editor) {
        plugin.runAsync(() -> plugin.getDatabase().getSnapshot(user, versionId).ifPresent(snapshot -> {
            final DataSnapshot.Unpacked unpacked = snapshot.unpack(plugin);
            editor.accept(unpacked);
            plugin.getDatabase().saveSnapshot(user, unpacked.pack(plugin));
        }));
    }

    /**
     * Get the latest data snapshot for a user that has been saved in the database.
     * <p>
     * Not to be confused with {@link #getCurrentData(User)}, which will return the current data of a user
     * if they are online (this method will only return their latest <i>saved</i> snapshot).
     * </p>
     *
     * @param user The user to get the latest data snapshot of
     * @return The user's latest data snapshot, or an empty optional if the user has no data
     * @since 3.0
     */
    public CompletableFuture<Optional<DataSnapshot.Unpacked>> getLatestSnapshot(@NotNull User user) {
        return plugin.supplyAsync(
                () -> plugin.getDatabase().getLatestSnapshot(user).map(snapshot -> snapshot.unpack(plugin))
        );
    }

    /**
     * Edit the latest data snapshot for a user
     *
     * @param user   The user to edit the latest snapshot of
     * @param editor The editor function
     * @since 3.0
     */
    public void editLatestSnapshot(@NotNull User user, @NotNull Consumer<DataSnapshot.Unpacked> editor) {
        plugin.runAsync(() -> plugin.getDatabase().getLatestSnapshot(user).ifPresent(snapshot -> {
            final DataSnapshot.Unpacked unpacked = snapshot.unpack(plugin);
            editor.accept(unpacked);
            plugin.getDatabase().saveSnapshot(user, unpacked.pack(plugin));
        }));
    }

    /**
     * Saves a data snapshot to the database
     *
     * @param user     The user to save the data for
     * @param snapshot The snapshot to save
     * @since 3.0
     */
    public void saveSnapshot(@NotNull User user, @NotNull DataSnapshot snapshot) {
        plugin.runAsync(() -> plugin.getDatabase().saveSnapshot(
                user, snapshot instanceof DataSnapshot.Unpacked unpacked
                        ? unpacked.pack(plugin) : (DataSnapshot.Packed) snapshot
        ));
    }

    /**
     * Delete a data snapshot from the database
     *
     * @param user      The user to delete the snapshot of
     * @param versionId The version ID of the snapshot to delete
     * @return A future which will complete with true if the snapshot was deleted, or false if it wasn't
     * (e.g., if the snapshot didn't exist)
     */
    public CompletableFuture<Boolean> deleteSnapshot(@NotNull User user, @NotNull UUID versionId) {
        return plugin.supplyAsync(() -> plugin.getDatabase().deleteSnapshot(user, versionId));
    }

    /**
     * Get a builder for creating a new data snapshot
     *
     * @return The builder
     * @since 3.0
     */
    @NotNull
    public DataSnapshot.Builder snapshotBuilder() {
        return DataSnapshot.builder(plugin).saveCause(DataSnapshot.SaveCause.API);
    }

    /**
     * An exception indicating the plugin has been accessed before it has been registered.
     */
    static final class NotRegisteredException extends IllegalStateException {

        private static final String MESSAGE = """
                Could not access the HuskSync API as it has not yet been registered. This could be because:
                1) HuskSync has failed to enable successfully
                2) Your plugin isn't set to load after HuskSync has
                   (Check if it set as a (soft)depend in plugin.yml or to load: BEFORE in paper-plugin.yml?)
                3) You are attempting to access HuskSync on plugin construction/before your plugin has enabled.
                4) You have shaded HuskSync into your plugin jar and need to fix your maven/gradle/build script
                   to only include HuskSync as a dependency and not as a shaded dependency.""";

        NotRegisteredException() {
            super(MESSAGE);
        }

    }

}
