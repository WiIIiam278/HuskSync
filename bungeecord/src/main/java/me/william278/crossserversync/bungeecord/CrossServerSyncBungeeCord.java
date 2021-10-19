package me.william278.crossserversync.bungeecord;

import me.william278.crossserversync.bungeecord.config.ConfigLoader;
import me.william278.crossserversync.bungeecord.config.ConfigManager;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.Objects;

public final class CrossServerSyncBungeeCord extends Plugin {

    private static CrossServerSyncBungeeCord instance;
    public static CrossServerSyncBungeeCord getInstance() {
        return instance;
    }

    public PlayerDataCache cache;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic

        // Load config
        ConfigManager.loadConfig();

        // Load settings from config
        ConfigLoader.loadSettings(Objects.requireNonNull(ConfigManager.getConfig()));

        // Setup player data cache
        cache = new PlayerDataCache();

        // Initialize the redis listener
        new BungeeRedisListener();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
