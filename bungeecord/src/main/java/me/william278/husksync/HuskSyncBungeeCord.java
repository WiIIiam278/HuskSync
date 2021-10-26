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
import me.william278.husksync.bungeecord.migrator.MPDBMigrator;
import me.william278.husksync.bungeecord.util.BungeeUpdateChecker;
import me.william278.husksync.redis.RedisMessage;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import org.bstats.bungeecord.Metrics;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

public final class HuskSyncBungeeCord extends Plugin {

    private static final int METRICS_ID = 13141;

    private static HuskSyncBungeeCord instance;
    public static HuskSyncBungeeCord getInstance() {
        return instance;
    }

    // Whether the plugin is ready to accept redis messages
    public static boolean readyForRedis = false;

    /**
     Set of all the {@link Server}s that have completed the synchronisation handshake with HuskSync on the proxy
     */
    public static HashSet<Server> synchronisedServers;

    private static Database database;
    public static Connection getConnection() throws SQLException {
        return database.getConnection();
    }

    public static MPDBMigrator mpdbMigrator;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        synchronisedServers = new HashSet<>();

        // Load config
        ConfigManager.loadConfig();

        // Load settings from config
        ConfigLoader.loadSettings(Objects.requireNonNull(ConfigManager.getConfig()));

        // Load messages
        ConfigManager.loadMessages();

        // Load locales from messages
        ConfigLoader.loadMessageStrings(Objects.requireNonNull(ConfigManager.getMessages()));

        // Do update checker
        if (Settings.automaticUpdateChecks) {
            new BungeeUpdateChecker(getDescription().getVersion()).logToConsole();
        }

        // Initialize the database
        database = switch (Settings.dataStorageType) {
            case SQLITE -> new SQLite(this);
            case MYSQL -> new MySQL(this);
        };
        database.load();
        database.createTables();

        // Abort loading if the database failed to initialize
        if (database.isInactive()) {
            getLogger().severe("Failed to initialize the database; HuskSync will now abort loading itself (" + getProxy().getName() + ") v" + getDescription().getVersion());
            return;
        }

        // Setup player data cache
        DataManager.playerDataCache = new DataManager.PlayerDataCache();

        // Initialize the redis listener
        if (!new BungeeRedisListener().isActiveAndEnabled) {
            getLogger().severe("Failed to initialize Redis; HuskSync will now abort loading itself (" + getProxy().getName() + ") v" + getDescription().getVersion());
            return;
        }

        // Register listener
        getProxy().getPluginManager().registerListener(this, new BungeeEventListener());

        // Register command
        getProxy().getPluginManager().registerCommand(this, new HuskSyncCommand());

        // Prepare the migrator for use if needed
        mpdbMigrator = new MPDBMigrator();

        // Initialize bStats metrics
        try {
            new Metrics(this, METRICS_ID);
        } catch (Exception e) {
            getLogger().info("Skipped metrics initialization");
        }

        // Log to console
        getLogger().info("Enabled HuskSync (" + getProxy().getName() + ") v" + getDescription().getVersion());

        // Mark as ready for redis message processing
        readyForRedis = true;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

        // Send terminating handshake message
        for (Server server: synchronisedServers) {
            try {
                new RedisMessage(RedisMessage.MessageType.TERMINATE_HANDSHAKE,
                        new RedisMessage.MessageTarget(Settings.ServerType.BUKKIT, null),
                        server.serverUUID().toString(),
                        ProxyServer.getInstance().getName()).send();
            }  catch (IOException e) {
                getInstance().getLogger().log(Level.SEVERE, "Failed to serialize Redis message for handshake termination", e);
            }
        }

        // Close the database
        database.close();

        // Log to console
        getLogger().info("Disabled HuskSync (" + getProxy().getName() + ") v" + getDescription().getVersion());
    }

    /**
     * A record representing a server synchronised on the network and whether it has MySqlPlayerDataBridge installed
     */
    public record Server(UUID serverUUID, boolean hasMySqlPlayerDataBridge, String huskSyncVersion, String serverBrand) { }
}
