package me.william278.crossserversync.bukkit.config;

import me.william278.crossserversync.Settings;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigLoader {

    public static void loadSettings(FileConfiguration config) throws IllegalArgumentException {
        Settings.serverType = Settings.ServerType.BUKKIT;
        Settings.redisHost = config.getString("redis_settings.host", "localhost");
        Settings.redisPort = config.getInt("redis_settings.port", 6379);
        Settings.redisPassword = config.getString("redis_settings.password", "");
    }

}
