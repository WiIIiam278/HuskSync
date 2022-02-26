package me.william278.husksync.bungeecord.config;

import me.william278.husksync.HuskSyncBungeeCord;
import me.william278.husksync.Settings;
import me.william278.husksync.util.MessageManager;
import net.md_5.bungee.config.Configuration;

import java.util.HashMap;

public class ConfigLoader {

    private static final HuskSyncBungeeCord plugin = HuskSyncBungeeCord.getInstance();

    private static Configuration copyDefaults(Configuration config) {
        // Get the config version and update if needed
        String configVersion = config.getString("config_file_version", "1.0");
        if (configVersion.contains("-dev")) {
            configVersion = configVersion.replaceAll("-dev", "");
        }
        if (!configVersion.equals(plugin.getDescription().getVersion())) {
            if (configVersion.equalsIgnoreCase("1.0")) {
                config.set("check_for_updates", true);
            }
            if (configVersion.equalsIgnoreCase("1.0") || configVersion.equalsIgnoreCase("1.0.1") || configVersion.equalsIgnoreCase("1.0.2") || configVersion.equalsIgnoreCase("1.0.3")) {
                config.set("clusters.main.player_table", "husksync_players");
                config.set("clusters.main.data_table", "husksync_data");
            }
            config.set("config_file_version", plugin.getDescription().getVersion());
        }
        // Save the config back
        ConfigManager.saveConfig(config);
        return config;
    }

    public static void loadSettings(Configuration loadedConfig) throws IllegalArgumentException {
        Configuration config = copyDefaults(loadedConfig);

        Settings.language = config.getString("language", "en-gb");

        Settings.serverType = Settings.ServerType.PROXY;
        Settings.automaticUpdateChecks = config.getBoolean("check_for_updates", true);
        Settings.redisHost = config.getString("redis_settings.host", "localhost");
        Settings.redisPort = config.getInt("redis_settings.port", 6379);
        Settings.redisPassword = config.getString("redis_settings.password", "");
        Settings.redisSSL = config.getBoolean("redis_settings.use_ssl", false);

        Settings.dataStorageType = Settings.DataStorageType.valueOf(config.getString("data_storage_settings.database_type", "sqlite").toUpperCase());
        if (Settings.dataStorageType == Settings.DataStorageType.MYSQL) {
            Settings.mySQLHost = config.getString("data_storage_settings.mysql_settings.host", "localhost");
            Settings.mySQLPort = config.getInt("data_storage_settings.mysql_settings.port", 3306);
            Settings.mySQLDatabase = config.getString("data_storage_settings.mysql_settings.database", "HuskSync");
            Settings.mySQLUsername = config.getString("data_storage_settings.mysql_settings.username", "root");
            Settings.mySQLPassword = config.getString("data_storage_settings.mysql_settings.password", "pa55w0rd");
            Settings.mySQLParams = config.getString("data_storage_settings.mysql_settings.params", "?autoReconnect=true&useSSL=false");
        }

        Settings.hikariMaximumPoolSize = config.getInt("data_storage_settings.hikari_pool_settings.maximum_pool_size", 10);
        Settings.hikariMinimumIdle = config.getInt("data_storage_settings.hikari_pool_settings.minimum_idle", 10);
        Settings.hikariMaximumLifetime = config.getLong("data_storage_settings.hikari_pool_settings.maximum_lifetime", 1800000);
        Settings.hikariKeepAliveTime = config.getLong("data_storage_settings.hikari_pool_settings.keepalive_time", 0);
        Settings.hikariConnectionTimeOut = config.getLong("data_storage_settings.hikari_pool_settings.connection_timeout", 5000);

        Settings.bounceBackSynchronisation = config.getBoolean("bounce_back_synchronization", true);

        // Read cluster data
        Configuration section = config.getSection("clusters");
        final String settingDatabaseName = Settings.mySQLDatabase != null ? Settings.mySQLDatabase : "HuskSync";
        for (String clusterId : section.getKeys()) {
            final String playerTableName = config.getString("clusters." + clusterId + ".player_table", "husksync_players");
            final String dataTableName = config.getString("clusters." + clusterId + ".data_table", "husksync_data");
            final String databaseName = config.getString("clusters." + clusterId + ".database", settingDatabaseName);
            Settings.clusters.add(new Settings.SynchronisationCluster(clusterId, databaseName, playerTableName, dataTableName));
        }
    }

    public static void loadMessageStrings(Configuration config) {
        final HashMap<String,String> messages = new HashMap<>();
        for (String messageId : config.getKeys()) {
            messages.put(messageId, config.getString(messageId));
        }
        MessageManager.setMessages(messages);
    }

}
