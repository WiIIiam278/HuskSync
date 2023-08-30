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
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A snapshot of data, held by a {@link DataOwner}
 */
public class DataSnapshot {

    // Current version of the snapshot data format
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
    protected Version minecraftVersion;

    @SerializedName("format_version")
    protected int formatVersion;

    @SerializedName("data")
    protected Map<DataContainer.Type, byte[]> data;

    private DataSnapshot(@NotNull UUID id, boolean pinned, @NotNull OffsetDateTime timestamp,
                         @NotNull SaveCause saveCause, @NotNull Map<DataContainer.Type, byte[]> data,
                         @NotNull Version minecraftVersion, int formatVersion) {
        this.id = id;
        this.pinned = pinned;
        this.timestamp = timestamp;
        this.saveCause = saveCause;
        this.data = data;
        this.minecraftVersion = minecraftVersion;
        this.formatVersion = formatVersion;
    }

    @SuppressWarnings("unused")
    private DataSnapshot() {
    }

    @NotNull
    public static DataSnapshot.Packed of(@NotNull UUID id, boolean pinned, @NotNull OffsetDateTime timestamp,
                                         @NotNull SaveCause saveCause, @NotNull Map<DataContainer.Type, byte[]> data,
                                         @NotNull Version minecraftVersion, int formatVersion) {
        return new DataSnapshot.Packed(id, pinned, timestamp, saveCause, data, minecraftVersion, formatVersion);
    }

    @NotNull
    protected static DataSnapshot.Packed create(@NotNull HuskSync plugin, @NotNull DataOwner owner,
                                                @NotNull SaveCause saveCause) {
        return new DataSnapshot.Unpacked(
                UUID.randomUUID(), false, OffsetDateTime.now(),
                saveCause, owner.getData(),
                plugin.getMinecraftVersion(), CURRENT_FORMAT_VERSION
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
        //todo convert version 3 data to version 4
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
    public Set<DataContainer.Type> getDataTypes() {
        return data.keySet();
    }

    @NotNull
    public Version getMinecraftVersion() {
        return minecraftVersion;
    }

    public int getFormatVersion() {
        return formatVersion;
    }

    /**
     * A packed {@link DataSnapshot} that has not been deserialized.
     */
    public static class Packed extends DataSnapshot implements Adaptable {

        protected Packed(@NotNull UUID id, boolean pinned, @NotNull OffsetDateTime timestamp,
                         @NotNull SaveCause saveCause, @NotNull Map<DataContainer.Type, byte[]> data,
                         @NotNull Version minecraftVersion, int formatVersion) {
            super(id, pinned, timestamp, saveCause, data, minecraftVersion, formatVersion);
        }

        @SuppressWarnings("unused")
        private Packed() {
        }

        public void edit(@NotNull HuskSync plugin, @NotNull Consumer<Unpacked> editor) {
            final Unpacked unpacked = unpack(plugin);
            editor.accept(unpacked);
            this.data = unpacked.serializeData(plugin);
        }

        @NotNull
        public Packed copy() {
            return new Packed(
                    UUID.randomUUID(), pinned, OffsetDateTime.now(), saveCause, data, minecraftVersion, formatVersion
            );
        }

        public byte[] serialize(@NotNull HuskSync plugin) throws DataAdapter.AdaptionException {
            return plugin.getDataAdapter().toBytes(this);
        }

        @NotNull
        public String asJson(@NotNull HuskSync plugin) throws DataAdapter.AdaptionException {
            return plugin.getDataAdapter().toJson(this);
        }

        @NotNull
        public DataSnapshot.Unpacked unpack(@NotNull HuskSync plugin) {
            return new Unpacked(id, pinned, timestamp, saveCause, data, minecraftVersion, formatVersion, plugin);
        }

    }

    /**
     * An unpacked {@link DataSnapshot}.
     */
    public static class Unpacked extends DataSnapshot implements MutableDataStore {

        @Expose(serialize = false, deserialize = false)
        private final Map<DataContainer.Type, DataContainer> deserialized;

        private Unpacked(@NotNull UUID id, boolean pinned, @NotNull OffsetDateTime timestamp,
                         @NotNull SaveCause saveCause, @NotNull Map<DataContainer.Type, byte[]> data,
                         @NotNull Version minecraftVersion, int formatVersion, @NotNull HuskSync plugin) {
            super(id, pinned, timestamp, saveCause, data, minecraftVersion, formatVersion);
            this.deserialized = deserializeData(plugin);
        }

        private Unpacked(@NotNull UUID id, boolean pinned, @NotNull OffsetDateTime timestamp,
                         @NotNull SaveCause saveCause, @NotNull Map<DataContainer.Type, DataContainer> data,
                         @NotNull Version minecraftVersion, int formatVersion) {
            super(id, pinned, timestamp, saveCause, Map.of(), minecraftVersion, formatVersion);
            this.deserialized = data;
        }

        @NotNull
        private Map<DataContainer.Type, DataContainer> deserializeData(@NotNull HuskSync plugin) {
            return data.entrySet().stream()
                    .map((entry) -> Map.entry(entry.getKey(), Objects.requireNonNull(
                            plugin.getSerializers().get(entry.getKey()),
                            String.format("No deserializer found for %s", entry.getKey().name())
                    ).deserialize(entry.getValue())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        @NotNull
        private Map<DataContainer.Type, byte[]> serializeData(@NotNull HuskSync plugin) {
            return deserialized.entrySet().stream()
                    .map((entry) -> Map.entry(entry.getKey(), Objects.requireNonNull(
                            plugin.getSerializers().get(entry.getKey()),
                            String.format("No serializer found for %s", entry.getKey().name())
                    ).serialize(entry.getValue())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        @NotNull
        public Map<DataContainer.Type, DataContainer> getData() {
            return deserialized;
        }

        @NotNull
        public DataSnapshot.Packed pack(@NotNull HuskSync plugin) {
            return new DataSnapshot.Packed(
                    id, pinned, timestamp, saveCause, serializeData(plugin), minecraftVersion, formatVersion
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
         * Indicates data was saved from being imported from a legacy version (v1.x)
         *
         * @since 2.0
         */
        LEGACY_MIGRATION;

        @NotNull
        public String getDisplayName() {
            return Locales.truncate(name().toLowerCase(Locale.ENGLISH), 10);
        }

    }
}
