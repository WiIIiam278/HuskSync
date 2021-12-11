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
import me.william278.husksync.bungeecord.util.BungeeUpdateChecker;
import me.william278.husksync.redis.RedisMessage;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import org.bstats.bungeecord.Metrics;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

public final class HuskSyncBungeeCord extends Plugin {

    // BungeeCord bStats ID (different to Bukkit)
    private static final int METRICS_ID = 13141;

    private static HuskSyncBungeeCord instance;

    public static HuskSyncBungeeCord getInstance() {
        return instance;
    }

    // Whether the plugin is ready to accept redis messages
    public static boolean readyForRedis = false;

    // Whether the plugin is in the process of disabling and should skip responding to handshake confirmations
    public static boolean isDisabling = false;

    /**
     * Set of all the {@link Server}s that have completed the synchronisation handshake with HuskSync on the proxy
     */
    public static HashSet<Server> synchronisedServers;

    private static HashMap<String, Database> clusterDatabases;

    public static Connection getConnection(String clusterId) throws SQLException {
        return clusterDatabases.get(clusterId).getConnection();
    }

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
        clusterDatabases = new HashMap<>();
        for (Settings.SynchronisationCluster cluster : Settings.clusters) {
            Database clusterDatabase = switch (Settings.dataStorageType) {
                case SQLITE -> new SQLite(this, cluster);
                case MYSQL -> new MySQL(this, cluster);
            };
            clusterDatabase.load();
            clusterDatabase.createTables();
            clusterDatabases.put(cluster.clusterId(), clusterDatabase);
        }

        // Abort loading if the database failed to initialize
        for (Database database : clusterDatabases.values()) {
            if (database.isInactive()) {
                getLogger().severe("Failed to initialize the database(s); HuskSync will now abort loading itself (" + getProxy().getName() + ") v" + getDescription().getVersion());
                return;
            }
        }


        // Setup player data cache
        for (Settings.SynchronisationCluster cluster : Settings.clusters) {
            DataManager.playerDataCache.put(cluster, new DataManager.PlayerDataCache());
        }

        // Initialize the redis listener
        if (!new BungeeRedisListener().isActiveAndEnabled) {
            getLogger().severe("Failed to initialize Redis; HuskSync will now abort loading itself (" + getProxy().getName() + ") v" + getDescription().getVersion());
            return;
        }

        // Register listener
        getProxy().getPluginManager().registerListener(this, new BungeeEventListener());

        // Register command
        getProxy().getPluginManager().registerCommand(this, new HuskSyncCommand());

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
        isDisabling = true;

        // Send terminating handshake message
        for (Server server : synchronisedServers) {
            try {
                new RedisMessage(RedisMessage.MessageType.TERMINATE_HANDSHAKE,
                        new RedisMessage.MessageTarget(Settings.ServerType.BUKKIT, null, server.clusterId()),
                        server.serverUUID().toString(),
                        ProxyServer.getInstance().getName()).send();
            } catch (IOException e) {
                getInstance().getLogger().log(Level.SEVERE, "Failed to serialize Redis message for handshake termination", e);
            }
        }

        // Close the database
        for (Database database : clusterDatabases.values()) {
            database.close();
        }

        // Log to console
        getLogger().info("Disabled HuskSync (" + getProxy().getName() + ") v" + getDescription().getVersion());
    }

    /**
     * A record representing a server synchronised on the network and whether it has MySqlPlayerDataBridge installed
     */
    public record Server(UUID serverUUID, boolean hasMySqlPlayerDataBridge, String huskSyncVersion, String serverBrand,
                         String clusterId) {
    }
}
