package me.william278.husksync.bungeecord.data.sql;

import com.zaxxer.hikari.HikariDataSource;
import me.william278.husksync.HuskSyncBungeeCord;
import me.william278.husksync.Settings;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class SQLite extends Database {

    final String[] SQL_SETUP_STATEMENTS = {
            "PRAGMA foreign_keys = ON;",
            "PRAGMA encoding = 'UTF-8';",

            "CREATE TABLE IF NOT EXISTS " + cluster.playerTableName() + " (" +
                    "`id` integer PRIMARY KEY," +
                    "`uuid` char(36) NOT NULL UNIQUE," +
                    "`username` varchar(16) NOT NULL" +
                    ");",

            "CREATE TABLE IF NOT EXISTS " + cluster.dataTableName() + " (" +
                    "`player_id` integer NOT NULL REFERENCES " + cluster.playerTableName() + "(`id`)," +
                    "`version_uuid` char(36) NOT NULL UNIQUE," +
                    "`timestamp` datetime NOT NULL," +
                    "`inventory` longtext NOT NULL," +
                    "`ender_chest` longtext NOT NULL," +
                    "`health` double NOT NULL," +
                    "`max_health` double NOT NULL," +
                    "`health_scale` double NOT NULL," +
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

                    "PRIMARY KEY (`player_id`,`version_uuid`)" +
                    ");"
    };

    private String getDatabaseName() {
        return cluster.databaseName() + "Data";
    }

    private HikariDataSource dataSource;

    public SQLite(HuskSyncBungeeCord instance, Settings.SynchronisationCluster cluster) {
        super(instance, cluster);
    }

    // Create the database file if it does not exist yet
    private void createDatabaseFileIfNotExist() {
        File databaseFile = new File(plugin.getDataFolder(), getDatabaseName() + ".db");
        if (!databaseFile.exists()) {
            try {
                if (!databaseFile.createNewFile()) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to write new file: " + getDatabaseName() + ".db (file already exists)");
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "An error occurred writing a file: " + getDatabaseName() + ".db (" + e.getCause() + ")");
            }
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void load() {
        // Make SQLite database file
        createDatabaseFileIfNotExist();

        // Create new HikariCP data source
        final String jdbcUrl = "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + File.separator + getDatabaseName() + ".db";
        dataSource = new HikariDataSource();
        dataSource.setDataSourceClassName("org.sqlite.SQLiteDataSource");
        dataSource.addDataSourceProperty("url", jdbcUrl);

        // Set various additional parameters
        dataSource.setMaximumPoolSize(hikariMaximumPoolSize);
        dataSource.setMinimumIdle(hikariMinimumIdle);
        dataSource.setMaxLifetime(hikariMaximumLifetime);
        dataSource.setKeepaliveTime(hikariKeepAliveTime);
        dataSource.setConnectionTimeout(hikariConnectionTimeOut);
        dataSource.setPoolName(dataPoolName);
    }

    @Override
    public void createTables() {
        // Create tables
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                for (String tableCreationStatement : SQL_SETUP_STATEMENTS) {
                    statement.execute(tableCreationStatement);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "An error occurred creating tables on the SQLite database: ", e);
        }
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

}
