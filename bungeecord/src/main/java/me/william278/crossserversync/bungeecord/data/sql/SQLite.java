package me.william278.crossserversync.bungeecord.data.sql;

import com.zaxxer.hikari.HikariDataSource;
import me.william278.crossserversync.CrossServerSyncBungeeCord;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.logging.Level;

public class SQLite extends Database {

    final static String[] SQL_SETUP_STATEMENTS = {
            "PRAGMA foreign_keys = ON;",
            "PRAGMA encoding = 'UTF-8';",

            "CREATE TABLE IF NOT EXISTS " + PLAYER_TABLE_NAME + " (" +
                    "`id` integer PRIMARY KEY," +
                    "`uuid` char(36) NOT NULL UNIQUE" +
                    ");",

            "CREATE TABLE IF NOT EXISTS " + DATA_TABLE_NAME + " (" +
                    "`player_id` integer NOT NULL REFERENCES " + PLAYER_TABLE_NAME + "(`id`)," +
                    "`version_uuid` char(36) NOT NULL UNIQUE," +
                    "`timestamp` datetime NOT NULL," +
                    "`inventory` longtext NOT NULL," +
                    "`ender_chest` longtext NOT NULL," +
                    "`health` double NOT NULL," +
                    "`max_health` double NOT NULL," +
                    "`hunger` integer NOT NULL," +
                    "`saturation` float NOT NULL," +
                    "`selected_slot` integer NOT NULL," +
                    "`status_effects` longtext NOT NULL," +

                    "PRIMARY KEY (`player_id`,`version_uuid`)" +
                    ");"
    };

    private static final String DATABASE_NAME = "CrossServerSyncData";

    private HikariDataSource dataSource;

    public SQLite(CrossServerSyncBungeeCord instance) {
        super(instance);
    }

    // Create the database file if it does not exist yet
    private void createDatabaseFileIfNotExist() {
        File databaseFile = new File(plugin.getDataFolder(), DATABASE_NAME + ".db");
        if (!databaseFile.exists()) {
            try {
                if (!databaseFile.createNewFile()) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to write new file: " + DATABASE_NAME + ".db (file already exists)");
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "An error occurred writing a file: " + DATABASE_NAME + ".db (" + e.getCause() + ")");
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
        final String jdbcUrl = "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + File.separator + DATABASE_NAME + ".db";
        dataSource = new HikariDataSource();
        dataSource.setDataSourceClassName("org.sqlite.SQLiteDataSource");
        dataSource.addDataSourceProperty("url", jdbcUrl);

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
            plugin.getLogger().log(Level.SEVERE, "An error occurred creating tables on the SQLite database: ", e);
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
        final String BACKUPS_FOLDER_NAME = "database-backups";
        final String backupFileName = DATABASE_NAME + "Backup_" + DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SS")
                .withLocale(Locale.getDefault())
                .withZone(ZoneId.systemDefault())
                .format(Instant.now()).replaceAll(" ", "-") + ".db";
        final File databaseFile = new File(plugin.getDataFolder(), DATABASE_NAME + ".db");
        if (new File(plugin.getDataFolder(), BACKUPS_FOLDER_NAME).mkdirs()) {
            plugin.getLogger().info("Created backups directory in CrossServerSync plugin data folder.");
        }
        final File backUpFile = new File(plugin.getDataFolder(), BACKUPS_FOLDER_NAME + File.separator + backupFileName);
        try {
            Files.copy(databaseFile.toPath(), backUpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Created a backup of your database.");
        } catch (IOException iox) {
            plugin.getLogger().log(Level.WARNING, "An error occurred making a database backup", iox);
        }
    }
}
