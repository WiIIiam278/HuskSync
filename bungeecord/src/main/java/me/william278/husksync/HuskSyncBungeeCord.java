package me.william278.husksync;

import me.william278.husksync.bungeecord.command.HuskSyncCommand;
import me.william278.husksync.bungeecord.config.ConfigLoader;
import me.william278.husksync.bungeecord.config.ConfigManager;
import me.william278.husksync.bungeecord.data.DataManager;
import me.william278.husksync.bungeecord.data.sql.Database;
import me.william278.husksync.bungeecord.data.sql.MySQL;
import me.william278.husksync.bungeecord.data.sql.SQLite;
import me.william278.husksync.bungeecord.listener.BungeeEventListener;
import me.william278.husksync.bungeecord.listener.BungeeRedisListener;
import net.md_5.bungee.api.plugin.Plugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

public final class HuskSyncBungeeCord extends Plugin {

    private static HuskSyncBungeeCord instance;
    public static HuskSyncBungeeCord getInstance() {
        return instance;
    }

    private static Database database;
    public static Connection getConnection() throws SQLException {
        return database.getConnection();
    }

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

        // Initialize the database
        database = switch (Settings.dataStorageType) {
            case SQLITE -> new SQLite(this);
            case MYSQL -> new MySQL(this);
        };
        database.load();

        // Setup player data cache
        DataManager.playerDataCache = new DataManager.PlayerDataCache();

        // Register listener
        getProxy().getPluginManager().registerListener(this, new BungeeEventListener());

        // Register command
        getProxy().getPluginManager().registerCommand(this, new HuskSyncCommand());

        // Initialize the redis listener
        new BungeeRedisListener();

        // Log to console
        getLogger().info("Enabled HuskSync (" + getProxy().getName() + ") v" + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

        // Close the database
        database.close();

        // Log to console
        getLogger().info("Disabled HuskSync (" + getProxy().getName() + ") v" + getDescription().getVersion());
    }
}
