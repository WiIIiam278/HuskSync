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

package net.william278.husksync.database;

import lombok.Getter;
import net.william278.husksync.HuskSync;
import net.william278.husksync.config.Settings;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.user.User;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * An abstract representation of the plugin database, storing player data.
 * <p>
 * Implemented by different database platforms - MySQL, SQLite, etc. - as configured by the administrator.
 */
public abstract class Database {

    protected final HuskSync plugin;

    protected Database(@NotNull HuskSync plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads SQL table creation schema statements from a resource file as a string array
     *
     * @param schemaFileName database script resource file to load from
     * @return Array of string-formatted table creation schema statements
     * @throws IOException if the resource could not be read
     */
    @SuppressWarnings("SameParameterValue")
    @NotNull
    protected final String[] getSchemaStatements(@NotNull String schemaFileName) throws IOException {
        return formatStatementTables(new String(Objects.requireNonNull(plugin.getResource(schemaFileName))
                .readAllBytes(), StandardCharsets.UTF_8)).split(";");
    }

    /**
     * Format all table name placeholder strings in an SQL statement
     *
     * @param sql the SQL statement with unformatted table name placeholders
     * @return the formatted statement, with table placeholders replaced with the correct names
     */
    @NotNull
    protected final String formatStatementTables(@NotNull String sql) {
        final Settings.DatabaseSettings settings = plugin.getSettings().getDatabase();
        return sql.replaceAll("%users_table%", settings.getTableName(TableName.USERS))
                .replaceAll("%user_data_table%", settings.getTableName(TableName.USER_DATA));
    }

    /**
     * Initialize the database and ensure tables are present; create tables if they do not exist.
     *
     * @throws IllegalStateException if the database could not be initialized
     */
    @Blocking
    public abstract void initialize() throws IllegalStateException;

    /**
     * Ensure a {@link User} has an entry in the database and that their username is up-to-date
     *
     * @param user The {@link User} to ensure
     */
    @Blocking
    public abstract void ensureUser(@NotNull User user);

    /**
     * Get a player by their Minecraft account {@link UUID}
     *
     * @param uuid Minecraft account {@link UUID} of the {@link User} to get
     * @return An optional with the {@link User} present if they exist
     */
    @Blocking
    public abstract Optional<User> getUser(@NotNull UUID uuid);

    /**
     * Get a user by their username (<i>case-insensitive</i>)
     *
     * @param username Username of the {@link User} to get (<i>case-insensitive</i>)
     * @return An optional with the {@link User} present if they exist
     */
    @Blocking
    public abstract Optional<User> getUserByName(@NotNull String username);

    /**
     * Get all users
     *
     * @return A list of all users
     */
    @NotNull
    @Blocking
    public abstract List<User> getAllUsers();

    /**
     * Get the latest data snapshot for a user.
     *
     * @param user The user to get data for
     * @return an optional containing the {@link DataSnapshot}, if it exists, or an empty optional if it does not
     */
    @Blocking
    public abstract Optional<DataSnapshot.Packed> getLatestSnapshot(@NotNull User user);

    /**
     * Get all {@link DataSnapshot} entries for a user from the database.
     *
     * @param user The user to get data for
     * @return The list of a user's {@link DataSnapshot} entries
     */
    @Blocking
    @NotNull
    public abstract List<DataSnapshot.Packed> getAllSnapshots(@NotNull User user);

    /**
     * Gets a specific {@link DataSnapshot} entry for a user from the database, by its UUID.
     *
     * @param user        The user to get data for
     * @param versionUuid The UUID of the {@link DataSnapshot} entry to get
     * @return An optional containing the {@link DataSnapshot}, if it exists
     */
    @Blocking
    public abstract Optional<DataSnapshot.Packed> getSnapshot(@NotNull User user, @NotNull UUID versionUuid);

    /**
     * <b>(Internal)</b> Prune user data for a given user to the maximum value as configured.
     *
     * @param user The user to prune data for
     * @implNote Data snapshots marked as {@code pinned} are exempt from rotation
     */
    @Blocking
    protected abstract void rotateSnapshots(@NotNull User user);

    /**
     * Deletes a specific {@link DataSnapshot} entry for a user from the database, by its UUID.
     *
     * @param user        The user to get data for
     * @param versionUuid The UUID of the {@link DataSnapshot} entry to delete
     */
    @Blocking
    public abstract boolean deleteSnapshot(@NotNull User user, @NotNull UUID versionUuid);


    /**
     * Save user data to the database, doing the following (in order):
     * <ol>
     *     <li>Delete their most recent snapshot, if it was created before the backup frequency time</li>
     *     <li>Create the snapshot</li>
     *     <li>Rotate snapshot backups</li>
     * </ol>
     * This is an expensive blocking method and should be run off the main thread.
     *
     * @param user     The user to add data for
     * @param snapshot The {@link DataSnapshot} to set.
     * @apiNote Prefer {@link net.william278.husksync.sync.DataSyncer#saveData(User, DataSnapshot.Packed, BiConsumer)}.
     * </p>This method will not fire the {@link net.william278.husksync.event.DataSaveEvent}
     */
    @Blocking
    public void addSnapshot(@NotNull User user, @NotNull DataSnapshot.Packed snapshot) {
        final int backupFrequency = plugin.getSettings().getSynchronization().getSnapshotBackupFrequency();
        if (!snapshot.isPinned() && backupFrequency > 0) {
            this.rotateLatestSnapshot(user, snapshot.getTimestamp().minusHours(backupFrequency));
        }
        this.createSnapshot(user, snapshot);
        this.rotateSnapshots(user);
    }

    /**
     * Deletes the most recent data snapshot by the given {@link User user}
     * The snapshot must have been created after {@link OffsetDateTime time} and NOT be pinned
     * Facilities the backup frequency feature, reducing redundant snapshots from being saved longer than needed
     *
     * @param user   The user to delete a snapshot for
     * @param within The time to delete a snapshot after
     */
    @Blocking
    protected abstract void rotateLatestSnapshot(@NotNull User user, @NotNull OffsetDateTime within);

    /**
     * <b>Internal</b> - Create user data in the database
     *
     * @param user The user to add data for
     * @param data The {@link DataSnapshot} to set.
     */
    @Blocking
    protected abstract void createSnapshot(@NotNull User user, @NotNull DataSnapshot.Packed data);

    /**
     * Update a saved {@link DataSnapshot} by given version UUID
     *
     * @param user     The user whose data snapshot
     * @param snapshot The {@link DataSnapshot} to update
     */
    @Blocking
    public abstract void updateSnapshot(@NotNull User user, @NotNull DataSnapshot.Packed snapshot);

    /**
     * Unpin a saved {@link DataSnapshot} by given version UUID, setting it's {@code pinned} state to {@code false}.
     *
     * @param user        The user to unpin the data for
     * @param versionUuid The UUID of the user's {@link DataSnapshot} entry to unpin
     * @see DataSnapshot#isPinned()
     */
    @Blocking
    public final void unpinSnapshot(@NotNull User user, @NotNull UUID versionUuid) {
        this.getSnapshot(user, versionUuid).ifPresent(data -> {
            data.edit(plugin, (snapshot) -> snapshot.setPinned(false));
            this.updateSnapshot(user, data);
        });
    }

    /**
     * Pin a saved {@link DataSnapshot} by given version UUID, setting it's {@code pinned} state to {@code true}.
     *
     * @param user        The user to pin the data for
     * @param versionUuid The UUID of the user's {@link DataSnapshot} entry to pin
     */
    @Blocking
    public final void pinSnapshot(@NotNull User user, @NotNull UUID versionUuid) {
        this.getSnapshot(user, versionUuid).ifPresent(data -> {
            data.edit(plugin, (snapshot) -> snapshot.setPinned(true));
            this.updateSnapshot(user, data);
        });
    }

    /**
     * Wipes <b>all</b> {@link User} entries from the database.
     * <b>This should only be used when preparing tables for a data migration.</b>
     */
    @Blocking
    public abstract void wipeDatabase();

    /**
     * Close the database connection
     */
    public abstract void terminate();

    /**
     * Identifies types of databases
     */
    @Getter
    public enum Type {
        MYSQL("MySQL", "mysql"),
        MARIADB("MariaDB", "mariadb"),
        POSTGRES("PostgreSQL", "postgresql"),
        MONGO("MongoDB", "mongo");

        private final String displayName;
        private final String protocol;

        Type(@NotNull String displayName, @NotNull String protocol) {
            this.displayName = displayName;
            this.protocol = protocol;
        }
    }

    /**
     * Represents the names of tables in the database
     */
    @Getter
    public enum TableName {
        USERS("husksync_users"),
        USER_DATA("husksync_user_data");

        private final String defaultName;

        TableName(@NotNull String defaultName) {
            this.defaultName = defaultName;
        }

        @NotNull
        private Map.Entry<String, String> toEntry() {
            return Map.entry(name().toLowerCase(Locale.ENGLISH), defaultName);
        }

        @SuppressWarnings("unchecked")
        @NotNull
        public static Map<String, String> getDefaults() {
            return Map.ofEntries(Arrays.stream(values())
                    .map(TableName::toEntry)
                    .toArray(Map.Entry[]::new));
        }
    }
}
