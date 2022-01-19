package me.william278.husksync.migrator;

import me.william278.husksync.PlayerData;
import me.william278.husksync.Server;
import me.william278.husksync.Settings;
import me.william278.husksync.proxy.data.DataManager;
import me.william278.husksync.proxy.data.sql.Database;
import me.william278.husksync.proxy.data.sql.MySQL;
import me.william278.husksync.redis.RedisListener;
import me.william278.husksync.redis.RedisMessage;
import me.william278.husksync.util.Logger;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Class to handle migration of data from MySQLPlayerDataBridge
 * <p>
 * The migrator accesses and decodes MPDB's format directly,
 * by communicating with a Spigot server
 */
public class MPDBMigrator {

    public int migratedDataSent = 0;
    public int playersMigrated = 0;

    public HashMap<PlayerData, String> incomingPlayerData;

    public MigrationSettings migrationSettings = new MigrationSettings();
    private Settings.SynchronisationCluster targetCluster;
    private Database sourceDatabase;

    private HashSet<MPDBPlayerData> mpdbPlayerData;

    private final Logger logger;

    public MPDBMigrator(Logger logger) {
        this.logger = logger;
    }

    public boolean readyToMigrate(int networkPlayerCount, HashSet<Server> synchronisedServers) {
        if (networkPlayerCount > 0) {
            logger.log(Level.WARNING, "Failed to start migration because there are players online. " +
                    "Your network has to be empty to migrate data for safety reasons.");
            return false;
        }

        int synchronisedServersWithMpdb = 0;
        for (Server server : synchronisedServers) {
            if (server.hasMySqlPlayerDataBridge()) {
                synchronisedServersWithMpdb++;
            }
        }
        if (synchronisedServersWithMpdb < 1) {
            logger.log(Level.WARNING, "Failed to start migration because at least one Spigot server with both HuskSync and MySqlPlayerDataBridge installed is not online. " +
                    "Please start one Spigot server with HuskSync installed to begin migration.");
            return false;
        }

        for (Settings.SynchronisationCluster cluster : Settings.clusters) {
            if (migrationSettings.targetCluster.equals(cluster.clusterId())) {
                targetCluster = cluster;
                break;
            }
        }
        if (targetCluster == null) {
            logger.log(Level.WARNING, "Failed to start migration because the target cluster could not be found. " +
                    "Please ensure the target cluster is correct, configured in the proxy config file, then try again");
            return false;
        }

        migratedDataSent = 0;
        playersMigrated = 0;
        mpdbPlayerData = new HashSet<>();
        incomingPlayerData = new HashMap<>();
        final MigrationSettings settings = migrationSettings;

        // Get connection to source database
        sourceDatabase = new MigratorMySQL(logger, settings.sourceHost, settings.sourcePort,
                settings.sourceDatabase, settings.sourceUsername, settings.sourcePassword, targetCluster);
        sourceDatabase.load();
        if (sourceDatabase.isInactive()) {
            logger.log(Level.WARNING, "Failed to establish connection to the origin MySQL database. " +
                    "Please check you have input the correct connection details and try again.");
            return false;
        }

        return true;
    }

    // Carry out the migration
    public void executeMigrationOperations(DataManager dataManager, HashSet<Server> synchronisedServers, RedisListener redisListener) {
        // Prepare the target database for insertion
        prepareTargetDatabase(dataManager);

        // Fetch inventory data from MPDB
        getInventoryData();

        // Fetch ender chest data from MPDB
        getEnderChestData();

        // Fetch experience data from MPDB
        getExperienceData();

        // Send the encoded data to the Bukkit servers for conversion
        sendEncodedData(synchronisedServers, redisListener);
    }

