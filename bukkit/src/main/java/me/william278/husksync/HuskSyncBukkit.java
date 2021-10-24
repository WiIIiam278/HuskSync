package me.william278.husksync;

import me.william278.husksync.bukkit.config.ConfigLoader;
import me.william278.husksync.bukkit.data.BukkitDataCache;
import me.william278.husksync.bukkit.listener.BukkitRedisListener;
import me.william278.husksync.bukkit.listener.EventListener;
import me.william278.husksync.bukkit.migrator.MPDBDeserializer;
import me.william278.husksync.redis.RedisMessage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

public final class HuskSyncBukkit extends JavaPlugin {

    private static HuskSyncBukkit instance;

    public static HuskSyncBukkit getInstance() {
        return instance;
    }

    public static BukkitDataCache bukkitCache;

    // Used for establishing a handshake with redis
    public static UUID serverUUID;

    // Has a handshake been established with the Bungee?
    public static boolean handshakeCompleted = false;

    // THe handshake task to execute
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
                        new RedisMessage.MessageTarget(Settings.ServerType.BUNGEECORD, null),
                        serverUUID.toString(),
                        Boolean.toString(isMySqlPlayerDataBridgeInstalled),
                        Bukkit.getName()).send();
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
        try {
            new RedisMessage(RedisMessage.MessageType.TERMINATE_HANDSHAKE,
                    new RedisMessage.MessageTarget(Settings.ServerType.BUNGEECORD, null),
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

        // Check if MySqlPlayerDataBridge is installed
        Plugin mySqlPlayerDataBridge = Bukkit.getPluginManager().getPlugin("MySqlPlayerDataBridge");
        if (mySqlPlayerDataBridge != null) {
            isMySqlPlayerDataBridgeInstalled = mySqlPlayerDataBridge.isEnabled();
            MPDBDeserializer.setMySqlPlayerDataBridge();
            getLogger().info("MySQLPlayerDataBridge detected! Disabled data synchronisation to prevent data loss. To perform a migration, run \"husksync migrate\" in your Proxy (Bungeecord, Waterfall, etc) server console.");
        }

        // Initialize last data update UUID cache
        bukkitCache = new BukkitDataCache();

        // Initialize event listener
        getServer().getPluginManager().registerEvents(new EventListener(), this);

        // Initialize the redis listener
        if (!new BukkitRedisListener().isActiveAndEnabled) {
            getPluginLoader().disablePlugin(this);
            getLogger().severe("Failed to initialize Redis; disabling HuskSync (" + getServer().getName() + ") v" + getDescription().getVersion());
            return;
        }

        // Ensure redis is connected; establish a handshake
        establishRedisHandshake();

        // Log to console
        getLogger().info("Enabled HuskSync (" + getServer().getName() + ") v" + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        // Send termination handshake to proxy
        closeRedisHandshake();

        // Plugin shutdown logic
        getLogger().info("Disabled HuskSync (" + getServer().getName() + ") v" + getDescription().getVersion());
    }
}
