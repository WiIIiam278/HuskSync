package me.william278.husksync;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import me.william278.husksync.migrator.MPDBMigrator;
import me.william278.husksync.proxy.data.DataManager;
import me.william278.husksync.redis.RedisMessage;
import me.william278.husksync.velocity.util.VelocityUpdateChecker;
import me.william278.husksync.velocity.command.VelocityCommand;
import me.william278.husksync.velocity.config.ConfigLoader;
import me.william278.husksync.velocity.config.ConfigManager;
import me.william278.husksync.velocity.listener.VelocityEventListener;
import me.william278.husksync.velocity.listener.VelocityRedisListener;
import me.william278.husksync.velocity.util.VelocityLogger;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.logging.Level;

import static me.william278.husksync.HuskSyncVelocity.VERSION;

@Plugin(
        id = "velocity",
        name = "HuskSync",
        version = VERSION,
        description = "HuskSync for velocity",
        authors = {"William278"}
)
public class HuskSyncVelocity {

    // Plugin version
    public static final String VERSION = "1.2-dev";

    // Velocity bStats ID (different from Bukkit and BungeeCord)
    private static final int METRICS_ID = 13489;
    private final Metrics.Factory metricsFactory;

    private static HuskSyncVelocity instance;

    public static HuskSyncVelocity getInstance() {
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

    private final Logger logger;
    private final ProxyServer server;
    private final Path dataDirectory;

    // Get the data folder
    public File getDataFolder() {
        return dataDirectory.toFile();
    }

    // Get the proxy server
    public ProxyServer getProxyServer() {
        return server;
    }

    // Velocity logger handling
    private VelocityLogger velocityLogger;

    public VelocityLogger getVelocityLogger() {
        return velocityLogger;
    }

    @Inject
    public HuskSyncVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.metricsFactory = metricsFactory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // Set instance
        instance = this;

        // Setup logger
        velocityLogger = new VelocityLogger(logger);

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
            new VelocityUpdateChecker(VERSION).logToConsole();
        }

        // Setup data manager
        dataManager = new DataManager(getVelocityLogger(), getDataFolder());

        // Setup player data cache
        for (Settings.SynchronisationCluster cluster : Settings.clusters) {
            dataManager.playerDataCache.put(cluster, new DataManager.PlayerDataCache());
        }

        // Initialize the redis listener
        if (!new VelocityRedisListener().isActiveAndEnabled) {
            getVelocityLogger().severe("Failed to initialize Redis; HuskSync will now abort loading itself (Velocity) v" + VERSION);
            return;
        }

        // Register listener
        server.getEventManager().register(this, new VelocityEventListener());

        // Register command
        CommandManager commandManager = getProxyServer().getCommandManager();
        CommandMeta meta = commandManager.metaBuilder("husksync")
                .aliases("hs")
                .build();
        commandManager.register(meta, new VelocityCommand());

        // Prepare the migrator for use if needed
        mpdbMigrator = new MPDBMigrator(getVelocityLogger());

        // Initialize bStats metrics
        try {
            metricsFactory.make(this, METRICS_ID);
        } catch (Exception e) {
            getVelocityLogger().info("Skipped metrics initialization");
        }

        // Log to console
        getVelocityLogger().info("Enabled HuskSync (Velocity) v" + VERSION);

        // Mark as ready for redis message processing
        readyForRedis = true;
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        // Plugin shutdown logic
        isDisabling = true;

        // Send terminating handshake message
        for (Server server : synchronisedServers) {
            try {
                new RedisMessage(RedisMessage.MessageType.TERMINATE_HANDSHAKE,
                        new RedisMessage.MessageTarget(Settings.ServerType.BUKKIT, null, server.clusterId()),
                        server.serverUUID().toString(),
                        "Velocity").send();
            } catch (IOException e) {
                getVelocityLogger().log(Level.SEVERE, "Failed to serialize Redis message for handshake termination", e);
            }
        }

        // Close database connections
        dataManager.closeDatabases();

        // Log to console
        getVelocityLogger().info("Disabled HuskSync (Velocity) v" + VERSION);
    }
}
