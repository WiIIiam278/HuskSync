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

package net.william278.husksync.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import net.william278.desertwell.util.Version;
import net.william278.husksync.HuskSync;
import net.william278.husksync.adapter.Adaptable;
import net.william278.husksync.adapter.DataAdapter;
import net.william278.husksync.config.Locales;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A snapshot of a {@link DataHolder} at a given time.
 */
public class DataSnapshot {

    /*
     * Current version of the snapshot data format.
     * HuskSync v3.0 uses v4; HuskSync v2.0 uses v3. HuskSync v1.0 uses v1 or v2
     */
    protected static final int CURRENT_FORMAT_VERSION = 4;

    @SerializedName("id")
    protected UUID id;

    @SerializedName("pinned")
    protected boolean pinned;

    @SerializedName("timestamp")
    protected OffsetDateTime timestamp;

    @SerializedName("save_cause")
    protected SaveCause saveCause;

    @SerializedName("minecraft_version")
    protected String minecraftVersion;

    @SerializedName("platform_type")
    protected String platformType;

    @SerializedName("format_version")
    protected int formatVersion;

    @SerializedName("data")
    protected Map<String, String> data;

    private DataSnapshot(@NotNull UUID id, boolean pinned, @NotNull OffsetDateTime timestamp,
                         @NotNull SaveCause saveCause, @NotNull Map<String, String> data,
                         @NotNull Version minecraftVersion, @NotNull String platformType, int formatVersion) {
        this.id = id;
        this.pinned = pinned;
        this.timestamp = timestamp;
        this.saveCause = saveCause;
        this.data = data;
        this.minecraftVersion = minecraftVersion.toStringWithoutMetadata();
        this.platformType = platformType;
        this.formatVersion = formatVersion;
    }

    @SuppressWarnings("unused")
    private DataSnapshot() {
    }

    @NotNull
    public static DataSnapshot.Packed create(@NotNull HuskSync plugin,
                                             @NotNull Map<Identifier, Data> data,
                                             @NotNull SaveCause saveCause) {
        return new Unpacked(
                UUID.randomUUID(), false, OffsetDateTime.now(), saveCause, data,
                plugin.getMinecraftVersion(), plugin.getPlatformType(), DataSnapshot.CURRENT_FORMAT_VERSION
        ).pack(plugin);
    }

    @NotNull
    protected static DataSnapshot.Packed create(@NotNull HuskSync plugin, @NotNull PlayerDataHolder owner,
                                                @NotNull SaveCause saveCause) {
        return new DataSnapshot.Unpacked(
                UUID.randomUUID(), false, OffsetDateTime.now(), saveCause, owner.getData(),
                plugin.getMinecraftVersion(), plugin.getPlatformType(), CURRENT_FORMAT_VERSION
        ).pack(plugin);
    }

    @NotNull
    public static DataSnapshot.Packed deserialize(@NotNull HuskSync plugin, byte[] data) throws IllegalStateException {
        final DataSnapshot.Packed snapshot = plugin.getDataAdapter().fromBytes(data, DataSnapshot.Packed.class);
        if (snapshot.getMinecraftVersion().compareTo(plugin.getMinecraftVersion()) > 0) {
            throw new IllegalStateException(String.format("Cannot set data for user because the Minecraft version of " +
                            "their user data (%s) is newer than the server's Minecraft version (%s)." +
                            "Please ensure each server is running the same version of Minecraft.",
                    snapshot.getMinecraftVersion(), plugin.getMinecraftVersion()));
        }
        if (snapshot.getFormatVersion() > CURRENT_FORMAT_VERSION) {
            throw new IllegalStateException(String.format("Cannot set data for user because the format version of " +
                            "their user data (%s) is newer than the current format version (%s). " +
                            "Please ensure each server is running the latest version of HuskSync.",
                    snapshot.getFormatVersion(), CURRENT_FORMAT_VERSION));
        }
        if (snapshot.getFormatVersion() < CURRENT_FORMAT_VERSION) {
            if (plugin.getLegacyConverter().isPresent()) {
                return plugin.getLegacyConverter().get().convert(data);
            }
            throw new IllegalStateException(String.format(
                    "No legacy converter to convert format version: %s", snapshot.getFormatVersion()
            ));
        }
        if (!snapshot.getPlatformType().equalsIgnoreCase(plugin.getPlatformType())) {
            throw new IllegalStateException(String.format("Cannot set data for user because the platform type of " +
                            "their user data (%s) is different to the server platform type (%s). " +
                            "Please ensure each server is running the same platform type.",
                    snapshot.getPlatformType(), plugin.getPlatformType()));
        }
        return snapshot;
    }

    @NotNull
    public UUID getId() {
        return id;
    }

