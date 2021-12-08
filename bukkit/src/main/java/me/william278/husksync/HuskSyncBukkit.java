package me.william278.husksync;

import me.william278.husksync.bukkit.config.ConfigLoader;
import me.william278.husksync.bukkit.data.BukkitDataCache;
import me.william278.husksync.bukkit.listener.BukkitEventListener;
import me.william278.husksync.bukkit.listener.BukkitRedisListener;
import me.william278.husksync.bukkit.util.BukkitUpdateChecker;
import me.william278.husksync.bukkit.util.PlayerSetter;
import me.william278.husksync.redis.RedisMessage;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

public final class HuskSyncBukkit extends JavaPlugin {

    // Bukkit bStats ID (Different to BungeeCord)
    private static final int METRICS_ID = 13140;

    private static HuskSyncBukkit instance;
    public static HuskSyncBukkit getInstance() {
        return instance;
    }

    public static BukkitDataCache bukkitCache;

    // Used for establishing a handshake with redis
    public static UUID serverUUID;

    // Has a handshake been established with the Bungee?
    public static boolean handshakeCompleted = false;

    // The handshake task to execute
    private static BukkitTask handshakeTask;

    // Whether MySqlPlayerDataBridge is installed
    public static boolean isMySqlPlayerDataBridgeInstalled;

    // Establish the handshake with the proxy
    public static void establishRedisHandshake() {
        serverUUID = UUID.randomUUID();
        getInstance().getLogger().log(Level.INFO, "Executing handshake with Proxy server...");
        final int[] attempts = {0}; // How many attempts to establish communication have been made
        handshakeTask = Bukkit.getScheduler().runTaskTimerAsynchronously(getInstance(), () -> {
            if (handshakeCompleted) {
                handshakeTask.cancel();
                return;
            }
            try {
                new RedisMessage(RedisMessage.MessageType.CONNECTION_HANDSHAKE,
                        new RedisMessage.MessageTarget(Settings.ServerType.PROXY, null, Settings.cluster),
                        serverUUID.toString(),
                        Boolean.toString(isMySqlPlayerDataBridgeInstalled),
                        Bukkit.getName(),
                        getInstance().getDescription().getVersion())
                        .send();
                attempts[0]++;
                if (attempts[0] == 10) {
                    getInstance().getLogger().log(Level.WARNING, "Failed to complete handshake with the Proxy server; Please make sure your Proxy server is online and has HuskSync installed in its' /plugins/ folder. HuskSync will continue to try and establish a connection.");
                }
            } catch (IOException e) {
                getInstance().getLogger().log(Level.SEVERE, "Failed to serialize Redis message for handshake establishment", e);
            }
        }, 0, 60);
    }

    private void closeRedisHandshake() {
        if (!handshakeCompleted) return;
        try {
            new RedisMessage(RedisMessage.MessageType.TERMINATE_HANDSHAKE,
                    new RedisMessage.MessageTarget(Settings.ServerType.PROXY, null, Settings.cluster),
                    serverUUID.toString(),
                    Bukkit.getName()).send();
        }  catch (IOException e) {
            getInstance().getLogger().log(Level.SEVERE, "Failed to serialize Redis message for handshake termination", e);
        }
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

        // Do update checker
        if (Settings.automaticUpdateChecks) {
            new BukkitUpdateChecker().logToConsole();
        }

        // Initialize last data update UUID cache
        bukkitCache = new BukkitDataCache();

        // Initialize event listener
        getServer().getPluginManager().registerEvents(new BukkitEventListener(), this);

        // Initialize the redis listener
        if (!new BukkitRedisListener().isActiveAndEnabled) {
            getPluginLoader().disablePlugin(this);
            getLogger().severe("Failed to initialize Redis; disabling HuskSync (" + getServer().getName() + ") v" + getDescription().getVersion());
            return;
        }

        // Ensure redis is connected; establish a handshake
        establishRedisHandshake();

        // Initialize bStats metrics
        try {
            new Metrics(this, METRICS_ID);
        } catch (Exception e) {
            getLogger().info("Skipped metrics initialization");
        }

        // Log to console
        getLogger().info("Enabled HuskSync (" + getServer().getName() + ") v" + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        // Update player data for disconnecting players
        if (HuskSyncBukkit.handshakeCompleted && !HuskSyncBukkit.isMySqlPlayerDataBridgeInstalled && Bukkit.getOnlinePlayers().size() > 0) {
            getLogger().info("Saving data for remaining online players...");
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerSetter.updatePlayerData(player);
            }
            getLogger().info("Data save complete!");
        }


        // Send termination handshake to proxy
        closeRedisHandshake();

        // Plugin shutdown logic
        getLogger().info("Disabled HuskSync (" + getServer().getName() + ") v" + getDescription().getVersion());
    }
}
