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

import com.google.common.collect.Maps;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import de.themoep.minedown.adventure.MineDown;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import net.william278.desertwell.util.Version;
import net.william278.husksync.HuskSync;
import net.william278.husksync.adapter.Adaptable;
import net.william278.husksync.adapter.DataAdapter;
import org.apache.commons.text.WordUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A snapshot of a {@link DataHolder} at a given time.
 *
 * @since 3.0
 */
@SuppressWarnings({"LombokSetterMayBeUsed", "LombokGetterMayBeUsed"})
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DataSnapshot {

    /*
     * Current version of the snapshot data format.
     * HuskSync v3.1 uses v5, v3.0 uses v4; v2.0 uses v1-v3
     */
    protected static final int CURRENT_FORMAT_VERSION = 5;

    @SerializedName("id")
    protected UUID id;

    @SerializedName("pinned")
    protected boolean pinned;

    @SerializedName("timestamp")
    protected OffsetDateTime timestamp;

    @SerializedName("save_cause")
    protected String saveCause;

    @SerializedName("server_name")
    protected String serverName;

    @SerializedName("minecraft_version")
    protected String minecraftVersion;

    @SerializedName("platform_type")
    protected String platformType;

    @SerializedName("format_version")
    protected int formatVersion;

    @SerializedName("data")
    protected Map<String, String> data;

    // If the snapshot is invalid, this will be set to the validation exception
    @Nullable
    @Expose(serialize = false, deserialize = false)
    transient DataException.Reason exception = null;

    private DataSnapshot(@NotNull UUID id, boolean pinned, @NotNull OffsetDateTime timestamp,
                         @NotNull String saveCause, @NotNull String serverName, @NotNull Map<String, String> data,
                         @NotNull Version minecraftVersion, @NotNull String platformType, int formatVersion) {
        this.id = id;
        this.pinned = pinned;
        this.timestamp = timestamp;
        this.saveCause = saveCause;
        this.serverName = serverName;
        this.data = data;
        this.minecraftVersion = minecraftVersion.toStringWithoutMetadata();
        this.platformType = platformType;
        this.formatVersion = formatVersion;
    }

    @NotNull
    @ApiStatus.Internal
    public static DataSnapshot.Builder builder(@NotNull HuskSync plugin) {
        return new Builder(plugin);
    }

    // Deserialize a DataSnapshot downloaded from the database (with an ID & Timestamp from the database)
    @NotNull
    @ApiStatus.Internal
    public static DataSnapshot.Packed deserialize(@NotNull HuskSync plugin, byte[] data, @Nullable UUID id,
                                                  @Nullable OffsetDateTime timestamp) {
        final DataSnapshot.Packed snapshot = plugin.getDataAdapter().fromBytes(data, DataSnapshot.Packed.class);
        if (snapshot.getMinecraftVersion().compareTo(plugin.getMinecraftVersion()) > 0) {
            return snapshot.invalid(DataException.Reason.INVALID_MINECRAFT_VERSION);
        }
        if (snapshot.getFormatVersion() > CURRENT_FORMAT_VERSION) {
            return snapshot.invalid(DataException.Reason.INVALID_FORMAT_VERSION);
        }
        if (snapshot.getFormatVersion() < 4) {
            if (plugin.getLegacyConverter().isPresent()) {
                return plugin.getLegacyConverter().get().convert(
                        data, Objects.requireNonNull(id, "Attempted legacy conversion with null UUID!"),
                        Objects.requireNonNull(timestamp, "Attempted legacy conversion with null timestamp!")
                );
            }
            return snapshot.invalid(DataException.Reason.NO_LEGACY_CONVERTER);
        }
        if (!snapshot.getPlatformType().equalsIgnoreCase(plugin.getPlatformType())) {
            return snapshot.invalid(DataException.Reason.INVALID_PLATFORM_TYPE);
        }
        return snapshot;
    }

    // Deserialize a DataSnapshot from a network message payload (without an ID)
    @NotNull
    @ApiStatus.Internal
    public static DataSnapshot.Packed deserialize(@NotNull HuskSync plugin, byte[] data) throws IllegalStateException {
        return deserialize(plugin, data, null, null);
    }

    /**
     * Return the ID of the snapshot
     *
     * @return The snapshot ID
     * @since 3.0
     */
    @NotNull
    public UUID getId() {
        return id;
    }

    /**
     * <b>Internal use only</b> Set the ID of the snapshot
     *
     * @param id The snapshot ID
     * @since 3.0
     */
    @ApiStatus.Internal
    public void setId(@NotNull UUID id) {
        this.id = id;
    }

    /**
     * Get the short display ID of the snapshot
     *
     * @return The short display ID
     * @since 3.0
     */
    @NotNull
    public String getShortId() {
        return id.toString().substring(0, 8);
    }

    /**
     * Get whether the snapshot is pinned
     *
     * @return Whether the snapshot is pinned
     * @since 3.0
     */
    public boolean isPinned() {
        return pinned;
    }

    /**
     * Set whether the snapshot is pinned
     *
     * @param pinned Whether the snapshot is pinned
     * @since 3.0
     */
    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    /**
     * Get why the snapshot was created
     *
     * @return The {@link SaveCause data save cause} of the snapshot
     * @since 3.0
     */
    @NotNull
    public SaveCause getSaveCause() {
        return SaveCause.of(saveCause);
    }

    /**
     * Get the server the snapshot was created on.
     * <p>
     * Note that snapshots generated before v3.1 will return {@code "N/A"}
     *
     * @return The server name
     * @since 3.1
     */
    @NotNull
    public String getServerName() {
        return Optional.ofNullable(serverName).orElse("N/A");
    }

    /**
     * Set why the snapshot was created
     *
     * @param saveCause The {@link SaveCause data save cause} of the snapshot
     * @since 3.0
     */
    public void setSaveCause(@NotNull SaveCause saveCause) {
        this.saveCause = saveCause.name();
    }

    /**
     * Get when the snapshot was created
     *
     * @return The {@link OffsetDateTime timestamp} of the snapshot
     * @since 3.0
     */
    @NotNull
    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Get the Minecraft version of the server when the Snapshot was created
     *
     * @return The Minecraft version of the server when the Snapshot was created
     * @since 3.0
     */
    @NotNull
    public Version getMinecraftVersion() {
        return Version.fromString(minecraftVersion);
    }

    /**
     * Get the platform type of the server when the Snapshot was created
     *
     * @return The platform type of the server when the Snapshot was created (e.g. {@code "bukkit"})
     * @since 3.0
     */
    @NotNull
    public String getPlatformType() {
        return platformType;
    }

    /**
     * Get the format version of the snapshot (indicating the version of HuskSync that created it)
     *
     * @return The format version of the snapshot
     * @since 3.0
     */
    public int getFormatVersion() {
        return formatVersion;
    }

    /**
     * A packed {@link DataSnapshot} that has not been deserialized.
     *
     * @since 3.0
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Packed extends DataSnapshot implements Adaptable {

        protected Packed(@NotNull UUID id, boolean pinned, @NotNull OffsetDateTime timestamp,
                         @NotNull String saveCause, @NotNull String serverName, @NotNull Map<String, String> data,
                         @NotNull Version minecraftVersion, @NotNull String platformType, int formatVersion) {
            super(id, pinned, timestamp, saveCause, serverName, data, minecraftVersion, platformType, formatVersion);
        }

        @NotNull
        @ApiStatus.Internal
        DataSnapshot.Packed invalid(@NotNull DataException.Reason reason) {
            this.exception = reason;
            return this;
        }

        public boolean isInvalid() {
            return exception != null;
        }

        @NotNull
        public String getInvalidReason(@NotNull HuskSync plugin) {
            if (exception == null) {
                throw new IllegalStateException("Attempted to get an invalid reason for a valid snapshot!");
            }
            return exception.getMessage(plugin, this);
        }

        @ApiStatus.Internal
        void validate(@NotNull HuskSync plugin) throws DataException {
            if (exception != null) {
                throw exception.toException(this, plugin);
            }
        }

        @ApiStatus.Internal
        public void edit(@NotNull HuskSync plugin, @NotNull Consumer<Unpacked> editor) {
            final Unpacked data = unpack(plugin);
            editor.accept(data);
            this.pinned = data.isPinned();
            this.saveCause = data.getSaveCause().name();
            this.data = data.serializeData(plugin);
        }

        /**
         * Create a copy of this snapshot at the current system timestamp with a new ID
         *
         * @return The copied snapshot (with a new ID, with a timestamp of the current system time)
         */
        @NotNull
        public Packed copy() {
            return new Packed(
                    UUID.randomUUID(), pinned, OffsetDateTime.now(), saveCause, serverName,
                    data, getMinecraftVersion(), platformType, formatVersion
            );
        }

        @ApiStatus.Internal
        public byte[] asBytes(@NotNull HuskSync plugin) throws DataAdapter.AdaptionException {
            return plugin.getDataAdapter().toBytes(this);
        }

        @NotNull
        @ApiStatus.Internal
        public String asJson(@NotNull HuskSync plugin) throws DataAdapter.AdaptionException {
            return plugin.getDataAdapter().toJson(this);
        }

        @ApiStatus.Internal
        public int getFileSize(@NotNull HuskSync plugin) {
            return asBytes(plugin).length;
        }

        @NotNull
        public DataSnapshot.Unpacked unpack(@NotNull HuskSync plugin) throws DataException {
            this.validate(plugin);
            return new Unpacked(
                    id, pinned, timestamp, saveCause, serverName, data,
                    getMinecraftVersion(), platformType, formatVersion, plugin
            );
        }

    }

    /**
     * An unpacked {@link DataSnapshot}.
     *
     * @since 3.0
     */
    public static class Unpacked extends DataSnapshot implements DataHolder {

        @Expose(serialize = false, deserialize = false)
        private final TreeMap<Identifier, Data> deserialized;

        private Unpacked(@NotNull UUID id, boolean pinned, @NotNull OffsetDateTime timestamp,
                         @NotNull String saveCause, @NotNull String serverName, @NotNull Map<String, String> data,
                         @NotNull Version minecraftVersion, @NotNull String platformType, int formatVersion,
                         @NotNull HuskSync plugin) {
            super(id, pinned, timestamp, saveCause, serverName, data, minecraftVersion, platformType, formatVersion);
            this.deserialized = deserializeData(plugin);
        }

        private Unpacked(@NotNull UUID id, boolean pinned, @NotNull OffsetDateTime timestamp,
                         @NotNull String saveCause, @NotNull String serverName, @NotNull TreeMap<Identifier, Data> data,
                         @NotNull Version minecraftVersion, @NotNull String platformType, int formatVersion) {
            super(id, pinned, timestamp, saveCause, serverName, Map.of(), minecraftVersion, platformType, formatVersion);
            this.deserialized = data;
        }

        @NotNull
        @ApiStatus.Internal
        private TreeMap<Identifier, Data> deserializeData(@NotNull HuskSync plugin) {
            return data.entrySet().stream()
                    .filter(e -> plugin.getIdentifier(e.getKey()).isPresent())
                    .map(entry -> Map.entry(plugin.getIdentifier(entry.getKey()).orElseThrow(), entry.getValue()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> plugin.deserializeData(entry.getKey(), entry.getValue(), getMinecraftVersion()),
                            (a, b) -> b, () -> Maps.newTreeMap(SerializerRegistry.DEPENDENCY_ORDER_COMPARATOR)
                    ));
        }

        @NotNull
        @ApiStatus.Internal
        private Map<String, String> serializeData(@NotNull HuskSync plugin) {
            return deserialized.entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> entry.getKey().toString(),
                            entry -> plugin.serializeData(entry.getKey(), entry.getValue())
                    ));
        }

        /**
         * Get the data the snapshot is holding
         *
         * @return The data map
         * @since 3.0
         */
        @NotNull
        public Map<Identifier, Data> getData() {
            return deserialized;
        }

        /**
         * Pack the {@link DataSnapshot} into a {@link DataSnapshot.Packed packed} snapshot
         *
         * @param plugin The HuskSync plugin instance
         * @return The packed snapshot
         * @since 3.0
         */
        @NotNull
        @ApiStatus.Internal
        public DataSnapshot.Packed pack(@NotNull HuskSync plugin) {
            return new DataSnapshot.Packed(
                    id, pinned, timestamp, saveCause, serverName, serializeData(plugin),
                    getMinecraftVersion(), platformType, formatVersion
            );
        }

    }

    /**
     * A builder for {@link DataSnapshot}s.
     *
     * @since 3.0
     */
    @SuppressWarnings("unused")
    public static class Builder {

        private final HuskSync plugin;
        private UUID id;
        private SaveCause saveCause;
        private String serverName;
        private boolean pinned;
        private OffsetDateTime timestamp;
        private final TreeMap<Identifier, Data> data;

        private Builder(@NotNull HuskSync plugin) {
            this.plugin = plugin;
            this.pinned = false;
            this.data = Maps.newTreeMap(SerializerRegistry.DEPENDENCY_ORDER_COMPARATOR);
            this.timestamp = OffsetDateTime.now();
            this.id = UUID.randomUUID();
            this.serverName = plugin.getServerName();
        }

        /**
         * Set the {@link UUID unique ID} of the snapshot
         *
         * @param id The {@link UUID} of the snapshot
         * @return The builder
         */
        @NotNull
        public Builder id(@NotNull UUID id) {
            this.id = id;
            return this;
        }

        /**
         * Set the cause of the data save
         *
         * @param saveCause The cause of the data save
         * @return The builder
         * @apiNote If the {@link SaveCause data save cause} specified is configured to auto-pin, then the value of
         * {@link #pinned(boolean)} will be ignored
         * @since 3.0
         */
        @NotNull
        public Builder saveCause(@NotNull SaveCause saveCause) {
            this.saveCause = saveCause;
            return this;
        }

        /**
         * Set the name of the server where this snapshot was created
         *
         * @param serverName The server name
         * @return The builder
         * @since 3.1
         */
        @NotNull
        public Builder serverName(@NotNull String serverName) {
            this.serverName = serverName;
            return this;
        }

        /**
         * Set whether the data should be pinned
         *
         * @param pinned Whether the data should be pinned
         * @return The builder
         * @apiNote If the {@link SaveCause data save cause} specified is configured to auto-pin, this will be ignored
         * @since 3.0
         */
        @NotNull
        public Builder pinned(boolean pinned) {
            this.pinned = pinned;
            return this;
        }

        /**
         * Set the timestamp of the snapshot.
         * By default, this is the current server time.
         * The timestamp passed to this method cannot be in the future.
         * <p>
         * Note that this will affect the rotation of data snapshots in the database if unpinned,
         * as well as the order snapshots appear in the list.
         *
         * @param timestamp The timestamp
         * @return The builder
         * @throws IllegalArgumentException if the timestamp is in the future
         * @since 3.0
         */
        @NotNull
        public Builder timestamp(@NotNull OffsetDateTime timestamp) {
            if (timestamp.isAfter(OffsetDateTime.now())) {
                throw new IllegalArgumentException("Data snapshots cannot have a timestamp set in the future! "
                                                   + "Make sure your database server time matches the server time.\n"
                                                   + "Current game server timestamp: " + OffsetDateTime.now() + " / "
                                                   + "Snapshot timestamp: " + timestamp);
            }
            this.timestamp = timestamp;
            return this;
        }

        /**
         * Set the data for a given identifier
         *
         * @param identifier The identifier
         * @param data       The data
         * @return The builder
         * @since 3.0
         */
        @NotNull
        public Builder data(@NotNull Identifier identifier, @NotNull Data data) {
            this.data.put(identifier, data);
            return this;
        }

        /**
         * Set a map of data to the snapshot
         *
         * @param data The data
         * @return The builder
         * @since 3.0
         */
        @NotNull
        public Builder data(@NotNull Map<Identifier, Data> data) {
            this.data.putAll(data);
            return this;
        }

        /**
         * Set the inventory contents of the snapshot
         * <p>
         * Equivalent to {@code data(Identifier.INVENTORY, inventory)}
         * </p>
         *
         * @param inventory The inventory contents
         * @return The builder
         * @since 3.0
         */
        @NotNull
        public Builder inventory(@NotNull Data.Items.Inventory inventory) {
            return data(Identifier.INVENTORY, inventory);
        }

        /**
         * Set the Ender Chest contents of the snapshot
         * <p>
         * Equivalent to {@code data(Identifier.ENDER_CHEST, inventory)}
         * </p>
         *
         * @param enderChest The Ender Chest contents
         * @return The builder
         * @since 3.0
         */
        @NotNull
        public Builder enderChest(@NotNull Data.Items.EnderChest enderChest) {
            return data(Identifier.ENDER_CHEST, enderChest);
        }

        /**
         * Set the potion effects of the snapshot
         * <p>
         * Equivalent to {@code data(Identifier.POTION_EFFECTS, potionEffects)}
         * </p>
         *
         * @param potionEffects The potion effects
         * @return The builder
         * @since 3.0
         */
        @NotNull
        public Builder potionEffects(@NotNull Data.PotionEffects potionEffects) {
            return data(Identifier.POTION_EFFECTS, potionEffects);
        }

        /**
         * Set the advancements of the snapshot
         * <p>
         * Equivalent to {@code data(Identifier.ADVANCEMENTS, advancements)}
         * </p>
         *
         * @param advancements The advancements
         * @return The builder
         * @since 3.0
         */
        @NotNull
        public Builder advancements(@NotNull Data.Advancements advancements) {
            return data(Identifier.ADVANCEMENTS, advancements);
        }

        /**
         * Set the location of the snapshot
         * <p>
         * Equivalent to {@code data(Identifier.LOCATION, location)}
         * </p>
         *
         * @param location The location
         * @return The builder
         * @since 3.0
         */
        @NotNull
        public Builder location(@NotNull Data.Location location) {
            return data(Identifier.LOCATION, location);
        }

        /**
         * Set the statistics of the snapshot
         * <p>
         * Equivalent to {@code data(Identifier.STATISTICS, statistics)}
         * </p>
         *
         * @param statistics The statistics
         * @return The builder
         * @since 3.0
         */
        @NotNull
        public Builder statistics(@NotNull Data.Statistics statistics) {
            return data(Identifier.STATISTICS, statistics);
        }

        /**
         * Set the health of the snapshot
         * <p>
         * Equivalent to {@code data(Identifier.HEALTH, health)}
         * </p>
         *
         * @param health The health
         * @return The builder
         * @since 3.0
         */
        @NotNull
        public Builder health(@NotNull Data.Health health) {
            return data(Identifier.HEALTH, health);
        }

        /**
         * Set the hunger of the snapshot
         * <p>
         * Equivalent to {@code data(Identifier.HUNGER, hunger)}
         * </p>
         *
         * @param hunger The hunger
         * @return The builder
         * @since 3.0
         */
        @NotNull
        public Builder hunger(@NotNull Data.Hunger hunger) {
            return data(Identifier.HUNGER, hunger);
        }

        /**
         * Set the attributes of the snapshot
         * <p>
         * Equivalent to {@code data(Identifier.ATTRIBUTES, attributes)}
         * </p>
         *
         * @param attributes The user's attributes
         * @return The builder
         * @since 3.5
         */
        @NotNull
        public Builder attributes(@NotNull Data.Attributes attributes) {
            return data(Identifier.ATTRIBUTES, attributes);
        }

        /**
         * Set the experience of the snapshot
         * <p>
         * Equivalent to {@code data(Identifier.EXPERIENCE, experience)}
         * </p>
         *
         * @param experience The experience
         * @return The builder
         * @since 3.0
         */
        @NotNull
        public Builder experience(@NotNull Data.Experience experience) {
            return data(Identifier.EXPERIENCE, experience);
        }

        /**
         * Set the game mode of the snapshot
         * <p>
         * Equivalent to {@code data(Identifier.GAME_MODE, gameMode)}
         * </p>
         *
         * @param gameMode The game mode
         * @return The builder
         * @since 3.0
         */
        @NotNull
        public Builder gameMode(@NotNull Data.GameMode gameMode) {
            return data(Identifier.GAME_MODE, gameMode);
        }

        /**
         * Set the flight status of the snapshot
         * <p>
         * Equivalent to {@code data(Identifier.FLIGHT_STATUS, flightStatus)}
         * </p>
         *
         * @param flightStatus The flight status
         * @return The builder
         * @since 3.5
         */
        @NotNull
        public Builder flightStatus(@NotNull Data.FlightStatus flightStatus) {
            return data(Identifier.FLIGHT_STATUS, flightStatus);
        }

        /**
         * Set the persistent data container of the snapshot
         * <p>
         * Equivalent to {@code data(Identifier.PERSISTENT_DATA, persistentData)}
         * </p>
         *
         * @param persistentData The persistent data container data
         * @return The builder
         * @since 3.0
         */
        @NotNull
        public Builder persistentData(@NotNull Data.PersistentData persistentData) {
            return data(Identifier.PERSISTENT_DATA, persistentData);
        }

        /**
         * Build the {@link DataSnapshot}
         *
         * @return The {@link DataSnapshot.Unpacked snapshot}
         * @throws IllegalStateException If no save cause is specified
         * @since 3.0
         */
        @NotNull
        public DataSnapshot.Unpacked build() throws IllegalStateException {
            if (saveCause == null) {
                throw new IllegalStateException("Cannot build DataSnapshot without a save cause");
            }
            return new Unpacked(
                    id,
                    pinned || plugin.getSettings().getSynchronization().doAutoPin(saveCause),
                    timestamp,
                    saveCause.name(),
                    serverName,
                    data,
                    plugin.getMinecraftVersion(),
                    plugin.getPlatformType(),
                    DataSnapshot.CURRENT_FORMAT_VERSION
            );
        }

        /**
         * Build and pack the {@link DataSnapshot}
         *
         * @return The {@link DataSnapshot.Packed snapshot}
         * @throws IllegalStateException If no save cause is specified
         * @since 3.0
         */
        @NotNull
        public DataSnapshot.Packed buildAndPack() throws IllegalStateException {
            return build().pack(plugin);
        }

    }

    public interface Cause {

        @NotNull
        String name();

        /**
         * Returns the capitalized display name of the cause.
         *
         * @return the cause display name
         */
        @NotNull
        default String getDisplayName() {
            return WordUtils.capitalizeFully(name().replaceAll("_", " "));
        }

    }

    /**
     * A string wrapper, for identifying the cause of a player data save.
     * </p>
     * Cause names have a max length of 32 characters.
     */
    @Accessors(fluent = true)
    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class SaveCause implements Cause {

        /**
         * Indicates data saved when a player disconnected from the server (either to change servers, or to log off)
         *
         * @since 2.0
         */
        public static final SaveCause DISCONNECT = of("DISCONNECT");

        /**
         * Indicates data saved when the world saved
         *
         * @since 2.0
         */
        public static final SaveCause WORLD_SAVE = of("WORLD_SAVE");

        /**
         * Indicates data saved when the user died
         *
         * @since 2.1
         */
        public static final SaveCause DEATH = of("DEATH");

        /**
         * Indicates data saved when the server shut down
         *
         * @since 2.0
         */
        public static final SaveCause SERVER_SHUTDOWN = of("SERVER_SHUTDOWN", false);

        /**
         * Indicates data was saved by editing inventory contents via the {@code /inventory} command
         *
         * @since 2.0
         */
        public static final SaveCause INVENTORY_COMMAND = of("INVENTORY_COMMAND");

        /**
         * Indicates data was saved by editing Ender Chest contents via the {@code /enderchest} command
         *
         * @since 2.0
         */
        public static final SaveCause ENDERCHEST_COMMAND = of("ENDERCHEST_COMMAND");

        /**
         * Indicates data was saved by restoring it from a previous version
         *
         * @since 2.0
         */
        public static final SaveCause BACKUP_RESTORE = of("BACKUP_RESTORE");

        /**
         * Indicates data was saved by an API call
         *
         * @since 2.0
         */
        public static final SaveCause API = of("API");

        /**
         * Indicates data was saved from being imported from MySQLPlayerDataBridge
         *
         * @since 2.0
         */
        public static final SaveCause MPDB_MIGRATION = of("MPDB_MIGRATION", false);

        /**
         * Indicates data was saved from being imported from a legacy version (v1.x -> v2.x)
         *
         * @since 2.0
         */
        public static final SaveCause LEGACY_MIGRATION = of("LEGACY_MIGRATION", false);

        /**
         * Indicates data was saved from being imported from a legacy version (v2.x -> v3.x)
         *
         * @since 3.0
         */
        public static final SaveCause CONVERTED_FROM_V2 = of("CONVERTED_FROM_V2", false);

        @NotNull
        private final String name;

        private final boolean fireDataSaveEvent;

        private static Map<String, SaveCause> registry;

        /**
         * Get or create a {@link SaveCause} from a name
         *
         * @param name the name to be displayed
         * @return the cause
         */
        @NotNull
        public static SaveCause of(@NotNull String name) {
            return of(name,true);
        }

        /**
         * Get or create a {@link SaveCause} from a name and whether it should fire a save event
         *
         * @param name           the name to be displayed
         * @param firesSaveEvent whether the cause should fire a save event
         * @return the cause
         */
        @NotNull
        public static SaveCause of(@NotNull String name, boolean firesSaveEvent) {
            name = name.length() > 32 ? name.substring(0, 31) : name;

            if (registry == null) registry = new HashMap<>();
            if (registry.containsKey(name)) return registry.get(name);

            SaveCause cause = new SaveCause(name, firesSaveEvent);
            registry.put(cause.name(), cause);
            return cause;
        }

        @NotNull
        public String getLocale(@NotNull HuskSync plugin) {
            return plugin.getLocales()
                    .getRawLocale("save_cause_%s".formatted(name().toLowerCase(Locale.ENGLISH)))
                    .orElse(getDisplayName());
        }

        @NotNull
        @ApiStatus.Obsolete
        public static SaveCause[] values() {
            if (registry == null) registry = new HashMap<>();
            return registry.values().toArray(new SaveCause[0]);
        }
    }

    /**
     * A string wrapper, for identifying the cause of a player data update.
     * </p>
     * Cause names have a max length of 32 characters.
     */
    @Accessors(fluent = true)
    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class UpdateCause implements Cause {

        /**
         * Indicates the data was updated by a synchronization process
         *
         * @since 3.0
         */
        public static final UpdateCause SYNCHRONIZED = of("SYNCHRONIZED",
                "synchronization_complete", "synchronization_failed"
        );

        /**
         * Indicates the data was updated by a user joining the server
         *
         * @since 3.0
         */
        public static final UpdateCause NEW_USER = of("NEW_USER",
                "user_registration_complete", null
        );

        /**
         * Indicates the data was updated by a data update process (management command, API, etc.)
         *
         * @since 3.0
         */
        public static final UpdateCause UPDATED = of("UPDATED",
                "data_update_complete", "data_update_failed"
        );

        /**
         * Indicates data was saved by an API call
         *
         * @since 3.3.3
         */
        public static final UpdateCause API = of("API");

        @NotNull
        private final String name;
        @Nullable
        private String completedLocale;
        @Nullable
        private String failureLocale;

        /**
         * Get or create a {@link UpdateCause} from a name and completed/failure locales
         *
         * @param name            the name to be displayed
         * @param completedLocale the locale to be displayed on successful update,
         *                        or {@code null} if none is to be shown
         * @param failureLocale   the locale to be displayed on a failed update,
         *                        or {@code null} if none is to be shown
         * @return the cause
         */
        public static UpdateCause of(@NotNull String name, @Nullable String completedLocale,
                                     @Nullable String failureLocale) {
            return new UpdateCause(
                    name.length() > 32 ? name.substring(0, 31) : name,
                    completedLocale, failureLocale
            );
        }

        /**
         * Get or create a {@link UpdateCause} from a name
         *
         * @param name the name to be displayed
         * @return the cause
         */
        @NotNull
        public static UpdateCause of(@NotNull String name) {
            return of(name, null, null);
        }

        /**
         * Get the message to be displayed when a user's data is successfully updated.
         *
         * @param plugin plugin instance
         * @return the message
         */
        public Optional<MineDown> getCompletedLocale(@NotNull HuskSync plugin) {
            if (completedLocale() != null) {
                return Optional.of(plugin.getLocales().getLocale(completedLocale())
                        .orElse(plugin.getLocales().format(getDisplayName())));
            }
            return Optional.empty();
        }

        /**
         * Get the message to be displayed when a user's data fails to update.
         *
         * @param plugin plugin instance
         * @return the message
         */
        public Optional<MineDown> getFailedLocale(@NotNull HuskSync plugin) {
            if (failureLocale() != null) {
                return Optional.of(plugin.getLocales().getLocale(failureLocale())
                        .orElse(plugin.getLocales().format(failureLocale())));
            }
            return Optional.empty();
        }

        @NotNull
        public static UpdateCause[] values() {
            return new UpdateCause[]{
                    SYNCHRONIZED, NEW_USER, UPDATED
            };
        }
    }
}
