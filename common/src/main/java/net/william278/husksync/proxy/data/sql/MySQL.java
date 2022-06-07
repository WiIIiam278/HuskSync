package net.william278.husksync.proxy.data.sql;

import com.zaxxer.hikari.HikariDataSource;
import net.william278.husksync.Settings;
import net.william278.husksync.util.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class MySQL extends Database {

    final String[] SQL_SETUP_STATEMENTS = {
            "CREATE TABLE IF NOT EXISTS " + cluster.playerTableName() + " (" +
                    "`id` integer NOT NULL AUTO_INCREMENT," +
                    "`uuid` char(36) NOT NULL UNIQUE," +
                    "`username` varchar(16) NOT NULL," +

                    "PRIMARY KEY (`id`)" +
                    ");",

            "CREATE TABLE IF NOT EXISTS " + cluster.dataTableName() + " (" +
                    "`player_id` integer NOT NULL," +
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

                    "PRIMARY KEY (`player_id`,`version_uuid`)," +
                    "FOREIGN KEY (`player_id`) REFERENCES " + cluster.playerTableName() + " (`id`)" +
                    ");"

    };

    public String host = Settings.mySQLHost;
    public int port = Settings.mySQLPort;
    public String database = Settings.mySQLDatabase;
    public String username = Settings.mySQLUsername;
    public String password = Settings.mySQLPassword;
    public String params = Settings.mySQLParams;

    private HikariDataSource dataSource;

    public MySQL(Settings.SynchronisationCluster cluster, Logger logger) {
        super(cluster, logger);
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

        // Set data source driver path
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

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
            logger.log(Level.SEVERE, "An error occurred creating tables on the MySQL database: ", e);
        }
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

}
