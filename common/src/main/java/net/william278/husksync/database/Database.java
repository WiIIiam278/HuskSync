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

import net.william278.husksync.HuskSync;
import net.william278.husksync.config.Settings;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.data.DataSnapshot.SaveCause;
import net.william278.husksync.data.UserDataHolder;
import net.william278.husksync.user.User;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
        return sql.replaceAll("%users_table%", plugin.getSettings().getTableName(Settings.TableName.USERS))
                .replaceAll("%user_data_table%", plugin.getSettings().getTableName(Settings.TableName.USER_DATA));
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
     * Save user data to the database
     * </p>
     * This will remove the oldest data for the user if the amount of data exceeds the limit as configured
     *
     * @param user     The user to add data for
     * @param snapshot The {@link DataSnapshot} to set.
     *                 The implementation should version it with a random UUID and the current timestamp during insertion.
     * @see UserDataHolder#createSnapshot(SaveCause)
     */
    @Blocking
    public void addSnapshot(@NotNull User user, @NotNull DataSnapshot.Packed snapshot) {
        if (snapshot.getSaveCause() != SaveCause.SERVER_SHUTDOWN) {
            plugin.fireEvent(
                    plugin.getDataSaveEvent(user, snapshot),
                    (event) -> this.addAndRotateSnapshot(user, snapshot)
            );
            return;
        }

        this.addAndRotateSnapshot(user, snapshot);
    }

    /**
     * <b>Internal</b> - Save user data to the database. This will:
     * <ol>
     *     <li>Delete their most recent snapshot, if it was created before the backup frequency time</li>
     *     <li>Create the snapshot</li>
     *     <li>Rotate snapshot backups</li>
     * </ol>
     *
     * @param user     The user to add data for
     * @param snapshot The {@link DataSnapshot} to set.
     */
    @Blocking
    private void addAndRotateSnapshot(@NotNull User user, @NotNull DataSnapshot.Packed snapshot) {
        final int backupFrequency = plugin.getSettings().getBackupFrequency();
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
    public enum Type {
        MYSQL("MySQL", "mysql"),
        MARIADB("MariaDB", "mariadb");

        private final String displayName;
        private final String protocol;

        Type(@NotNull String displayName, @NotNull String protocol) {
            this.displayName = displayName;
            this.protocol = protocol;
        }

        @NotNull
        public String getDisplayName() {
            return displayName;
        }

        @NotNull
        public String getProtocol() {
            return protocol;
        }
    }

}
