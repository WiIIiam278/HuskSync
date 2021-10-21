package me.william278.crossserversync.bukkit.listener;

import me.william278.crossserversync.PlayerData;
import me.william278.crossserversync.Settings;
import me.william278.crossserversync.CrossServerSyncBukkit;
import me.william278.crossserversync.bukkit.PlayerSetter;
import me.william278.crossserversync.redis.RedisListener;
import me.william278.crossserversync.redis.RedisMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
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
        if (!message.getMessageTarget().targetServerType().equals(Settings.ServerType.BUKKIT)) {
            return;
        }
        // Handle the message for the player
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getUniqueId().equals(message.getMessageTarget().targetPlayerUUID())) {
                if (message.getMessageType().equals(RedisMessage.MessageType.PLAYER_DATA_REPLY)) {
                    try {
                        // Deserialize the received PlayerData
                        PlayerData data = (PlayerData) RedisMessage.deserialize(message.getMessageData());

                        // Set the player's data
                        PlayerSetter.setPlayerFrom(player, data);

                        // Update last loaded data UUID
                        CrossServerSyncBukkit.lastDataUpdateUUIDCache.setVersionUUID(player.getUniqueId(), data.getDataVersionUUID());
                    } catch (IOException | ClassNotFoundException e) {
                        log(Level.SEVERE, "Failed to deserialize PlayerData when handling a reply from the proxy with PlayerData");
                        e.printStackTrace();
                    }
                }
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
