package me.william278.husksync.bungeecord.data.sql;

import com.zaxxer.hikari.HikariDataSource;
import me.william278.husksync.HuskSyncBungeeCord;
import me.william278.husksync.Settings;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class MySQL extends Database {

    final static String[] SQL_SETUP_STATEMENTS = {
            "CREATE TABLE IF NOT EXISTS " + PLAYER_TABLE_NAME + " (" +
                    "`id` integer NOT NULL AUTO_INCREMENT," +
                    "`uuid` char(36) NOT NULL UNIQUE," +

                    "PRIMARY KEY (`id`)" +
                    ");",

            "CREATE TABLE IF NOT EXISTS " + DATA_TABLE_NAME + " (" +
                    "`player_id` integer NOT NULL," +
                    "`version_uuid` char(36) NOT NULL UNIQUE," +
                    "`timestamp` datetime NOT NULL," +
                    "`inventory` longtext NOT NULL," +
                    "`ender_chest` longtext NOT NULL," +
                    "`health` double NOT NULL," +
                    "`max_health` double NOT NULL," +
                    "`hunger` integer NOT NULL," +
                    "`saturation` float NOT NULL," +
                    "`saturation_exhaustion` float NOT NULL," +
                    "`selected_slot` integer NOT NULL," +
                    "`status_effects` longtext NOT NULL," +
                    "`total_experience` integer NOT NULL," +
                    "`exp_level` integer NOT NULL," +
                    "`exp_progress` float NOT NULL," +
                    "`game_mode` tinytext NOT NULL," +
                    "`statistics` longtext NOT NULL," +
                    "`is_flying` boolean NOT NULL," +
                    "`advancements` longtext NOT NULL," +
                    "`location` text NOT NULL," +

                    "PRIMARY KEY (`player_id`,`uuid`)," +
                    "FOREIGN KEY (`player_id`) REFERENCES " + PLAYER_TABLE_NAME + " (`id`)" +
                    ");"

    };

    final String host = me.william278.husksync.Settings.mySQLHost;
    final int port = me.william278.husksync.Settings.mySQLPort;
    final String database = me.william278.husksync.Settings.mySQLDatabase;
    final String username = me.william278.husksync.Settings.mySQLUsername;
    final String password = me.william278.husksync.Settings.mySQLPassword;
    final String params = Settings.mySQLParams;

    private HikariDataSource dataSource;

    public MySQL(HuskSyncBungeeCord instance) {
        super(instance);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void load() {
        // Create new HikariCP data source
        final String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + params;
        dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(jdbcUrl);

        dataSource.setUsername(username);
        dataSource.setPassword(password);

        // Set various additional parameters
        dataSource.setMaximumPoolSize(hikariMaximumPoolSize);
        dataSource.setMinimumIdle(hikariMinimumIdle);
        dataSource.setMaxLifetime(hikariMaximumLifetime);
        dataSource.setKeepaliveTime(hikariKeepAliveTime);
        dataSource.setConnectionTimeout(hikariConnectionTimeOut);
        dataSource.setPoolName(DATA_POOL_NAME);

        // Create tables
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                for (String tableCreationStatement : SQL_SETUP_STATEMENTS) {
                    statement.execute(tableCreationStatement);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "An error occurred creating tables on the MySQL database: ", e);
        }
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Override
    public void backup() {
        plugin.getLogger().info("Remember to make backups of your HuskHomes Database before updating the plugin!");
    }
}
