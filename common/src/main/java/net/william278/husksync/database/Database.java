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
import net.william278.husksync.data.DataSaveCause;
import net.william278.husksync.data.UserData;
import net.william278.husksync.data.UserDataSnapshot;
import net.william278.husksync.migrator.Migrator;
import net.william278.husksync.player.User;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
    protected final String[] getSchemaStatements(@NotNull String schemaFileName) throws IOException {
        return formatStatementTables(new String(Objects.requireNonNull(plugin.getResource(schemaFileName))
                .readAllBytes(), StandardCharsets.UTF_8)).split(";");
    }

    /**
     * Format all table name placeholder strings in a SQL statement
     *
     * @param sql the SQL statement with un-formatted table name placeholders
     * @return the formatted statement, with table placeholders replaced with the correct names
     */
    protected final String formatStatementTables(@NotNull String sql) {
        return sql.replaceAll("%users_table%", plugin.getSettings().getTableName(Settings.TableName.USERS))
                .replaceAll("%user_data_table%", plugin.getSettings().getTableName(Settings.TableName.USER_DATA));
    }

    /**
     * Initialize the database and ensure tables are present; create tables if they do not exist.
     */
    public abstract void initialize();

    /**
     * Ensure a {@link User} has an entry in the database and that their username is up-to-date
     *
     * @param user The {@link User} to ensure
     * @return A future returning void when complete
     */
    public abstract CompletableFuture<Void> ensureUser(@NotNull User user);

    /**
     * Get a player by their Minecraft account {@link UUID}
     *
     * @param uuid Minecraft account {@link UUID} of the {@link User} to get
     * @return A future returning an optional with the {@link User} present if they exist
     */
    public abstract CompletableFuture<Optional<User>> getUser(@NotNull UUID uuid);

    /**
     * Get a user by their username (<i>case-insensitive</i>)
     *
     * @param username Username of the {@link User} to get (<i>case-insensitive</i>)
     * @return A future returning an optional with the {@link User} present if they exist
     */
    public abstract CompletableFuture<Optional<User>> getUserByName(@NotNull String username);

    /**
     * Get the current uniquely versioned user data for a given user, if it exists.
     *
     * @param user the user to get data for
     * @return an optional containing the {@link UserDataSnapshot}, if it exists, or an empty optional if it does not
     */
    public abstract CompletableFuture<Optional<UserDataSnapshot>> getCurrentUserData(@NotNull User user);

    /**
     * Get all {@link UserDataSnapshot} entries for a user from the database.
     *
     * @param user The user to get data for
     * @return A future returning a list of a user's {@link UserDataSnapshot} entries
     */
    public abstract CompletableFuture<List<UserDataSnapshot>> getUserData(@NotNull User user);

    /**
     * Gets a specific {@link UserDataSnapshot} entry for a user from the database, by its UUID.
     *
     * @param user        The user to get data for
     * @param versionUuid The UUID of the {@link UserDataSnapshot} entry to get
     * @return A future returning an optional containing the {@link UserDataSnapshot}, if it exists, or an empty optional if it does not
     */
    public abstract CompletableFuture<Optional<UserDataSnapshot>> getUserData(@NotNull User user, @NotNull UUID versionUuid);

    /**
     * <b>(Internal)</b> Prune user data for a given user to the maximum value as configured.
     *
     * @param user The user to prune data for
     * @implNote Data snapshots marked as {@code pinned} are exempt from rotation
     */
    protected abstract void rotateUserData(@NotNull User user);

    /**
     * Deletes a specific {@link UserDataSnapshot} entry for a user from the database, by its UUID.
     *
     * @param user        The user to get data for
     * @param versionUuid The UUID of the {@link UserDataSnapshot} entry to delete
     * @return A future returning void when complete
     */
    public abstract CompletableFuture<Boolean> deleteUserData(@NotNull User user, @NotNull UUID versionUuid);

    /**
     * Save user data to the database<p>
     * This will remove the oldest data for the user if the amount of data exceeds the limit as configured
     *
     * @param user     The user to add data for
     * @param userData The {@link UserData} to set. The implementation should version it with a random UUID and the current timestamp during insertion.
     * @return A future returning void when complete
     * @see UserDataSnapshot#create(UserData)
     */
    public abstract CompletableFuture<Void> setUserData(@NotNull User user, @NotNull UserData userData, @NotNull DataSaveCause dataSaveCause);

    /**
     * Pin a saved {@link UserDataSnapshot} by given version UUID, setting it's {@code pinned} state to {@code true}.
     *
     * @param user        The user to pin the data for
     * @param versionUuid The UUID of the user's {@link UserDataSnapshot} entry to pin
     * @return A future returning a boolean; {@code true} if the operation completed successfully, {@code false} if it failed
     * @see UserDataSnapshot#pinned()
     */
    public abstract CompletableFuture<Void> pinUserData(@NotNull User user, @NotNull UUID versionUuid);

    /**
     * Unpin a saved {@link UserDataSnapshot} by given version UUID, setting it's {@code pinned} state to {@code false}.
     *
     * @param user        The user to unpin the data for
     * @param versionUuid The UUID of the user's {@link UserDataSnapshot} entry to unpin
     * @return A future returning a boolean; {@code true} if the operation completed successfully, {@code false} if it failed
     * @see UserDataSnapshot#pinned()
     */
    public abstract CompletableFuture<Void> unpinUserData(@NotNull User user, @NotNull UUID versionUuid);

    /**
     * Wipes <b>all</b> {@link UserData} entries from the database.
     * <b>This should never be used</b>, except when preparing tables for migration.
     *
     * @return A future returning void when complete
     * @see Migrator#start()
     */
    public abstract CompletableFuture<Void> wipeDatabase();

    /**
     * Close the database connection
     */
    public abstract void close();

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
