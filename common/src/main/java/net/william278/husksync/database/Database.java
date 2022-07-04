package net.william278.husksync.database;

import net.william278.husksync.data.UserData;
import net.william278.husksync.data.VersionedUserData;
import net.william278.husksync.player.User;
import net.william278.husksync.util.Logger;
import net.william278.husksync.util.ResourceReader;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * An abstract representation of the plugin database, storing player data.
 * <p>
 * Implemented by different database platforms - MySQL, SQLite, etc. - as configured by the administrator.
 */
public abstract class Database {

    /**
     * Name of the table that stores player information
     */
    protected final String playerTableName;

    /**
     * Name of the table that stores data
     */
    protected final String dataTableName;

    /**
     * The maximum number of user records to store in the database at once per user
     */
    protected final int maxUserDataRecords;

    /**
     * Logger instance used for database error logging
     */
    private final Logger logger;

    /**
     * Returns the {@link Logger} used to log database errors
     *
     * @return the {@link Logger} instance
     */
    protected Logger getLogger() {
        return logger;
    }

    /**
     * The {@link ResourceReader} used to read internal resource files by name
     */
    private final ResourceReader resourceReader;

    protected Database(@NotNull String playerTableName, @NotNull String dataTableName, final int maxUserDataRecords,
                       @NotNull ResourceReader resourceReader, @NotNull Logger logger) {
        this.playerTableName = playerTableName;
        this.dataTableName = dataTableName;
        this.maxUserDataRecords = maxUserDataRecords;
        this.resourceReader = resourceReader;
        this.logger = logger;
    }

    /**
     * Loads SQL table creation schema statements from a resource file as a string array
     *
     * @param schemaFileName database script resource file to load from
     * @return Array of string-formatted table creation schema statements
     * @throws IOException if the resource could not be read
     */
    protected final String[] getSchemaStatements(@NotNull String schemaFileName) throws IOException {
        return formatStatementTables(new String(resourceReader.getResource(schemaFileName)
                .readAllBytes(), StandardCharsets.UTF_8)).split(";");
    }

    /**
     * Format all table name placeholder strings in a SQL statement
     *
     * @param sql the SQL statement with un-formatted table name placeholders
     * @return the formatted statement, with table placeholders replaced with the correct names
     */
    protected final String formatStatementTables(@NotNull String sql) {
        return sql.replaceAll("%players_table%", playerTableName)
                .replaceAll("%data_table%", dataTableName);
    }

    /**
     * Initialize the database and ensure tables are present; create tables if they do not exist.
     *
     * @return A future returning boolean - if the connection could be established.
     */
    public abstract boolean initialize();

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
     * @return an optional containing the {@link VersionedUserData}, if it exists, or an empty optional if it does not
     */
    public abstract CompletableFuture<Optional<VersionedUserData>> getCurrentUserData(@NotNull User user);

    /**
     * Get all {@link VersionedUserData} entries for a user from the database.
     *
     * @param user The user to get data for
     * @return A future returning a list of a user's {@link VersionedUserData} entries
     */
    public abstract CompletableFuture<List<VersionedUserData>> getUserData(@NotNull User user);

    /**
     * <b>(Internal)</b> Prune user data records for a given user to the maximum value as configured
     *
     * @param user The user to prune data for
     * @return A future returning void when complete
     */
    protected abstract CompletableFuture<Void> pruneUserDataRecords(@NotNull User user);

    /**
     * Add user data to the database<p>
     * This will remove the oldest data for the user if the amount of data exceeds the limit as configured
     *
     * @param user     The user to add data for
     * @param userData The {@link UserData} to set. The implementation should version it with a random UUID and the current timestamp during insertion.
     * @return A future returning void when complete
     * @see VersionedUserData#version(UserData)
     */
    public abstract CompletableFuture<Void> setUserData(@NotNull User user, @NotNull UserData userData);

    /**
     * Close the database connection
     */
    public abstract void close();

}
