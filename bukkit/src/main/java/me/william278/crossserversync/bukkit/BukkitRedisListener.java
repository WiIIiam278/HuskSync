package me.william278.crossserversync.bukkit;

import me.william278.crossserversync.Settings;
import me.william278.crossserversync.redis.RedisListener;
import me.william278.crossserversync.redis.RedisMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class BukkitRedisListener extends RedisListener {

    private static final CrossServerSyncBukkit plugin = CrossServerSyncBukkit.getInstance();

    // Initialize the listener on the bukkit server
    public BukkitRedisListener() {
        listen();
    }

    /**
     * Handle an incoming {@link RedisMessage}
     *
     * @param message The {@link RedisMessage} to handle
     */
    @Override
    public void handleMessage(RedisMessage message) {
        // Ignore messages for proxy servers
        if (message.getMessageTarget().targetServerType() != Settings.ServerType.BUKKIT) {
            return;
        }
        // Handle the message for the player
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getUniqueId() == message.getMessageTarget().targetPlayerName()) {

                return;
            }
        }
    }

    /**
     * Log to console
     *
     * @param level   The {@link Level} to log
     * @param message Message to log
     */
    @Override
    public void log(Level level, String message) {
        plugin.getLogger().log(level, message);
    }
}
