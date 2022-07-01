package net.william278.husksync.database;

import com.zaxxer.hikari.HikariDataSource;
import net.william278.husksync.config.Settings;
import net.william278.husksync.data.UserData;
import net.william278.husksync.player.User;
import net.william278.husksync.util.Logger;
import net.william278.husksync.util.ResourceReader;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class MySqlDatabase extends Database {
    /**
     * MySQL server hostname
     */
    private final String mySqlHost;

    /**
     * MySQL server port
     */
    private final int mySqlPort;

    /**
     * Database to use on the MySQL server
     */
    private final String mySqlDatabaseName;
    private final String mySqlUsername;
    private final String mySqlPassword;
    private final String mySqlConnectionParameters;

    private final int hikariMaximumPoolSize;
    private final int hikariMinimumIdle;
    private final int hikariMaximumLifetime;
    private final int hikariKeepAliveTime;
    private final int hikariConnectionTimeOut;

    private static final String DATA_POOL_NAME = "HuskHomesHikariPool";

    private HikariDataSource dataSource;

    public MySqlDatabase(@NotNull Settings settings, @NotNull ResourceReader resourceReader, @NotNull Logger logger) {
        super(settings.getStringValue(Settings.ConfigOption.DATABASE_PLAYERS_TABLE_NAME),
                settings.getStringValue(Settings.ConfigOption.DATABASE_DATA_TABLE_NAME),
                settings.getIntegerValue(Settings.ConfigOption.SYNCHRONIZATION_MAX_USER_DATA_RECORDS),
                resourceReader, logger);
        mySqlHost = settings.getStringValue(Settings.ConfigOption.DATABASE_HOST);
        mySqlPort = settings.getIntegerValue(Settings.ConfigOption.DATABASE_PORT);
        mySqlDatabaseName = settings.getStringValue(Settings.ConfigOption.DATABASE_NAME);
        mySqlUsername = settings.getStringValue(Settings.ConfigOption.DATABASE_USERNAME);
        mySqlPassword = settings.getStringValue(Settings.ConfigOption.DATABASE_PASSWORD);
        mySqlConnectionParameters = settings.getStringValue(Settings.ConfigOption.DATABASE_CONNECTION_PARAMS);
        hikariMaximumPoolSize = settings.getIntegerValue(Settings.ConfigOption.DATABASE_CONNECTION_POOL_MAX_SIZE);
        hikariMinimumIdle = settings.getIntegerValue(Settings.ConfigOption.DATABASE_CONNECTION_POOL_MIN_IDLE);
        hikariMaximumLifetime = settings.getIntegerValue(Settings.ConfigOption.DATABASE_CONNECTION_POOL_MAX_LIFETIME);
        hikariKeepAliveTime = settings.getIntegerValue(Settings.ConfigOption.DATABASE_CONNECTION_POOL_KEEPALIVE);
        hikariConnectionTimeOut = settings.getIntegerValue(Settings.ConfigOption.DATABASE_CONNECTION_POOL_TIMEOUT);
    }

    /**
     * Fetch the auto-closeable connection from the hikariDataSource
     *
     * @return The {@link Connection} to the MySQL database
     * @throws SQLException if the connection fails for some reason
     */
    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            // Create jdbc driver connection url
            final String jdbcUrl = "jdbc:mysql://" + mySqlHost + ":" + mySqlPort + "/" + mySqlDatabaseName + mySqlConnectionParameters;
            dataSource = new HikariDataSource();
            dataSource.setJdbcUrl(jdbcUrl);

            // Authenticate
            dataSource.setUsername(mySqlUsername);
            dataSource.setPassword(mySqlPassword);

            // Set various additional parameters
            dataSource.setMaximumPoolSize(hikariMaximumPoolSize);
            dataSource.setMinimumIdle(hikariMinimumIdle);
            dataSource.setMaxLifetime(hikariMaximumLifetime);
            dataSource.setKeepaliveTime(hikariKeepAliveTime);
            dataSource.setConnectionTimeout(hikariConnectionTimeOut);
            dataSource.setPoolName(DATA_POOL_NAME);

            // Prepare database schema; make tables if they don't exist
            try (Connection connection = dataSource.getConnection()) {
                // Load database schema CREATE statements from schema file
                final String[] databaseSchema = getSchemaStatements("database/mysql_schema.sql");
                try (Statement statement = connection.createStatement()) {
                    for (String tableCreationStatement : databaseSchema) {
                        statement.execute(tableCreationStatement);
                    }
                }
            } catch (SQLException | IOException e) {
                getLogger().log(Level.SEVERE, "An error occurred creating tables on the MySQL database: ", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> ensureUser(@NotNull User user) {
        return CompletableFuture.runAsync(() -> getUser(user.uuid).thenAccept(optionalUser ->
                optionalUser.ifPresentOrElse(existingUser -> {
                            if (!existingUser.username.equals(user.username)) {
                                // Update a user's name if it has changed in the database
                                try (Connection connection = getConnection()) {
                                    try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                                            UPDATE `%players_table%`
                                            SET `username`=?
                                            WHERE `uuid`=?"""))) {

                                        statement.setString(1, user.username);
                                        statement.setString(2, existingUser.uuid.toString());
                                        statement.executeUpdate();
                                    }
                                    getLogger().log(Level.INFO, "Updated " + user.username + "'s name in the database (" + existingUser.username + " -> " + user.username + ")");
                                } catch (SQLException e) {
                                    getLogger().log(Level.SEVERE, "Failed to update a user's name on the database", e);
                                }
                            }
                        },
                        () -> {
                            // Insert new player data into the database
                            try (Connection connection = getConnection()) {
                                try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                                        INSERT INTO `%players_table%` (`uuid`,`username`)
                                        VALUES (?,?);"""))) {

                                    statement.setString(1, user.uuid.toString());
                                    statement.setString(2, user.username);
                                    statement.executeUpdate();
                                }
                            } catch (SQLException e) {
                                getLogger().log(Level.SEVERE, "Failed to insert a user into the database", e);
                            }
                        })));
    }

    @Override
    public CompletableFuture<Optional<User>> getUser(@NotNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                        SELECT `uuid`, `username`
                        FROM `%players_table%`
                        WHERE `uuid`=?"""))) {

                    statement.setString(1, uuid.toString());

                    final ResultSet resultSet = statement.executeQuery();
                    if (resultSet.next()) {
                        return Optional.of(new User(UUID.fromString(resultSet.getString("uuid")),
                                resultSet.getString("username")));
                    }
                }
            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "Failed to fetch a user from uuid from the database", e);
            }
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<Optional<User>> getUserByName(@NotNull String username) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                        SELECT `uuid`, `username`
                        FROM `%players_table%`
                        WHERE `username`=?"""))) {
                    statement.setString(1, username);

                    final ResultSet resultSet = statement.executeQuery();
                    if (resultSet.next()) {
                        return Optional.of(new User(UUID.fromString(resultSet.getString("uuid")),
                                resultSet.getString("username")));
                    }
                }
            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "Failed to fetch a user by name from the database", e);
            }
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<Optional<UserData>> getCurrentUserData(@NotNull User user) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                        SELECT `version_uuid`, `timestamp`, `data`
                        FROM `%data_table%`
                        WHERE `player_uuid`=?
                        ORDER BY `timestamp` DESC
                        LIMIT 1;"""))) {
                    statement.setString(1, user.uuid.toString());
                    final ResultSet resultSet = statement.executeQuery();
                    if (resultSet.next()) {
                        final UserData data = UserData.fromJson(resultSet.getString("data"));
                        data.setMetadata(UUID.fromString(resultSet.getString("version_uuid")),
                                resultSet.getTimestamp("timestamp").toInstant().toEpochMilli());
                        return Optional.of(data);
                    }
                }
            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "Failed to fetch a user's current user data from the database", e);
            }
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<List<UserData>> getUserData(@NotNull User user) {
        return CompletableFuture.supplyAsync(() -> {
            final ArrayList<UserData> retrievedData = new ArrayList<>();
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                        SELECT `version_uuid`, `timestamp`, `data`
                        FROM `%data_table%`
                        WHERE `player_uuid`=?
                        ORDER BY `timestamp` DESC;"""))) {
                    statement.setString(1, user.uuid.toString());
                    final ResultSet resultSet = statement.executeQuery();
                    while (resultSet.next()) {
                        final UserData data = UserData.fromJson(resultSet.getString("data"));
                        data.setMetadata(UUID.fromString(resultSet.getString("version_uuid")),
                                resultSet.getTimestamp("timestamp").toInstant().toEpochMilli());
                        retrievedData.add(data);
                    }
                    return retrievedData;
                }
            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "Failed to fetch a user's current user data from the database", e);
            }
            return retrievedData;
        });
    }

    @Override
    protected CompletableFuture<Void> pruneUserDataRecords(@NotNull User user) {
        return CompletableFuture.runAsync(() -> getUserData(user).thenAccept(data -> {
            if (data.size() > maxUserDataRecords) {
                Collections.reverse(data);
                data.subList(0, data.size() - maxUserDataRecords).forEach(dataToDelete -> {
                    try (Connection connection = getConnection()) {
                        try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                                DELETE FROM `%data_table%`
                                WHERE `version_uuid`=?"""))) {
                            statement.setString(1, dataToDelete.getDataUuidVersion().toString());
                            statement.executeUpdate();
                        }
                    } catch (SQLException e) {
                        getLogger().log(Level.SEVERE, "Failed to prune user data from the database", e);
                    }
                });
            }
        }));
    }

    @Override
    public CompletableFuture<Void> setUserData(@NotNull User user, @NotNull UserData userData) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                        INSERT INTO `%data_table%`
                        (`player_uuid`,`version_uuid`,`timestamp`,`data`)
                        VALUES (?,?,?,?);"""))) {
                    statement.setString(1, user.uuid.toString());
                    statement.setString(2, userData.getDataUuidVersion().toString());
                    statement.setTimestamp(3, Timestamp.from(Instant.ofEpochMilli(userData.getCreationTimestamp())));
                    statement.setString(4, userData.toJson());
                    statement.executeUpdate();
                }
            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "Failed to set user data in the database", e);
            }
        }).thenRunAsync(() -> pruneUserDataRecords(user).join());
    }
}
