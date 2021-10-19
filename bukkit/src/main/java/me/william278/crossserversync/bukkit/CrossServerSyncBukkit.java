package me.william278.crossserversync.bukkit;

import me.william278.crossserversync.bukkit.config.ConfigLoader;
import org.bukkit.plugin.java.JavaPlugin;

public final class CrossServerSyncBukkit extends JavaPlugin {

    private static CrossServerSyncBukkit instance;
    public static CrossServerSyncBukkit getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic

        // Load the config file
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        saveConfig();
        reloadConfig();
        ConfigLoader.loadSettings(getConfig());

        // Initialize the redis listener
        new BukkitRedisListener();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