    @NotNull
    public String getShortId() {
        return id.toString().substring(0, 8);
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public SaveCause getSaveCause() {
        return saveCause;
    }

    @NotNull
    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    @NotNull
    public Version getMinecraftVersion() {
        return Version.fromString(minecraftVersion);
    }

    @NotNull
    public String getPlatformType() {
        return platformType;
    }

    public int getFormatVersion() {
        return formatVersion;
    }

    /**
     * A packed {@link DataSnapshot} that has not been deserialized.
     */
    public static class Packed extends DataSnapshot implements Adaptable {

        protected Packed(@NotNull UUID id, boolean pinned, @NotNull OffsetDateTime timestamp,
                         @NotNull SaveCause saveCause, @NotNull Map<String, String> data,
                         @NotNull Version minecraftVersion, @NotNull String platformType, int formatVersion) {
            super(id, pinned, timestamp, saveCause, data, minecraftVersion, platformType, formatVersion);
        }

        @SuppressWarnings("unused")
        private Packed() {
        }

        public void edit(@NotNull HuskSync plugin, @NotNull Function<Unpacked, Unpacked> editor) {
            final Unpacked edited = editor.apply(unpack(plugin));
            this.pinned = edited.isPinned();
            this.data = edited.serializeData(plugin);
        }

        @NotNull
        public Packed copy() {
            return new Packed(
                    UUID.randomUUID(), pinned, OffsetDateTime.now(), saveCause, data,
                    getMinecraftVersion(), platformType, formatVersion
            );
        }

        @NotNull
        public byte[] asBytes(@NotNull HuskSync plugin) throws DataAdapter.AdaptionException {
            return plugin.getDataAdapter().toBytes(this);
        }

        @NotNull
        public String asJson(@NotNull HuskSync plugin) throws DataAdapter.AdaptionException {
            return plugin.getDataAdapter().toJson(this);
        }

        public int getFileSize(@NotNull HuskSync plugin) {
            return asBytes(plugin).length;
        }

        @NotNull
        public DataSnapshot.Unpacked unpack(@NotNull HuskSync plugin) {
            return new Unpacked(
                    id, pinned, timestamp, saveCause, data,
                    getMinecraftVersion(), platformType, formatVersion, plugin
            );
        }

    }

    /**
     * An unpacked {@link DataSnapshot}.
     */
    public static class Unpacked extends DataSnapshot implements DataHolder {

        @Expose(serialize = false, deserialize = false)
        private final Map<Identifier, Data> deserialized;

        private Unpacked(@NotNull UUID id, boolean pinned, @NotNull OffsetDateTime timestamp,
                         @NotNull SaveCause saveCause, @NotNull Map<String, String> data,
                         @NotNull Version minecraftVersion, @NotNull String platformType, int formatVersion,
                         @NotNull HuskSync plugin) {
            super(id, pinned, timestamp, saveCause, data, minecraftVersion, platformType, formatVersion);
            this.deserialized = deserializeData(plugin);
        }

        private Unpacked(@NotNull UUID id, boolean pinned, @NotNull OffsetDateTime timestamp,
                         @NotNull SaveCause saveCause, @NotNull Map<Identifier, Data> data,
                         @NotNull Version minecraftVersion, @NotNull String platformType, int formatVersion) {
            super(id, pinned, timestamp, saveCause, Map.of(), minecraftVersion, platformType, formatVersion);
            this.deserialized = data;
        }

        @NotNull
        private Map<Identifier, Data> deserializeData(@NotNull HuskSync plugin) {
            return data.entrySet().stream()
                    .map((entry) -> plugin.getIdentifier(entry.getKey()).map(id -> Map.entry(
                            id, plugin.getSerializers().get(id).deserialize(entry.getValue())
                    )).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        @NotNull
        private Map<String, String> serializeData(@NotNull HuskSync plugin) {
            return deserialized.entrySet().stream()
                    .map((entry) -> Map.entry(entry.getKey().toString(),
                            Objects.requireNonNull(
                                    plugin.getSerializers().get(entry.getKey()),
                                    String.format("No serializer found for %s", entry.getKey())
                            ).serialize(entry.getValue())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        @NotNull
        public Map<Identifier, Data> getData() {
            return deserialized;
        }

        @NotNull
        public DataSnapshot.Packed pack(@NotNull HuskSync plugin) {
            return new DataSnapshot.Packed(
                    id, pinned, timestamp, saveCause, serializeData(plugin),
                    getMinecraftVersion(), platformType, formatVersion
            );
        }

    }

    /**
     * Identifies the cause of a player data save.
     *
     * @implNote This enum is saved in the database.
     * </p>
     * Cause names have a max length of 32 characters.
     */
    public enum SaveCause {

        /**
         * Indicates data saved when a player disconnected from the server (either to change servers, or to log off)
         *
         * @since 2.0
         */
        DISCONNECT,
        /**
         * Indicates data saved when the world saved
         *
         * @since 2.0
         */
        WORLD_SAVE,
        /**
         * Indicates data saved when the user died
         *
         * @since 2.1
         */
        DEATH,
        /**
         * Indicates data saved when the server shut down
         *
         * @since 2.0
         */
        SERVER_SHUTDOWN,
        /**
         * Indicates data was saved by editing inventory contents via the {@code /inventory} command
         *
         * @since 2.0
         */
        INVENTORY_COMMAND,
        /**
         * Indicates data was saved by editing Ender Chest contents via the {@code /enderchest} command
         *
         * @since 2.0
         */
        ENDERCHEST_COMMAND,
        /**
         * Indicates data was saved by restoring it from a previous version
         *
         * @since 2.0
         */
        BACKUP_RESTORE,
        /**
         * Indicates data was saved by an API call
         *
         * @since 2.0
         */
        API,
        /**
         * Indicates data was saved from being imported from MySQLPlayerDataBridge
         *
         * @since 2.0
         */
        MPDB_MIGRATION,
        /**
         * Indicates data was saved from being imported from a legacy version (v1.x -> v2.x)
         *
         * @since 2.0
         */
        LEGACY_MIGRATION,
        /**
         * Indicates data was saved from being imported from a legacy version (v2.x -> v3.x)
         *
         * @since 3.0
         */
        CONVERTED_FROM_V2;

        @NotNull
        public String getDisplayName() {
            return Locales.truncate(name().toLowerCase(Locale.ENGLISH)
                    .replaceAll("_", " "), 20);
        }

    }
}
