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

import net.william278.desertwell.util.ThrowingConsumer;
import net.william278.husksync.HuskSync;
import net.william278.husksync.adapter.Adaptable;
import net.william278.husksync.data.Data;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.data.Identifier;
import net.william278.husksync.data.Serializer;
import net.william278.husksync.sync.DataSyncer;
import net.william278.husksync.user.OnlineUser;
import net.william278.husksync.user.User;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * The common implementation of the HuskSync API, containing cross-platform API calls.
 * </p>
 * Retrieve an instance of the API class via {@link #getInstance()}.
 *
 * @since 2.0
 */
@SuppressWarnings("unused")
public class HuskSyncAPI {

    // Instance of the plugin
    protected static HuskSyncAPI instance;

    /**
     * <b>(Internal use only)</b> - Instance of the implementing plugin.
     */
    protected final HuskSync plugin;

    /**
     * <b>(Internal use only)</b> - Constructor, instantiating the base API class.
     */
    @ApiStatus.Internal
    protected HuskSyncAPI(@NotNull HuskSync plugin) {
        this.plugin = plugin;
    }

    /**
     * Entrypoint to the HuskSync API on the common platform - returns an instance of the API
     *
     * @return instance of the HuskSync API
     * @since 3.3
     */
    @NotNull
    public static HuskSyncAPI getInstance() {
        if (instance == null) {
            throw new NotRegisteredException();
        }
        return instance;
    }

    /**
     * <b>(Internal use only)</b> - Unregister the API for this platform.
     */
    @ApiStatus.Internal
    public static void unregister() {
        instance = null;
    }

    /**
     * Get a {@link User} by their UUID
     *
     * @param uuid The UUID of the user to get
     * @return A future containing the user, or an empty optional if the user doesn't exist
     * @since 3.0
     */
    @NotNull
    public CompletableFuture<Optional<User>> getUser(@NotNull UUID uuid) {
        return plugin.supplyAsync(() -> plugin.getDatabase().getUser(uuid));
    }

    /**
     * Get an {@link OnlineUser} by their UUID
     *
     * @param uuid the UUID of the user to get
     * @return The {@link OnlineUser} wrapped in an optional, if they are online on <i>this</i> server.
     * @since 3.7.2
     */
    @NotNull
    public Optional<OnlineUser> getOnlineUser(@NotNull UUID uuid) {
        return plugin.getOnlineUser(uuid);
    }

    /**
     * Get a {@link User} by their username
     *
     * @param username The username of the user to get
     * @return A future containing the user, or an empty optional if the user doesn't exist
     * @since 3.0
     */
    @NotNull
    public CompletableFuture<Optional<User>> getUser(@NotNull String username) {
        return plugin.supplyAsync(() -> plugin.getDatabase().getUserByName(username));
    }

    /**
     * Create a new data snapshot of an {@link OnlineUser}'s data.
     *
     * @param user The user to create the snapshot of
     * @return The snapshot of the user's data
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
        return plugin.getRedisManager()
                .getUserData(UUID.randomUUID(), user)
                .thenApply(data -> data.or(() -> plugin.getDatabase().getLatestSnapshot(user)))
                .thenApply(data -> data.map(snapshot -> snapshot.unpack(plugin)));
    }

    /**
     * Set a user's current data.
     * <p>
     * This will update the user's data in the database (creating a new snapshot) and send a data update,
     * updating the user if they are online.
     *
     * @param user The user to set the data of
     * @param data The data to set
     * @since 3.0
     */
    public void setCurrentData(@NotNull User user, @NotNull DataSnapshot data) {
        plugin.runAsync(() -> {
            final DataSnapshot.Packed packed = data instanceof DataSnapshot.Unpacked unpacked
                    ? unpacked.pack(plugin) : (DataSnapshot.Packed) data;
            addSnapshot(user, packed);
            plugin.getRedisManager().sendUserDataUpdate(user, packed);
        });
    }

    /**
     * Edit a user's current data.
     * <p>
     * This will update the user's data in the database (creating a new snapshot) and send a data update,
     * updating the user if they are online.
     *
     * @param user   The user to edit the data of
     * @param editor The editor function
     * @since 3.0
     */
    public void editCurrentData(@NotNull User user, @NotNull ThrowingConsumer<DataSnapshot.Unpacked> editor) {
        getCurrentData(user).thenAccept(optional -> optional.ifPresent(data -> {
            editor.accept(data);
            data.setId(UUID.randomUUID());
            setCurrentData(user, data);
        }));
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
                             @NotNull ThrowingConsumer<DataSnapshot.Unpacked> editor) {
        plugin.runAsync(() -> plugin.getDatabase().getSnapshot(user, versionId).ifPresent(snapshot -> {
            final DataSnapshot.Unpacked unpacked = snapshot.unpack(plugin);
            editor.accept(unpacked);
            plugin.getDatabase().updateSnapshot(user, unpacked.pack(plugin));
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
    public void editLatestSnapshot(@NotNull User user, @NotNull ThrowingConsumer<DataSnapshot.Unpacked> editor) {
        plugin.runAsync(() -> plugin.getDatabase().getLatestSnapshot(user).ifPresent(snapshot -> {
            final DataSnapshot.Unpacked unpacked = snapshot.unpack(plugin);
            editor.accept(unpacked);
            plugin.getDatabase().updateSnapshot(user, unpacked.pack(plugin));
        }));
    }

    /**
     * Adds a data snapshot to the database
     *
     * @param user     The user to save the data for
     * @param snapshot The snapshot to save
     * @param callback A callback to run after the data has been saved (if the DataSaveEvent was not canceled)
     * @implNote Note that the {@link net.william278.husksync.event.DataSaveEvent} will be fired unless the
     * {@link DataSnapshot.SaveCause#fireDataSaveEvent()} is {@code false}
     * @since 3.3.2
     */
    public void addSnapshot(@NotNull User user, @NotNull DataSnapshot snapshot,
                            @Nullable BiConsumer<User, DataSnapshot.Packed> callback) {
        plugin.runAsync(() -> plugin.getDataSyncer().saveData(
                user,
                snapshot instanceof DataSnapshot.Unpacked unpacked
                        ? unpacked.pack(plugin) : (DataSnapshot.Packed) snapshot,
                callback
        ));
    }

    /**
     * Adds a data snapshot to the database
     *
     * @param user     The user to save the data for
     * @param snapshot The snapshot to save
     * @implNote Note that the {@link net.william278.husksync.event.DataSaveEvent} will be fired unless the
     * {@link DataSnapshot.SaveCause#fireDataSaveEvent()} is {@code false}
     * @since 3.0
     */
    public void addSnapshot(@NotNull User user, @NotNull DataSnapshot snapshot) {
        this.addSnapshot(user, snapshot, null);
    }

    /**
     * Update an <i>existing</i> data snapshot in the database.
     * Not to be confused with {@link #addSnapshot(User, DataSnapshot)}, which will add a new snapshot if one
     * snapshot doesn't exist.
     *
     * @param user     The user to update the snapshot of
     * @param snapshot The snapshot to update
     * @since 3.0
     */
    public void updateSnapshot(@NotNull User user, @NotNull DataSnapshot snapshot) {
        plugin.runAsync(() -> plugin.getDatabase().updateSnapshot(
                user, snapshot instanceof DataSnapshot.Unpacked unpacked
                        ? unpacked.pack(plugin) : (DataSnapshot.Packed) snapshot
        ));
    }

    /**
     * Pin a data snapshot, preventing it from being rotated
     *
     * @param user            The user to pin the snapshot of
     * @param snapshotVersion The version ID of the snapshot to pin
     * @since 3.0
     */
    public void pinSnapshot(@NotNull User user, @NotNull UUID snapshotVersion) {
        plugin.runAsync(() -> plugin.getDatabase().pinSnapshot(user, snapshotVersion));
    }

    /**
     * Unpin a data snapshot, allowing it to be rotated
     *
     * @param user            The user to unpin the snapshot of
     * @param snapshotVersion The version ID of the snapshot to unpin
     * @since 3.0
     */
    public void unpinSnapshot(@NotNull User user, @NotNull UUID snapshotVersion) {
        plugin.runAsync(() -> plugin.getDatabase().unpinSnapshot(user, snapshotVersion));
    }

    /**
     * Delete a data snapshot from the database
     *
     * @param user      The user to delete the snapshot of
     * @param versionId The version ID of the snapshot to delete
     * @return A future which will complete with true if the snapshot was deleted, or false if it wasn't
     * (e.g., if the snapshot didn't exist)
     * @since 3.0
     */
    public CompletableFuture<Boolean> deleteSnapshot(@NotNull User user, @NotNull UUID versionId) {
        return plugin.supplyAsync(() -> plugin.getDatabase().deleteSnapshot(user, versionId));
    }

    /**
     * Delete a data snapshot from the database
     *
     * @param user     The user to delete the snapshot of
     * @param snapshot The snapshot to delete
     * @return A future which will complete with true if the snapshot was deleted, or false if it wasn't
     * (e.g., if the snapshot hasn't been saved to the database yet)
     * @since 3.0
     */
    public CompletableFuture<Boolean> deleteSnapshot(@NotNull User user, @NotNull DataSnapshot snapshot) {
        return deleteSnapshot(user, snapshot.getId());
    }

    /**
     * Registers a new custom data type serializer.
     * <p>
     * This allows for custom {@link Data} types to be persisted in {@link DataSnapshot}s. To register
     * a new data type, you must provide a {@link Serializer} for serializing and deserializing the data type
     * and invoke this method.
     * </p>
     * You'll need to do this on every server you wish to sync data between. On servers where the registered
     * data type is not present, the data will be ignored and snapshots created on that server will not
     * contain the data.
     *
     * @param identifier The identifier of the data type to register.
     *                   Create one using {@code Identifier.from(Key.of("your_plugin_name", "key"))}
     * @param serializer An implementation of {@link Serializer} for serializing and deserializing the {@link Data}
     * @param <T>        A type extending {@link Data}; this will represent the data being held.
     */
    public <T extends Data> void registerDataSerializer(@NotNull Identifier identifier,
                                                        @NotNull Serializer<T> serializer) {
        plugin.registerSerializer(identifier, serializer);
    }

    /**
     * Get a registered data serializer by its identifier
     *
     * @param identifier The identifier of the data type to get the serializer for
     * @return The serializer for the given identifier, or an empty optional if the serializer isn't registered
     * @since 3.5.4
     */
    public Optional<Serializer<Data>> getDataSerializer(@NotNull Identifier identifier) {
        return plugin.getSerializer(identifier);
    }

    /**
     * Get a {@link DataSnapshot.Unpacked} from a {@link DataSnapshot.Packed}
     *
     * @param unpacked The unpacked snapshot
     * @return The packed snapshot
     * @since 3.0
     */
    @NotNull
    public DataSnapshot.Packed packSnapshot(@NotNull DataSnapshot.Unpacked unpacked) {
        return unpacked.pack(plugin);
    }

    /**
     * Get a {@link DataSnapshot.Unpacked} from a {@link DataSnapshot.Packed}
     *
     * @param packed The packed snapshot
     * @return The unpacked snapshot
     * @since 3.0
     */
    @NotNull
    public DataSnapshot.Unpacked unpackSnapshot(@NotNull DataSnapshot.Packed packed) {
        return packed.unpack(plugin);
    }

    /**
     * Unpack, edit, and repack a data snapshot.
     * </p>
     * This won't save the snapshot to the database; it'll just edit the data snapshot in place.
     *
     * @param packed The packed snapshot
     * @param editor An editor function for editing the unpacked snapshot
     * @return The edited packed snapshot
     * @since 3.0
     */
    @NotNull
    public DataSnapshot.Packed editPackedSnapshot(@NotNull DataSnapshot.Packed packed,
                                                  @NotNull ThrowingConsumer<DataSnapshot.Unpacked> editor) {
        final DataSnapshot.Unpacked unpacked = packed.unpack(plugin);
        editor.accept(unpacked);
        return unpacked.pack(plugin);
    }

    /**
     * Get the estimated size of a {@link DataSnapshot} in bytes
     *
     * @param snapshot The snapshot to get the size of
     * @return The size of the snapshot in bytes
     * @since 3.0
     */
    public int getSnapshotFileSize(@NotNull DataSnapshot snapshot) {
        return (snapshot instanceof DataSnapshot.Packed packed)
                ? packed.getFileSize(plugin)
                : ((DataSnapshot.Unpacked) snapshot).pack(plugin).getFileSize(plugin);
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
     * Deserialize a JSON string to an {@link Adaptable}
     *
     * @param serialized The serialized JSON string
     * @param type       The type of the element
     * @param <T>        The type of the element
     * @return The deserialized element
     * @throws Serializer.DeserializationException If the element could not be deserialized
     * @since 3.0
     */
    @NotNull
    public <T extends Adaptable> T deserializeData(@NotNull String serialized, Class<T> type)
            throws Serializer.DeserializationException {
        return plugin.getDataAdapter().fromJson(serialized, type);
    }

    /**
     * Serialize an {@link Adaptable} to a JSON string
     *
     * @param element The element to serialize
     * @param <T>     The type of the element
     * @return The serialized JSON string
     * @throws Serializer.SerializationException If the element could not be serialized
     * @since 3.0
     */
    @NotNull
    public <T extends Adaptable> String serializeData(@NotNull T element)
            throws Serializer.SerializationException {
        return plugin.getDataAdapter().toJson(element);
    }

    /**
     * Set the {@link DataSyncer} to be used to sync data
     *
     * @param syncer The data syncer to use for synchronizing user data
     * @since 3.1
     */
    public void setDataSyncer(@NotNull DataSyncer syncer) {
        plugin.setDataSyncer(syncer);
    }

    /**
     * <b>(Internal use only)</b> - Get the plugin instance
     *
     * @return The plugin instance
     */
    @ApiStatus.Internal
    public HuskSync getPlugin() {
        return plugin;
    }

    /**
     * An exception indicating the plugin has been accessed before it has been registered.
     */
    static final class NotRegisteredException extends IllegalStateException {

        private static final String REASONS = """
                This may be because:
                1) HuskSync has failed to enable successfully
                2) Your plugin isn't set to load after HuskSync has
                   (Check if it set as a (soft)depend in plugin.yml or to load: BEFORE in paper-plugin.yml?)
                3) You are attempting to access HuskSync on plugin construction/before your plugin has enabled.""";

        NotRegisteredException(@NotNull String reasons) {
            super("Could not access the HuskSync API as it has not yet been registered. %s".formatted(reasons));
        }

        NotRegisteredException() {
            this(REASONS);
        }

    }

}