    // Clear the new database out of current data
    private void prepareTargetDatabase(DataManager dataManager) {
        logger.log(Level.INFO, "Preparing target database...");
        try (Connection connection = dataManager.getConnection(targetCluster.clusterId())) {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM " + targetCluster.playerTableName() + ";")) {
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM " + targetCluster.dataTableName() + ";")) {
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "An exception occurred preparing the target database", e);
        } finally {
            logger.log(Level.INFO, "Finished preparing target database!");
        }
    }

    private void getInventoryData() {
        logger.log(Level.INFO, "Getting inventory data from MySQLPlayerDataBridge...");
        try (Connection connection = sourceDatabase.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + migrationSettings.inventoryDataTable + ";")) {
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    final UUID playerUUID = UUID.fromString(resultSet.getString("player_uuid"));
                    final String playerName = resultSet.getString("player_name");

                    MPDBPlayerData data = new MPDBPlayerData(playerUUID, playerName);
                    data.inventoryData = resultSet.getString("inventory");
                    data.armorData = resultSet.getString("armor");

                    mpdbPlayerData.add(data);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "An exception occurred getting inventory data", e);
        } finally {
            logger.log(Level.INFO, "Finished getting inventory data from MySQLPlayerDataBridge");
        }
    }

    private void getEnderChestData() {
        logger.log(Level.INFO, "Getting ender chest data from MySQLPlayerDataBridge...");
        try (Connection connection = sourceDatabase.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + migrationSettings.enderChestDataTable + ";")) {
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    final UUID playerUUID = UUID.fromString(resultSet.getString("player_uuid"));

                    for (MPDBPlayerData data : mpdbPlayerData) {
                        if (data.playerUUID.equals(playerUUID)) {
                            data.enderChestData = resultSet.getString("enderchest");
                            break;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "An exception occurred getting ender chest data", e);
        } finally {
            logger.log(Level.INFO, "Finished getting ender chest data from MySQLPlayerDataBridge");
        }
    }

    private void getExperienceData() {
        logger.log(Level.INFO, "Getting experience data from MySQLPlayerDataBridge...");
        try (Connection connection = sourceDatabase.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + migrationSettings.expDataTable + ";")) {
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    final UUID playerUUID = UUID.fromString(resultSet.getString("player_uuid"));

                    for (MPDBPlayerData data : mpdbPlayerData) {
                        if (data.playerUUID.equals(playerUUID)) {
                            data.expLevel = resultSet.getInt("exp_lvl");
                            data.expProgress = resultSet.getFloat("exp");
                            data.totalExperience = resultSet.getInt("total_exp");
                            break;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "An exception occurred getting experience data", e);
        } finally {
            logger.log(Level.INFO, "Finished getting experience data from MySQLPlayerDataBridge");
        }
    }

    private void sendEncodedData(HashSet<Server> synchronisedServers, RedisListener redisListener) {
        for (Server processingServer : synchronisedServers) {
            if (processingServer.hasMySqlPlayerDataBridge()) {
                for (MPDBPlayerData data : mpdbPlayerData) {
                    try {
                        new RedisMessage(RedisMessage.MessageType.DECODE_MPDB_DATA,
                                new RedisMessage.MessageTarget(Settings.ServerType.BUKKIT, null, null),
                                processingServer.serverUUID().toString(),
                                RedisMessage.serialize(data))
                                .send();
                        migratedDataSent++;
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Failed to serialize encoded MPDB data", e);
                    }
                }
                logger.log(Level.INFO, "Finished dispatching encoded data for " + migratedDataSent + " players; please wait for conversion to finish");
            }
            return;
        }
    }

    /**
     * Loads all incoming decoded MPDB data to the cache and database
     *
     * @param dataToLoad HashMap of the {@link PlayerData} to player Usernames that will be loaded
     */
    public void loadIncomingData(HashMap<PlayerData, String> dataToLoad, DataManager dataManager) {
        int playersSaved = 0;
        logger.log(Level.INFO, "Saving data for " + playersMigrated + " players...");

        for (PlayerData playerData : dataToLoad.keySet()) {
            String playerName = dataToLoad.get(playerData);

            // Add the player to the MySQL table
            dataManager.ensurePlayerExists(playerData.getPlayerUUID(), playerName);

            // Update the data in the cache and SQL
            for (Settings.SynchronisationCluster cluster : Settings.clusters) {
                dataManager.updatePlayerData(playerData, cluster);
                break;
            }

            playersSaved++;
            logger.log(Level.INFO, "Saved data for " + playersSaved + "/" + playersMigrated + " players");
        }

        // Mark as done when done
        logger.log(Level.INFO, """
                === MySQLPlayerDataBridge Migration Wizard ==========
                                                
                Migration complete!
                                                
                Successfully migrated data for %1%/%2% players.
                                                
                You should now uninstall MySQLPlayerDataBridge from
                the rest of the Spigot servers, then restart them.
                """.replaceAll("%1%", Integer.toString(playersMigrated))
                .replaceAll("%2%", Integer.toString(migratedDataSent)));
        sourceDatabase.close(); // Close source database
    }

    /**
     * Class used to hold settings for the MPDB migration
     */
    public static class MigrationSettings {
        public String sourceHost;
        public int sourcePort;
        public String sourceDatabase;
        public String sourceUsername;
        public String sourcePassword;

        public String inventoryDataTable;
        public String enderChestDataTable;
        public String expDataTable;

        public String targetCluster;

        public MigrationSettings() {
            sourceHost = "localhost";
            sourcePort = 3306;
            sourceDatabase = "mpdb";
            sourceUsername = "root";
            sourcePassword = "pa55w0rd";

            targetCluster = "main";

            inventoryDataTable = "mpdb_inventory";
            enderChestDataTable = "mpdb_enderchest";
            expDataTable = "mpdb_experience";
        }
    }

    /**
     * MySQL class used for importing data from MPDB
     */
    public static class MigratorMySQL extends MySQL {
        public MigratorMySQL(Logger logger, String host, int port, String database, String username, String password, Settings.SynchronisationCluster cluster) {
            super(cluster, logger);
            super.host = host;
            super.port = port;
            super.database = database;
            super.username = username;
            super.password = password;
            super.params = "?useSSL=false";
            super.dataPoolName = super.dataPoolName + "Migrator";
        }
    }

}
