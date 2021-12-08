package me.william278.husksync.velocity.config;

import me.william278.husksync.HuskSyncVelocity;
import me.william278.husksync.Settings;
import me.william278.husksync.util.MessageManager;
import ninja.leaping.configurate.ConfigurationNode;

import java.util.HashMap;

public class ConfigLoader {

    private static ConfigurationNode copyDefaults(ConfigurationNode configRoot) {
        // Get the config version and update if needed
        String configVersion = getConfigString(configRoot, "1.0", "config_file_version");
        if (configVersion.contains("-dev")) {
            configVersion = configVersion.replaceAll("-dev", "");
        }
        if (!configVersion.equals(HuskSyncVelocity.VERSION)) {
            if (configVersion.equalsIgnoreCase("1.0")) {
                configRoot.getNode("check_for_updates").setValue(true);
            }
            if (configVersion.equalsIgnoreCase("1.0") || configVersion.equalsIgnoreCase("1.0.1") || configVersion.equalsIgnoreCase("1.0.2") || configVersion.equalsIgnoreCase("1.0.3")) {
                configRoot.getNode("clusters.main.player_table").setValue("husksync_players");
                configRoot.getNode("clusters.main.data_table").setValue("husksync_data");
            }
            configRoot.getNode("config_file_version").setValue(HuskSyncVelocity.VERSION);
        }
        // Save the config back
        ConfigManager.saveConfig(configRoot);
        return configRoot;
    }

    private static String getConfigString(ConfigurationNode rootNode, String defaultValue, String... nodePath) {
        return !rootNode.getNode(nodePath).isVirtual() ? rootNode.getNode(nodePath).getString() : defaultValue;
    }

    private static boolean getConfigBoolean(ConfigurationNode rootNode, boolean defaultValue, String... nodePath) {
        return !rootNode.getNode(nodePath).isVirtual() ? rootNode.getNode(nodePath).getBoolean() : defaultValue;
    }

    private static int getConfigInt(ConfigurationNode rootNode, int defaultValue, String... nodePath) {
        return !rootNode.getNode(nodePath).isVirtual() ? rootNode.getNode(nodePath).getInt() : defaultValue;
    }

    private static long getConfigLong(ConfigurationNode rootNode, long defaultValue, String... nodePath) {
        return !rootNode.getNode(nodePath).isVirtual() ? rootNode.getNode(nodePath).getLong() : defaultValue;
    }

    public static void loadSettings(ConfigurationNode loadedConfig) throws IllegalArgumentException {
        ConfigurationNode config = copyDefaults(loadedConfig);

        Settings.language = getConfigString(config, "en-gb", "language");

        Settings.serverType = Settings.ServerType.PROXY;
        Settings.automaticUpdateChecks = getConfigBoolean(config, true, "check_for_updates");
        Settings.redisHost = getConfigString(config, "localhost", "redis_settings", "host");
        Settings.redisPort = getConfigInt(config, 6379, "redis_settings", "port");
        Settings.redisPassword = getConfigString(config, "", "redis_settings", "password");

        Settings.dataStorageType = Settings.DataStorageType.valueOf(getConfigString(config, "sqlite", "data_storage_settings", "database_type").toUpperCase());
        if (Settings.dataStorageType == Settings.DataStorageType.MYSQL) {
            Settings.mySQLHost = getConfigString(config, "localhost", "data_storage_settings", "mysql_settings", "host");
            Settings.mySQLPort = getConfigInt(config, 3306, "data_storage_settings", "mysql_settings", "port");
            Settings.mySQLDatabase = getConfigString(config, "HuskSync", "data_storage_settings", "mysql_settings", "database");
            Settings.mySQLUsername = getConfigString(config, "root", "data_storage_settings", "mysql_settings", "username");
            Settings.mySQLPassword = getConfigString(config, "pa55w0rd", "data_storage_settings", "mysql_settings", "password");
            Settings.mySQLParams = getConfigString(config, "?autoReconnect=true&useSSL=false", "data_storage_settings", "mysql_settings", "params");
        }

        Settings.hikariMaximumPoolSize = getConfigInt(config, 10, "data_storage_settings", "hikari_pool_settings", "maximum_pool_size");
        Settings.hikariMinimumIdle = getConfigInt(config, 10, "data_storage_settings", "hikari_pool_settings", "minimum_idle");
        Settings.hikariMaximumLifetime = getConfigLong(config, 1800000, "data_storage_settings", "hikari_pool_settings", "maximum_lifetime");
        Settings.hikariKeepAliveTime = getConfigLong(config, 0, "data_storage_settings", "hikari_pool_settings", "keepalive_time");
        Settings.hikariConnectionTimeOut = getConfigLong(config, 5000, "data_storage_settings", "hikari_pool_settings", "connection_timeout");

        // Read cluster data
        ConfigurationNode clusterSection = config.getNode("clusters");
        final String settingDatabaseName = Settings.mySQLDatabase != null ? Settings.mySQLDatabase : "HuskSync";
        for (ConfigurationNode cluster : clusterSection.getChildrenList()) {
            final String clusterId = (String) cluster.getKey();
            final String playerTableName = getConfigString(config, "husksync_players", "clusters", clusterId, "player_table");
            final String dataTableName = getConfigString(config, "husksync_data", "clusters", clusterId, "data_table");
            final String databaseName = getConfigString(config, settingDatabaseName, "clusters", clusterId, "database");
            Settings.clusters.add(new Settings.SynchronisationCluster(clusterId, databaseName, playerTableName, dataTableName));
        }
    }

    public static void loadMessageStrings(ConfigurationNode config) {
        final HashMap<String, String> messages = new HashMap<>();
        for (ConfigurationNode message : config.getChildrenList()) {
            final String messageId = (String) message.getKey();
            messages.put(messageId, getConfigString(config, "", messageId));
        }
        MessageManager.setMessages(messages);
    }

}
