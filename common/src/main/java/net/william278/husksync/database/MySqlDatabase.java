package net.william278.husksync.database;

import com.zaxxer.hikari.HikariDataSource;
import net.william278.husksync.config.Settings;
import net.william278.husksync.data.*;
import net.william278.husksync.event.DataSaveEvent;
import net.william278.husksync.event.EventCannon;
import net.william278.husksync.player.User;
import net.william278.husksync.util.Logger;
import net.william278.husksync.util.ResourceReader;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Date;
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
    private final long hikariMaximumLifetime;
    private final long hikariKeepAliveTime;
    private final long hikariConnectionTimeOut;

    private static final String DATA_POOL_NAME = "HuskSyncHikariPool";

    /**
     * The Hikari data source - a pool of database connections that can be fetched on-demand
     */
    private HikariDataSource connectionPool;

    public MySqlDatabase(@NotNull Settings settings, @NotNull ResourceReader resourceReader, @NotNull Logger logger,
                         @NotNull DataAdapter dataAdapter, @NotNull EventCannon eventCannon) {
        super(settings.getTableName(Settings.TableName.USERS),
                settings.getTableName(Settings.TableName.USER_DATA),
                Math.max(1, Math.min(20, settings.maxUserDataSnapshots)),
                resourceReader, dataAdapter, eventCannon, logger);
        this.mySqlHost = settings.mySqlHost;
        this.mySqlPort = settings.mySqlPort;
        this.mySqlDatabaseName = settings.mySqlDatabase;
        this.mySqlUsername = settings.mySqlUsername;
        this.mySqlPassword = settings.mySqlPassword;
        this.mySqlConnectionParameters = settings.mySqlConnectionParameters;
        this.hikariMaximumPoolSize = settings.mySqlConnectionPoolSize;
        this.hikariMinimumIdle = settings.mySqlConnectionPoolIdle;
        this.hikariMaximumLifetime = settings.mySqlConnectionPoolLifetime;
        this.hikariKeepAliveTime = settings.mySqlConnectionPoolKeepAlive;
        this.hikariConnectionTimeOut = settings.mySqlConnectionPoolTimeout;
    }

    /**
     * Fetch the auto-closeable connection from the hikariDataSource
     *
     * @return The {@link Connection} to the MySQL database
     * @throws SQLException if the connection fails for some reason
     */
    private Connection getConnection() throws SQLException {
        return connectionPool.getConnection();
    }

    @Override
    public boolean initialize() {
        try {
            // Create jdbc driver connection url
            final String jdbcUrl = "jdbc:mysql://" + mySqlHost + ":" + mySqlPort + "/" + mySqlDatabaseName + mySqlConnectionParameters;
            connectionPool = new HikariDataSource();
            connectionPool.setJdbcUrl(jdbcUrl);

            // Authenticate
            connectionPool.setUsername(mySqlUsername);
            connectionPool.setPassword(mySqlPassword);

            // Set various additional parameters
            connectionPool.setMaximumPoolSize(hikariMaximumPoolSize);
            connectionPool.setMinimumIdle(hikariMinimumIdle);
            connectionPool.setMaxLifetime(hikariMaximumLifetime);
            connectionPool.setKeepaliveTime(hikariKeepAliveTime);
            connectionPool.setConnectionTimeout(hikariConnectionTimeOut);
            connectionPool.setPoolName(DATA_POOL_NAME);

            // Prepare database schema; make tables if they don't exist
            try (Connection connection = connectionPool.getConnection()) {
                // Load database schema CREATE statements from schema file
                final String[] databaseSchema = getSchemaStatements("database/mysql_schema.sql");
                try (Statement statement = connection.createStatement()) {
                    for (String tableCreationStatement : databaseSchema) {
                        statement.execute(tableCreationStatement);
                    }
                }
                return true;
            } catch (SQLException | IOException e) {
                getLogger().log(Level.SEVERE, "Failed to perform database setup: " + e.getMessage());
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "An unhandled exception occurred during database setup!", e);
        }
        return false;
    }

    @Override
    public CompletableFuture<Void> ensureUser(@NotNull User user) {
        return getUser(user.uuid).thenAccept(optionalUser ->
                optionalUser.ifPresentOrElse(existingUser -> {
                            if (!existingUser.username.equals(user.username)) {
                                // Update a user's name if it has changed in the database
                                try (Connection connection = getConnection()) {
                                    try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                                            UPDATE `%users_table%`
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
                                        INSERT INTO `%users_table%` (`uuid`,`username`)
                                        VALUES (?,?);"""))) {

                                    statement.setString(1, user.uuid.toString());
                                    statement.setString(2, user.username);
                                    statement.executeUpdate();
                                }
                            } catch (SQLException e) {
                                getLogger().log(Level.SEVERE, "Failed to insert a user into the database", e);
                            }
                        }));
    }

    @Override
    public CompletableFuture<Optional<User>> getUser(@NotNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                        SELECT `uuid`, `username`
                        FROM `%users_table%`
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
                        FROM `%users_table%`
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
    public CompletableFuture<Optional<UserDataSnapshot>> getCurrentUserData(@NotNull User user) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                        SELECT `version_uuid`, `timestamp`, `save_cause`, `pinned`, `data`
                        FROM `%user_data_table%`
                        WHERE `player_uuid`=?
                        ORDER BY `timestamp` DESC
                        LIMIT 1;"""))) {
                    statement.setString(1, user.uuid.toString());
                    final ResultSet resultSet = statement.executeQuery();
                    if (resultSet.next()) {
                        final Blob blob = resultSet.getBlob("data");
                        final byte[] dataByteArray = blob.getBytes(1, (int) blob.length());
                        blob.free();
                        return Optional.of(new UserDataSnapshot(
                                UUID.fromString(resultSet.getString("version_uuid")),
                                Date.from(resultSet.getTimestamp("timestamp").toInstant()),
                                DataSaveCause.getCauseByName(resultSet.getString("save_cause")),
                                resultSet.getBoolean("pinned"),
                                getDataAdapter().fromBytes(dataByteArray)));
                    }
                }
            } catch (SQLException | DataAdaptionException e) {
                getLogger().log(Level.SEVERE, "Failed to fetch a user's current user data from the database", e);
            }
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<List<UserDataSnapshot>> getUserData(@NotNull User user) {
        return CompletableFuture.supplyAsync(() -> {
            final List<UserDataSnapshot> retrievedData = new ArrayList<>();
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                        SELECT `version_uuid`, `timestamp`, `save_cause`, `pinned`, `data`
                        FROM `%user_data_table%`
                        WHERE `player_uuid`=?
                        ORDER BY `timestamp` DESC;"""))) {
                    statement.setString(1, user.uuid.toString());
                    final ResultSet resultSet = statement.executeQuery();
                    while (resultSet.next()) {
                        final Blob blob = resultSet.getBlob("data");
                        final byte[] dataByteArray = blob.getBytes(1, (int) blob.length());
                        blob.free();
                        final UserDataSnapshot data = new UserDataSnapshot(
                                UUID.fromString(resultSet.getString("version_uuid")),
                                Date.from(resultSet.getTimestamp("timestamp").toInstant()),
                                DataSaveCause.getCauseByName(resultSet.getString("save_cause")),
                                resultSet.getBoolean("pinned"),
                                getDataAdapter().fromBytes(dataByteArray));
                        retrievedData.add(data);
                    }
                    return retrievedData;
                }
            } catch (SQLException | DataAdaptionException e) {
                getLogger().log(Level.SEVERE, "Failed to fetch a user's current user data from the database", e);
            }
            return retrievedData;
        });
    }

    @Override
    public CompletableFuture<Optional<UserDataSnapshot>> getUserData(@NotNull User user, @NotNull UUID versionUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                        SELECT `version_uuid`, `timestamp`, `save_cause`, `pinned`, `data`
                        FROM `%user_data_table%`
                        WHERE `player_uuid`=? AND `version_uuid`=?
                        ORDER BY `timestamp` DESC
                        LIMIT 1;"""))) {
                    statement.setString(1, user.uuid.toString());
                    statement.setString(2, versionUuid.toString());
                    final ResultSet resultSet = statement.executeQuery();
                    if (resultSet.next()) {
                        final Blob blob = resultSet.getBlob("data");
                        final byte[] dataByteArray = blob.getBytes(1, (int) blob.length());
                        blob.free();
                        return Optional.of(new UserDataSnapshot(
                                UUID.fromString(resultSet.getString("version_uuid")),
                                Date.from(resultSet.getTimestamp("timestamp").toInstant()),
                                DataSaveCause.getCauseByName(resultSet.getString("save_cause")),
                                resultSet.getBoolean("pinned"),
                                getDataAdapter().fromBytes(dataByteArray)));
                    }
                }
            } catch (SQLException | DataAdaptionException e) {
                getLogger().log(Level.SEVERE, "Failed to fetch specific user data by UUID from the database", e);
            }
            return Optional.empty();
        });
    }

    @Override
    protected CompletableFuture<Void> rotateUserData(@NotNull User user) {
        return CompletableFuture.runAsync(() -> {
            final List<UserDataSnapshot> unpinnedUserData = getUserData(user).join().stream()
                    .filter(dataSnapshot -> !dataSnapshot.pinned()).toList();
            if (unpinnedUserData.size() > maxUserDataRecords) {
                try (Connection connection = getConnection()) {
                    try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                            DELETE FROM `%user_data_table%`
                            WHERE `player_uuid`=?
                            AND `pinned` IS FALSE
                            ORDER BY `timestamp` ASC
                            LIMIT %entry_count%;""".replace("%entry_count%",
                            Integer.toString(unpinnedUserData.size() - maxUserDataRecords))))) {
                        statement.setString(1, user.uuid.toString());
                        statement.executeUpdate();
                    }
                } catch (SQLException e) {
                    getLogger().log(Level.SEVERE, "Failed to prune user data from the database", e);
                }
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> deleteUserData(@NotNull User user, @NotNull UUID versionUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                        DELETE FROM `%user_data_table%`
                        WHERE `player_uuid`=? AND `version_uuid`=?
                        LIMIT 1;"""))) {
                    statement.setString(1, user.uuid.toString());
                    statement.setString(2, versionUuid.toString());
                    return statement.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "Failed to delete specific user data from the database", e);
            }
            return false;
        });
    }

    @Override
    public CompletableFuture<Void> setUserData(@NotNull User user, @NotNull UserData userData,
                                               @NotNull DataSaveCause saveCause) {
        return CompletableFuture.runAsync(() -> {
            final DataSaveEvent dataSaveEvent = (DataSaveEvent) getEventCannon().fireDataSaveEvent(user,
                    userData, saveCause).join();
            if (!dataSaveEvent.isCancelled()) {
                final UserData finalData = dataSaveEvent.getUserData();
                try (Connection connection = getConnection()) {
                    try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                            INSERT INTO `%user_data_table%`
                            (`player_uuid`,`version_uuid`,`timestamp`,`save_cause`,`data`)
                            VALUES (?,UUID(),NOW(),?,?);"""))) {
                        statement.setString(1, user.uuid.toString());
                        statement.setString(2, saveCause.name());
                        statement.setBlob(3, new ByteArrayInputStream(
                                getDataAdapter().toBytes(finalData)));
                        statement.executeUpdate();
                    }
                } catch (SQLException | DataAdaptionException e) {
                    getLogger().log(Level.SEVERE, "Failed to set user data in the database", e);
                }
            }
        }).thenRun(() -> rotateUserData(user).join());
    }

    @Override
    public CompletableFuture<Void> pinUserData(@NotNull User user, @NotNull UUID versionUuid) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                        UPDATE `%user_data_table%`
                        SET `pinned`=TRUE
                        WHERE `player_uuid`=? AND `version_uuid`=?
                        LIMIT 1;"""))) {
                    statement.setString(1, user.uuid.toString());
                    statement.setString(2, versionUuid.toString());
                    statement.executeUpdate();
                }
            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "Failed to pin user data in the database", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> unpinUserData(@NotNull User user, @NotNull UUID versionUuid) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                        UPDATE `%user_data_table%`
                        SET `pinned`=FALSE
                        WHERE `player_uuid`=? AND `version_uuid`=?
                        LIMIT 1;"""))) {
                    statement.setString(1, user.uuid.toString());
                    statement.setString(2, versionUuid.toString());
                    statement.executeUpdate();
                }
            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "Failed to unpin user data in the database", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> wipeDatabase() {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection()) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate(formatStatementTables("DELETE FROM `%user_data_table%`;"));
                }
            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "Failed to wipe the database", e);
            }
        });
    }

    @Override
    public void close() {
        if (connectionPool != null) {
            if (!connectionPool.isClosed()) {
                connectionPool.close();
            }
        }
    }

}
