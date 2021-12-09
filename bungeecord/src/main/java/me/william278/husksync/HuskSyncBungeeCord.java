package me.william278.husksync;

import me.william278.husksync.bungeecord.command.BungeeCommand;
import me.william278.husksync.bungeecord.config.ConfigLoader;
import me.william278.husksync.bungeecord.config.ConfigManager;
import me.william278.husksync.proxy.data.DataManager;
import me.william278.husksync.bungeecord.listener.BungeeEventListener;
import me.william278.husksync.bungeecord.listener.BungeeRedisListener;
import me.william278.husksync.migrator.MPDBMigrator;
import me.william278.husksync.bungeecord.util.BungeeLogger;
import me.william278.husksync.bungeecord.util.BungeeUpdateChecker;
import me.william278.husksync.redis.RedisMessage;
import me.william278.husksync.util.Logger;
import net.byteflux.libby.BungeeLibraryManager;
import net.byteflux.libby.Library;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import org.bstats.bungeecord.Metrics;

import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
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

    public static DataManager dataManager;

    public static MPDBMigrator mpdbMigrator;

    private Logger logger;

    public Logger getBungeeLogger() {
        return logger;
    }

    @Override
    public void onLoad() {
        instance = this;
        logger = new BungeeLogger(getLogger());
        fetchDependencies();
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

        // Setup data manager
        dataManager = new DataManager(getBungeeLogger(), getDataFolder());

        // Ensure the data manager initialized correctly
        if (dataManager.hasFailedInitialization) {
            getBungeeLogger().severe("Failed to initialize the HuskSync database(s).\n" +
                    "HuskSync will now abort loading itself (" + getProxy().getName() + ") v" + getDescription().getVersion());
        }

        // Setup player data cache
        for (Settings.SynchronisationCluster cluster : Settings.clusters) {
            dataManager.playerDataCache.put(cluster, new DataManager.PlayerDataCache());
        }

        // Initialize the redis listener
        if (!new BungeeRedisListener().isActiveAndEnabled) {
            getBungeeLogger().severe("Failed to initialize Redis; HuskSync will now abort loading itself (" + getProxy().getName() + ") v" + getDescription().getVersion());
            return;
        }

        // Register listener
        getProxy().getPluginManager().registerListener(this, new BungeeEventListener());

        // Register command
        getProxy().getPluginManager().registerCommand(this, new BungeeCommand());

        // Prepare the migrator for use if needed
        mpdbMigrator = new MPDBMigrator(getBungeeLogger());

        // Initialize bStats metrics
        try {
            new Metrics(this, METRICS_ID);
        } catch (Exception e) {
            getBungeeLogger().info("Skipped metrics initialization");
        }

        // Log to console
        getBungeeLogger().info("Enabled HuskSync (" + getProxy().getName() + ") v" + getDescription().getVersion());

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
                getBungeeLogger().log(Level.SEVERE, "Failed to serialize Redis message for handshake termination", e);
            }
        }

        dataManager.closeDatabases();

        // Log to console
        getBungeeLogger().info("Disabled HuskSync (" + getProxy().getName() + ") v" + getDescription().getVersion());
    }

    // Load dependencies
    private void fetchDependencies() {
        BungeeLibraryManager manager = new BungeeLibraryManager(getInstance());

        Library mySqlLib = Library.builder()
                .groupId("mysql")
                .artifactId("mysql-connector-java")
                .version("8.0.25")
                .build();

        Library sqLiteLib = Library.builder()
                .groupId("org.xerial")
                .artifactId("sqlite-jdbc")
                .version("3.36.0.3")
                .build();

        manager.addMavenCentral();
        manager.loadLibrary(mySqlLib);
        manager.loadLibrary(sqLiteLib);
    }
}
