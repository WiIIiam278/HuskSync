package me.william278.crossserversync.bungeecord;

import me.william278.crossserversync.Settings;
import me.william278.crossserversync.redis.RedisListener;
import me.william278.crossserversync.redis.RedisMessage;

import java.util.logging.Level;

public class BungeeRedisListener extends RedisListener {

    private static final CrossServerSyncBungeeCord plugin = CrossServerSyncBungeeCord.getInstance();

    // Initialize the listener on the bungee
    public BungeeRedisListener() {
        listen();
    }

    /**
     * Handle an incoming {@link RedisMessage}
     *
     * @param message The {@link RedisMessage} to handle
     */
    @Override
    public void handleMessage(RedisMessage message) {
        // Ignore messages destined for Bukkit servers
        if (message.getMessageTarget().targetServerType() != Settings.ServerType.BUNGEECORD) {
            return;
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
