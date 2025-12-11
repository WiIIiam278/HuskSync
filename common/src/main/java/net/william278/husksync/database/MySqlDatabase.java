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
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.logging.Level;

import static net.william278.husksync.config.Settings.DatabaseSettings;

public class MySqlDatabase extends Database {

    private static final String DATA_POOL_NAME = "HuskSyncHikariPool";
    private final String flavor;
    private final String driverClass;
    private HikariDataSource dataSource;

    public MySqlDatabase(@NotNull HuskSync plugin) {
        super(plugin);

        final Type type = plugin.getSettings().getDatabase().getType();
        this.flavor = type.getProtocol();
        this.driverClass = type == Type.MARIADB ? "org.mariadb.jdbc.Driver" : "com.mysql.cj.jdbc.Driver";
    }

    /**
     * Fetch the auto-closeable connection from the hikariDataSource
     *
     * @return The {@link Connection} to the MySQL database
     * @throws SQLException if the connection fails for some reason
     */
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
                throw new IllegalStateException("Failed to create database tables. Please ensure you are running MySQL v8.0+ " +
                        "and that your connecting user account has privileges to create tables.", e);
            }
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Failed to establish a connection to the MySQL database. " +
                    "Please check the supplied database credentials in the config file", e);
        }
    }

    @Blocking
    @Override
    public void ensureUser(@NotNull User user) {
        getUser(user.getUuid()).ifPresentOrElse(
                existingUser -> {
                    if (!existingUser.getName().equals(user.getName())) {
                        // Update a user's name if it has changed in the database
                        try (Connection connection = getConnection()) {
                            try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                                    UPDATE `%users_table%`
                                    SET `username`=?
                                    WHERE `uuid`=?"""))) {

                                statement.setString(1, user.getName());
                                statement.setString(2, existingUser.getUuid().toString());
                                statement.executeUpdate();
                            }
                            plugin.log(Level.INFO, "Updated " + user.getName() + "'s name in the database ("
                                    + existingUser.getName() + " -> " + user.getName() + ")");
                        } catch (SQLException e) {
                            plugin.log(Level.SEVERE, "Failed to update a user's name on the database", e);
                        }
                    }
                },
                () -> {
                    // Insert new player data into the database
                    try (Connection connection = getConnection()) {
                        try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                                INSERT INTO `%users_table%` (`uuid`,`username`)
                                VALUES (?,?);"""))) {

                            statement.setString(1, user.getUuid().toString());
                            statement.setString(2, user.getName());
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
            plugin.log(Level.SEVERE, "Failed to fetch a user from uuid from the database", e);
        }
        return Optional.empty();
    }

    @Blocking
    @Override
    public Optional<User> getUserByName(@NotNull String username) {
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
                    SELECT `uuid`, `username`
                    FROM `%users_table%`;
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
                    SELECT `version_uuid`, `timestamp`, `data`
                    FROM `%user_data_table%`
                    WHERE `player_uuid`=?
                    ORDER BY `timestamp` DESC
                    LIMIT 1;"""))) {
                statement.setString(1, user.getUuid().toString());
                final ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    final UUID versionUuid = UUID.fromString(resultSet.getString("version_uuid"));
                    final OffsetDateTime timestamp = OffsetDateTime.ofInstant(
                            resultSet.getTimestamp("timestamp").toInstant(), TimeZone.getDefault().toZoneId()
                    );
                    final Blob blob = resultSet.getBlob("data");
                    final byte[] dataByteArray = blob.getBytes(1, (int) blob.length());
                    blob.free();
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
                    SELECT `version_uuid`, `timestamp`,  `data`
                    FROM `%user_data_table%`
                    WHERE `player_uuid`=?
                    ORDER BY `timestamp` DESC;"""))) {
                statement.setString(1, user.getUuid().toString());
                final ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    final UUID versionUuid = UUID.fromString(resultSet.getString("version_uuid"));
                    final OffsetDateTime timestamp = OffsetDateTime.ofInstant(
                            resultSet.getTimestamp("timestamp").toInstant(), TimeZone.getDefault().toZoneId()
                    );
                    final Blob blob = resultSet.getBlob("data");
                    final byte[] dataByteArray = blob.getBytes(1, (int) blob.length());
                    blob.free();
                    retrievedData.add(DataSnapshot.deserialize(plugin, dataByteArray, versionUuid, timestamp));
                }
                return retrievedData;
            }
        } catch (SQLException | DataAdapter.AdaptionException e) {
            plugin.log(Level.SEVERE, "Failed to fetch a user's list of snapshots from the database", e);
        }
        return retrievedData;
    }

    @Override
    public int getUnpinnedSnapshotCount(@NotNull User user) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
            SELECT COUNT(`version_uuid`)
            FROM `%user_data_table%`
            WHERE `player_uuid`=? AND `pinned`=false;"""))) {
                statement.setString(1, user.getUuid().toString());
                final ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "Failed to fetch a user's current snapshot count", e);
        }
        return 0;
    }

    @Blocking
    @Override
    public Optional<DataSnapshot.Packed> getSnapshot(@NotNull User user, @NotNull UUID versionUuid) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                    SELECT `version_uuid`, `timestamp`,  `data`
                    FROM `%user_data_table%`
                    WHERE `player_uuid`=? AND `version_uuid`=?
                    ORDER BY `timestamp` DESC
                    LIMIT 1;"""))) {
                statement.setString(1, user.getUuid().toString());
                statement.setString(2, versionUuid.toString());
                final ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    final Blob blob = resultSet.getBlob("data");
                    final OffsetDateTime timestamp = OffsetDateTime.ofInstant(
                            resultSet.getTimestamp("timestamp").toInstant(), TimeZone.getDefault().toZoneId()
                    );
                    final byte[] dataByteArray = blob.getBytes(1, (int) blob.length());
                    blob.free();
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
        final int unpinnedSnapshots = getUnpinnedSnapshotCount(user);
        final int maxSnapshots = plugin.getSettings().getSynchronization().getMaxUserDataSnapshots();
        if (unpinnedSnapshots > maxSnapshots) {
            try (Connection connection = getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                        DELETE FROM `%user_data_table%`
                        WHERE `player_uuid`=?
                        AND `pinned` IS FALSE
                        ORDER BY `timestamp` ASC
                        LIMIT %entry_count%;""".replace("%entry_count%",
                        Integer.toString(unpinnedSnapshots - maxSnapshots))))) {
                    statement.setString(1, user.getUuid().toString());
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
                    DELETE FROM `%user_data_table%`
                    WHERE `player_uuid`=? AND `version_uuid`=?
                    LIMIT 1;"""))) {
                statement.setString(1, user.getUuid().toString());
                statement.setString(2, versionUuid.toString());
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
                    DELETE FROM `%user_data_table%`
                    WHERE `player_uuid`=? AND `timestamp`>? AND `pinned` IS FALSE
                    ORDER BY `timestamp` ASC
                    LIMIT 1;"""))) {
                statement.setString(1, user.getUuid().toString());
                statement.setTimestamp(2, Timestamp.from(within.toInstant()));
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
                    INSERT INTO `%user_data_table%`
                    (`player_uuid`,`version_uuid`,`timestamp`,`save_cause`,`pinned`,`data`)
                    VALUES (?,?,?,?,?,?);"""))) {
                statement.setString(1, user.getUuid().toString());
                statement.setString(2, data.getId().toString());
                statement.setTimestamp(3, Timestamp.from(data.getTimestamp().toInstant()));
                statement.setString(4, data.getSaveCause().name());
                statement.setBoolean(5, data.isPinned());
                statement.setBlob(6, new ByteArrayInputStream(data.asBytes(plugin)));
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
                    UPDATE `%user_data_table%`
                    SET `save_cause`=?,`pinned`=?,`data`=?
                    WHERE `player_uuid`=? AND `version_uuid`=?
                    LIMIT 1;"""))) {
                statement.setString(1, data.getSaveCause().name());
                statement.setBoolean(2, data.isPinned());
                statement.setBlob(3, new ByteArrayInputStream(data.asBytes(plugin)));
                statement.setString(4, user.getUuid().toString());
                statement.setString(5, data.getId().toString());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "Failed to pin user data in the database", e);
        }
    }

    @Blocking
    @Override
    public void saveMapData(@NotNull String serverName, int mapId, byte @NotNull [] data) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                    INSERT INTO `%map_data_table%`
                    (`server_name`,`map_id`,`data`)
                    VALUES (?,?,?);"""))) {
                statement.setString(1, serverName);
                statement.setInt(2, mapId);
                statement.setBlob(3, new ByteArrayInputStream(data));
                statement.executeUpdate();
            }
        } catch (SQLException | DataAdapter.AdaptionException e) {
            plugin.log(Level.SEVERE, "Failed to write map data to the database", e);
        }
    }

    @Blocking
    @Override
    public byte @Nullable [] getMapData(@NotNull String serverName, int mapId) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                    SELECT `data`
                    FROM `%map_data_table%`
                    WHERE `server_name`=? AND `map_id`=?
                    LIMIT 1;"""))) {
                statement.setString(1, serverName);
                statement.setInt(2, mapId);

                final ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    final Blob blob = resultSet.getBlob("data");
                    final byte[] dataByteArray = blob.getBytes(1, (int) blob.length());
                    blob.free();
                    return dataByteArray;
                }
            }
        } catch (SQLException | DataAdapter.AdaptionException e) {
            plugin.log(Level.SEVERE, "Failed to get map data from the database", e);
        }
        return null;
    }

    @Blocking
    @Override
    public @Nullable Map.Entry<String, Integer> getMapBinding(@NotNull String serverName, int mapId) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                    SELECT `from_server_name`, `from_id`
                    FROM `%map_ids_table%`
                    WHERE `to_server_name`=? AND `to_id`=?
                    LIMIT 1;
                    """))) {
                statement.setString(1, serverName);
                statement.setInt(2, mapId);

                final ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    return new AbstractMap.SimpleImmutableEntry<>(
                            resultSet.getString("from_server_name"),
                            resultSet.getInt("from_id")
                    );
                }
            }
        } catch (SQLException | DataAdapter.AdaptionException e) {
            plugin.log(Level.SEVERE, "Failed to get map data from the database", e);
        }
        return null;
    }

    @Blocking
    @Override
    public void setMapBinding(@NotNull String fromServerName, int fromMapId, @NotNull String toServerName, int toMapId) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                    INSERT INTO `%map_ids_table%`
                    (`from_server_name`,`from_id`,`to_server_name`,`to_id`)
                    VALUES (?,?,?,?);"""))) {
                statement.setString(1, fromServerName);
                statement.setInt(2, fromMapId);
                statement.setString(3, toServerName);
                statement.setInt(4, toMapId);
                statement.executeUpdate();
            }
        } catch (SQLException | DataAdapter.AdaptionException e) {
            plugin.log(Level.SEVERE, "Failed to connect map IDs in the database", e);
        }
    }

    @Blocking
    @Override
    public int getBoundMapId(@NotNull String fromServerName, int fromMapId, @NotNull String toServerName) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(formatStatementTables("""
                    SELECT `to_id`
                    FROM `%map_ids_table%`
                    WHERE `from_server_name`=? AND `from_id`=? AND `to_server_name`=?
                    LIMIT 1;"""))) {
                statement.setString(1, fromServerName);
                statement.setInt(2, fromMapId);
                statement.setString(3, toServerName);

                final ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    return resultSet.getInt("to_id");
                }
            }
        } catch (SQLException | DataAdapter.AdaptionException e) {
            plugin.log(Level.SEVERE, "Failed to get new map id from the database", e);
        }
        return -1;
    }

    @Override
    public void wipeDatabase() {
        try (Connection connection = getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(formatStatementTables("DELETE FROM `%user_data_table%`;"));
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
