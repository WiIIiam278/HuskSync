package me.william278.husksync;

import me.william278.husksync.bukkit.config.ConfigLoader;
import me.william278.husksync.bukkit.data.BukkitDataCache;
import me.william278.husksync.bukkit.listener.BukkitRedisListener;
import me.william278.husksync.bukkit.listener.EventListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class HuskSyncBukkit extends JavaPlugin {

    private static HuskSyncBukkit instance;
    public static HuskSyncBukkit getInstance() {
        return instance;
    }

    public static BukkitDataCache bukkitCache;

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

        // Initialize last data update UUID cache
        bukkitCache = new BukkitDataCache();

        // Initialize event listener
        getServer().getPluginManager().registerEvents(new EventListener(), this);

        // Initialize the redis listener
        new BukkitRedisListener();

        // Log to console
        getLogger().info("Enabled HuskSync (" + getServer().getName() + ") v" + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Disabled HuskSync (" + getServer().getName() + ") v" + getDescription().getVersion());
    }
}
