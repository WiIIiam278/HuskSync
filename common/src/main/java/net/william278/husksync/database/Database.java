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
import net.william278.husksync.data.PlayerDataHolder;
import net.william278.husksync.migrator.Migrator;
import net.william278.husksync.player.User;
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
    public abstract void initialize() throws IllegalStateException;

    /**
     * Ensure a {@link User} has an entry in the database and that their username is up-to-date
     *
     * @param user The {@link User} to ensure
     */
    public abstract void ensureUser(@NotNull User user);

    /**
     * Get a player by their Minecraft account {@link UUID}
     *
     * @param uuid Minecraft account {@link UUID} of the {@link User} to get
     * @return A future returning an optional with the {@link User} present if they exist
     */
    public abstract Optional<User> getUser(@NotNull UUID uuid);

    /**
     * Get a user by their username (<i>case-insensitive</i>)
     *
     * @param username Username of the {@link User} to get (<i>case-insensitive</i>)
     * @return A future returning an optional with the {@link User} present if they exist
     */
    public abstract Optional<User> getUserByName(@NotNull String username);


    /**
     * Get the latest data snapshot for a user.
     *
     * @param user The user to get data for
     * @return an optional containing the {@link DataSnapshot}, if it exists, or an empty optional if it does not
     */
    public abstract Optional<DataSnapshot.Packed> getLatestDataSnapshot(@NotNull User user);

    /**
     * Get the latest data snapshot for a user that is not pinned.
     *
     * @param user The user to get data for
     * @return an optional containing the {@link DataSnapshot}, if it exists, or an empty optional if it does not
     */
    public abstract Optional<DataSnapshot.Packed> getLatestUnpinnedDataSnapshot(@NotNull User user);

    /**
     * Get all {@link DataSnapshot} entries for a user from the database.
     *
     * @param user The user to get data for
     * @return A future returning a list of a user's {@link DataSnapshot} entries
     */
    @NotNull
    public abstract List<DataSnapshot.Packed> getDataSnapshots(@NotNull User user);

    /**
     * Gets a specific {@link DataSnapshot} entry for a user from the database, by its UUID.
     *
     * @param user        The user to get data for
     * @param versionUuid The UUID of the {@link DataSnapshot} entry to get
     * @return A future returning an optional containing the {@link DataSnapshot}, if it exists, or an empty optional if it does not
     */
    public abstract Optional<DataSnapshot.Packed> getDataSnapshot(@NotNull User user, @NotNull UUID versionUuid);

    /**
     * <b>(Internal)</b> Prune user data for a given user to the maximum value as configured.
     *
     * @param user The user to prune data for
     * @implNote Data snapshots marked as {@code pinned} are exempt from rotation
     */
    protected abstract void rotateUserData(@NotNull User user);

    /**
     * Deletes a specific {@link DataSnapshot} entry for a user from the database, by its UUID.
     *
     * @param user        The user to get data for
     * @param versionUuid The UUID of the {@link DataSnapshot} entry to delete
     */
    public abstract boolean deleteUserData(@NotNull User user, @NotNull UUID versionUuid);

    /**
     * Save user data to the database
     * </p>
     * This will remove the oldest data for the user if the amount of data exceeds the limit as configured
     *
     * @param user     The user to add data for
     * @param snapshot The {@link DataSnapshot} to set.
     *                 The implementation should version it with a random UUID and the current timestamp during insertion.
     * @see PlayerDataHolder#createSnapshot(SaveCause)
     */
    public void setUserData(@NotNull User user, @NotNull DataSnapshot.Packed snapshot) {
        if (snapshot.getSaveCause() != SaveCause.SERVER_SHUTDOWN) {
            plugin.fireEvent(
                    plugin.getDataSaveEvent(user, snapshot),
                    (event) -> this.saveDataSnapshot(user, snapshot)
            );
            return;
        }

        this.saveDataSnapshot(user, snapshot);
    }

    /**
     * <b>Internal</b> - Save user data to the database. This will:
     * <ol>
     *     <li>Determine the snapshot to replace, if needed</li>
     *     <li>Create the snapshot</li>
     *     <li>Delete the snapshot to replace, if needed</li>
     *     <li>Rotate snapshot backups</li>
     * </ol>
     *
     * @param user     The user to add data for
     * @param snapshot The {@link DataSnapshot} to set.
     */
    private void saveDataSnapshot(@NotNull User user, @NotNull DataSnapshot.Packed snapshot) {
        final Optional<UUID> toDelete = getSnapshotToOverwrite(user, snapshot.getTimestamp());
        this.createDataSnapshot(user, snapshot);
        toDelete.ifPresent(uuid -> this.deleteUserData(user, uuid));
        this.rotateUserData(user);
    }

    /**
     * Returns the ID of the latest snapshot that should be replaced, provided the backup frequency is greater than 0,
     * and the new snapshot is older than the latest snapshot by the backup frequency.
     *
     * @param user The user who owns the snapshot
     * @param time The time to check against
     * @return The UUID of the snapshot to replace, if it exists
     */
    private Optional<UUID> getSnapshotToOverwrite(@NotNull User user, @NotNull OffsetDateTime time) {
        final int backupFrequency = plugin.getSettings().getSnapshotBackupFrequency();
        if (backupFrequency <= 0) {
            return Optional.empty();
        }
        return getLatestUnpinnedDataSnapshot(user).flatMap(
                latest -> latest.getTimestamp().plusHours(backupFrequency).isBefore(time)
                        ? Optional.of(latest.getId()) : Optional.empty()
        );
    }

    /**
     * <b>Internal</b> - Create user data in the database
     *
     * @param user The user to add data for
     * @param data The {@link DataSnapshot} to set.
     */
    protected abstract void createDataSnapshot(@NotNull User user, @NotNull DataSnapshot.Packed data);

    /**
     * Update a saved {@link DataSnapshot} by given version UUID
     *
     * @param user        The user whose data snapshot
     * @param versionUuid The UUID of the user's {@link DataSnapshot} entry
     * @param snapshot    The {@link DataSnapshot} to update
     */
    protected abstract void updateUserData(@NotNull User user, @NotNull UUID versionUuid,
                                           @NotNull DataSnapshot.Packed snapshot);

    /**
     * Unpin a saved {@link DataSnapshot} by given version UUID, setting it's {@code pinned} state to {@code false}.
     *
     * @param user        The user to unpin the data for
     * @param versionUuid The UUID of the user's {@link DataSnapshot} entry to unpin
     * @see DataSnapshot#isPinned()
     */
    public final void unpinUserData(@NotNull User user, @NotNull UUID versionUuid) {
        this.getDataSnapshot(user, versionUuid).ifPresent(data -> {
            data.edit(plugin, (snapshot) -> snapshot.setPinned(false));
            this.updateUserData(user, versionUuid, data);
        });
    }

    /**
     * Pin a saved {@link DataSnapshot} by given version UUID, setting it's {@code pinned} state to {@code true}.
     *
     * @param user        The user to pin the data for
     * @param versionUuid The UUID of the user's {@link DataSnapshot} entry to pin
     */
    public final void pinUserData(@NotNull User user, @NotNull UUID versionUuid) {
        this.getDataSnapshot(user, versionUuid).ifPresent(data -> {
            data.edit(plugin, (snapshot) -> snapshot.setPinned(true));
            this.updateUserData(user, versionUuid, data);
        });
    }

    /**
     * Wipes <b>all</b> {@link User} entries from the database.
     * <b>This should never be used</b>, except when preparing tables for migration.
     *
     * @see Migrator#start()
     */
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
