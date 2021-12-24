package me.william278.husksync.bukkit.config;

import me.william278.husksync.Settings;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigLoader {

    public static void loadSettings(FileConfiguration config) throws IllegalArgumentException {
        Settings.serverType = Settings.ServerType.BUKKIT;
        Settings.automaticUpdateChecks = config.getBoolean("check_for_updates", true);
        Settings.cluster = config.getString("cluster_id", "main");
        Settings.redisHost = config.getString("redis_settings.host", "localhost");
        Settings.redisPort = config.getInt("redis_settings.port", 6379);
        Settings.redisPassword = config.getString("redis_settings.password", "");

        Settings.syncInventories = config.getBoolean("synchronisation_settings.inventories", true);
        Settings.syncEnderChests = config.getBoolean("synchronisation_settings.ender_chests", true);
        Settings.syncHealth = config.getBoolean("synchronisation_settings.health", true);
        Settings.syncHunger = config.getBoolean("synchronisation_settings.hunger", true);
        Settings.syncExperience = config.getBoolean("synchronisation_settings.experience", true);
        Settings.syncPotionEffects = config.getBoolean("synchronisation_settings.potion_effects", true);
        Settings.syncStatistics = config.getBoolean("synchronisation_settings.statistics", true);
        Settings.syncGameMode = config.getBoolean("synchronisation_settings.game_mode", true);
        Settings.syncAdvancements = config.getBoolean("synchronisation_settings.advancements", true);
        Settings.syncLocation = config.getBoolean("synchronisation_settings.location", false);
        Settings.syncFlight = config.getBoolean("synchronisation_settings.flight", false);

        // Future
        Settings.useNativeImplementation = config.getBoolean("native_advancement_synchronization", false);
    }

}
