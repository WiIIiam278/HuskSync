package me.william278.crossserversync.bungeecord.config;

import me.william278.crossserversync.Settings;
import net.md_5.bungee.config.Configuration;

public class ConfigLoader {

    public static void loadSettings(Configuration config) throws IllegalArgumentException {
        Settings.serverType = Settings.ServerType.BUNGEECORD;
        Settings.redisHost = config.getString("redis_settings.host", "localhost");
        Settings.redisPort = config.getInt("redis_settings.port", 6379);
        Settings.redisPassword = config.getString("redis_settings.password", "");

        Settings.dataStorageType = Settings.DataStorageType.valueOf(config.getString("data_storage_settings.database_type", "sqlite").toUpperCase());
        if (Settings.dataStorageType == Settings.DataStorageType.MYSQL) {
            Settings.mySQLHost = config.getString("data_storage_settings.mysql_settings.host", "localhost");
            Settings.mySQLPort = config.getInt("data_storage_settings.mysql_settings.port", 8123);
            Settings.mySQLDatabase = config.getString("data_storage_settings.mysql_settings.database", "CrossServerSync");
            Settings.mySQLUsername = config.getString("data_storage_settings.mysql_settings.username", "CrossServerSync");
            Settings.mySQLPassword = config.getString("data_storage_settings.mysql_settings.password", "CrossServerSync");
            Settings.mySQLParams = config.getString("data_storage_settings.mysql_settings.params", "CrossServerSync");
        }

        Settings.hikariMaximumPoolSize = config.getInt("data_storage_settings.hikari_pool_settings.maximum_pool_size", 10);
        Settings.hikariMinimumIdle = config.getInt("data_storage_settings.hikari_pool_settings.minimum_idle", 10);
        Settings.hikariMaximumLifetime = config.getLong("data_storage_settings.hikari_pool_settings.maximum_lifetime", 1800000);
        Settings.hikariKeepAliveTime = config.getLong("data_storage_settings.hikari_pool_settings.keepalive_time", 10);
        Settings.hikariConnectionTimeOut = config.getLong("data_storage_settings.hikari_pool_settings.connection_timeout", 5000);

    }

}
