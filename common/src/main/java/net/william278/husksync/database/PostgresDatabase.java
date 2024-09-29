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

import com.google.common.collect.Lists;
import com.zaxxer.hikari.HikariDataSource;
import net.william278.husksync.HuskSync;
import net.william278.husksync.adapter.DataAdapter;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.user.User;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.logging.Level;

import static net.william278.husksync.config.Settings.DatabaseSettings;

public class PostgresDatabase extends Database {

    private static final String DATA_POOL_NAME = "HuskSyncHikariPool";
    private final String flavor;
    private final String driverClass;
    private HikariDataSource dataSource;

    public PostgresDatabase(@NotNull HuskSync plugin) {
        super(plugin);

        final Type type = plugin.getSettings().getDatabase().getType();
        this.flavor = type.getProtocol();
        this.driverClass = "org.postgresql.Driver";
    }

    @Blocking
    @NotNull
    private Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new IllegalStateException("The database has not been initialized");
        }
        return dataSource.getConnection();
    }

    @Blocking
    @Override
    public void initialize() throws IllegalStateException {
        // Initialize the Hikari pooled connection
        final DatabaseSettings.DatabaseCredentials credentials = plugin.getSettings().getDatabase().getCredentials();
        dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driverClass);
        dataSource.setJdbcUrl(String.format("jdbc:%s://%s:%s/%s%s",
                flavor,
                credentials.getHost(),
                credentials.getPort(),
                credentials.getDatabase(),
                credentials.getParameters()
        ));

        // Authenticate with the database
        dataSource.setUsername(credentials.getUsername());
        dataSource.setPassword(credentials.getPassword());

        // Set connection pool options
        final DatabaseSettings.PoolSettings pool = plugin.getSettings().getDatabase().getConnectionPool();
        dataSource.setMaximumPoolSize(pool.getMaximumPoolSize());
        dataSource.setMinimumIdle(pool.getMinimumIdle());
        dataSource.setMaxLifetime(pool.getMaximumLifetime());
        dataSource.setKeepaliveTime(pool.getKeepaliveTime());
        dataSource.setConnectionTimeout(pool.getConnectionTimeout());
        dataSource.setPoolName(DATA_POOL_NAME);

        // Set additional connection pool properties
        final Properties properties = new Properties();
        properties.putAll(
                Map.of("cachePrepStmts", "true",
                        "prepStmtCacheSize", "250",
                        "prepStmtCacheSqlLimit", "2048",
                        "useServerPrepStmts", "true",
                        "useLocalSessionState", "true",
                        "useLocalTransactionState", "true"
                ));
        properties.putAll(
                Map.of(
                        "rewriteBatchedStatements", "true",
                        "cacheResultSetMetadata", "true",
                        "cacheServerConfiguration", "true",
                        "elideSetAutoCommits", "true",
                        "maintainTimeStats", "false")
        );
        dataSource.setDataSourceProperties(properties);

        // Check config for if tables should be created
        if (!plugin.getSettings().getDatabase().isCreateTables()) return;

        // Prepare database schema; make tables if they don't exist
        try (Connection connection = dataSource.getConnection()) {
            final String[] databaseSchema = getSchemaStatements(String.format("database/%s_schema.sql", flavor));
            try (Statement statement = connection.createStatement()) {
                for (String tableCreationStatement : databaseSchema) {
                    statement.execute(tableCreationStatement);
                }
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to create database tables. Please ensure you are running PostgreSQL " +
                                                "and that your connecting user account has privileges to create tables.", e);
            }
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Failed to establish a connection to the PostgreSQL database. " +
                                            "Please check the supplied database credentials in the config file", e);
        }
    }

    @Blocking
    @Override
    public void ensureUser(@NotNull User user) {
        getUser(user.getUuid()).ifPresentOrElse(
                existingUser -> {
                    if (!existingUser.getUsername().equals(user.getUsername())) {
                        // Update a user's name if it has changed in the database
                        try (Connection connection = getConnection()) {
                            try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                                    UPDATE %users_table%
                                    SET username=?
                                    WHERE uuid=?;"""))) {

                                statement.setString(1, user.getUsername());
                                statement.setObject(2, existingUser.getUuid());
                                statement.executeUpdate();
                            }
                            plugin.log(Level.INFO, "Updated " + user.getUsername() + "'s name in the database (" + existingUser.getUsername() + " -> " + user.getUsername() + ")");
                        } catch (SQLException e) {
                            plugin.log(Level.SEVERE, "Failed to update a user's name on the database", e);
                        }
                    }
                },
                () -> {
                    // Insert new player data into the database
                    try (Connection connection = getConnection()) {
                        try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                                INSERT INTO %users_table% (uuid,username)
                                VALUES (?,?);"""))) {

                            statement.setObject(1, user.getUuid());
                            statement.setString(2, user.getUsername());
                            statement.executeUpdate();
                        }
                    } catch (SQLException e) {
                        plugin.log(Level.SEVERE, "Failed to insert a user into the database", e);
                    }
                }
        );
    }

    @Blocking
    @Override
    public Optional<User> getUser(@NotNull UUID uuid) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                    SELECT uuid, username
                    FROM %users_table%
                    WHERE uuid=?;"""))) {

                statement.setObject(1, uuid);

                final ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    return Optional.of(new User((UUID) resultSet.getObject("uuid"),
                            resultSet.getString("username")));
                }
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "Failed to fetch a user from uuid from the database", e);
        }
        return Optional.empty();
    }

    @Blocking
    @Override
    public Optional<User> getUserByName(@NotNull String username) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                    SELECT uuid, username
                    FROM %users_table%
                    WHERE username=?;"""))) {
                statement.setString(1, username);

                final ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    return Optional.of(new User((UUID) resultSet.getObject("uuid"),
                            resultSet.getString("username")));
                }
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "Failed to fetch a user by name from the database", e);
        }
        return Optional.empty();
    }


    @Override
    @NotNull
    public List<User> getAllUsers() {
        final List<User> users = Lists.newArrayList();
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                    SELECT uuid, username
                    FROM %users_table%;
                    """))) {
                final ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    users.add(new User(UUID.fromString(resultSet.getString("uuid")),
                            resultSet.getString("username")));
                }
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "Failed to fetch a user by name from the database", e);
        }
        return users;
    }

    @Blocking
    @Override
    public Optional<DataSnapshot.Packed> getLatestSnapshot(@NotNull User user) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                    SELECT version_uuid, timestamp, data
                    FROM %user_data_table%
                    WHERE player_uuid=?
                    ORDER BY timestamp DESC
                    LIMIT 1;"""))) {
                statement.setObject(1, user.getUuid());
                final ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    final UUID versionUuid = (UUID) resultSet.getObject("version_uuid");
                    final OffsetDateTime timestamp = OffsetDateTime.ofInstant(
                            resultSet.getTimestamp("timestamp").toInstant(), TimeZone.getDefault().toZoneId()
                    );
                    final byte[] dataByteArray = resultSet.getBytes("data");
                    return Optional.of(DataSnapshot.deserialize(plugin, dataByteArray, versionUuid, timestamp));
                }
            }
        } catch (SQLException | DataAdapter.AdaptionException e) {
            plugin.log(Level.SEVERE, "Failed to fetch a user's current user data from the database", e);
        }
        return Optional.empty();
    }

    @Blocking
    @Override
    @NotNull
    public List<DataSnapshot.Packed> getAllSnapshots(@NotNull User user) {
        final List<DataSnapshot.Packed> retrievedData = Lists.newArrayList();
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                    SELECT version_uuid, timestamp,  data
                    FROM %user_data_table%
                    WHERE player_uuid=?
                    ORDER BY timestamp DESC;"""))) {
                statement.setObject(1, user.getUuid());
                final ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    final UUID versionUuid = (UUID) resultSet.getObject("version_uuid");
                    final OffsetDateTime timestamp = OffsetDateTime.ofInstant(
                            resultSet.getTimestamp("timestamp").toInstant(), TimeZone.getDefault().toZoneId()
                    );
                    final byte[] dataByteArray = resultSet.getBytes("data");
                    retrievedData.add(DataSnapshot.deserialize(plugin, dataByteArray, versionUuid, timestamp));
                }
                return retrievedData;
            }
        } catch (SQLException | DataAdapter.AdaptionException e) {
            plugin.log(Level.SEVERE, "Failed to fetch a user's current user data from the database", e);
        }
        return retrievedData;
    }

    @Blocking
    @Override
    public Optional<DataSnapshot.Packed> getSnapshot(@NotNull User user, @NotNull UUID versionUuid) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                    SELECT version_uuid, timestamp,  data
                    FROM %user_data_table%
                    WHERE player_uuid=? AND version_uuid=?
                    ORDER BY timestamp DESC
                    LIMIT 1;"""))) {
                statement.setObject(1, user.getUuid());
                statement.setObject(2, versionUuid);
                final ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    final OffsetDateTime timestamp = OffsetDateTime.ofInstant(
                            resultSet.getTimestamp("timestamp").toInstant(), TimeZone.getDefault().toZoneId()
                    );
                    final byte[] dataByteArray = resultSet.getBytes("data");
                    return Optional.of(DataSnapshot.deserialize(plugin, dataByteArray, versionUuid, timestamp));
                }
            }
        } catch (SQLException | DataAdapter.AdaptionException e) {
            plugin.log(Level.SEVERE, "Failed to fetch specific user data by UUID from the database", e);
        }
        return Optional.empty();
    }

    @Blocking
    @Override
    protected void rotateSnapshots(@NotNull User user) {
        final List<DataSnapshot.Packed> unpinnedUserData = getAllSnapshots(user).stream()
                .filter(dataSnapshot -> !dataSnapshot.isPinned()).toList();
        final int maxSnapshots = plugin.getSettings().getSynchronization().getMaxUserDataSnapshots();
        if (unpinnedUserData.size() > maxSnapshots) {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                        WITH cte AS (
                          SELECT version_uuid
                          FROM %user_data_table%
                          WHERE player_uuid=?
                            AND pinned=FALSE
                          ORDER BY timestamp ASC
                          LIMIT %entry_count%
                        )
                        DELETE FROM %user_data_table%
                        WHERE version_uuid IN (SELECT version_uuid FROM cte);""".replace("%entry_count%",
                        Integer.toString(unpinnedUserData.size() - maxSnapshots))))) {
                    statement.setObject(1, user.getUuid());
                    statement.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.log(Level.SEVERE, "Failed to prune user data from the database", e);
            }
        }
    }

    @Blocking
    @Override
    public boolean deleteSnapshot(@NotNull User user, @NotNull UUID versionUuid) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                    DELETE FROM %user_data_table%
                    WHERE player_uuid=? AND version_uuid=?;"""))) {
                statement.setObject(1, user.getUuid());
                statement.setObject(2, versionUuid);
                return statement.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "Failed to delete specific user data from the database", e);
        }
        return false;
    }

    @Blocking
    @Override
    protected void rotateLatestSnapshot(@NotNull User user, @NotNull OffsetDateTime within) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                    DELETE FROM %user_data_table%
                    WHERE player_uuid=? AND timestamp = (
                       SELECT timestamp
                       FROM %user_data_table%
                       WHERE player_uuid=? AND timestamp > ? AND pinned=FALSE
                       ORDER BY timestamp ASC
                       LIMIT 1
                    );"""))) {
                statement.setObject(1, user.getUuid());
                statement.setObject(2, user.getUuid());
                statement.setTimestamp(3, Timestamp.from(within.toInstant()));
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "Failed to delete a user's data from the database", e);
        }
    }

    @Blocking
    @Override
    protected void createSnapshot(@NotNull User user, @NotNull DataSnapshot.Packed data) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                    INSERT INTO %user_data_table%
                    (player_uuid,version_uuid,timestamp,save_cause,pinned,data)
                    VALUES (?,?,?,?,?,?);"""))) {
                statement.setObject(1, user.getUuid());
                statement.setObject(2, data.getId());
                statement.setTimestamp(3, Timestamp.from(data.getTimestamp().toInstant()));
                statement.setString(4, data.getSaveCause().name());
                statement.setBoolean(5, data.isPinned());
                statement.setBytes(6, data.asBytes(plugin));
                statement.executeUpdate();
            }
        } catch (SQLException | DataAdapter.AdaptionException e) {
            plugin.log(Level.SEVERE, "Failed to set user data in the database", e);
        }
    }

    @Blocking
    @Override
    public void updateSnapshot(@NotNull User user, @NotNull DataSnapshot.Packed data) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                    UPDATE %user_data_table%
                    SET save_cause=?,pinned=?,data=?
                    WHERE player_uuid=? AND version_uuid=?
                    LIMIT 1;"""))) {
                statement.setString(1, data.getSaveCause().name());
                statement.setBoolean(2, data.isPinned());
                statement.setBytes(3, data.asBytes(plugin));
                statement.setObject(4, user.getUuid());
                statement.setObject(5, data.getId());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "Failed to pin user data in the database", e);
        }
    }

    @Override
    public void wipeDatabase() {
        try (Connection connection = getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(formatStatementTables("DELETE FROM %user_data_table%;"));
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "Failed to wipe the database", e);
        }
    }

    @Override
    public void terminate() {
        if (dataSource != null) {
            if (!dataSource.isClosed()) {
                dataSource.close();
            }
        }
    }

}
