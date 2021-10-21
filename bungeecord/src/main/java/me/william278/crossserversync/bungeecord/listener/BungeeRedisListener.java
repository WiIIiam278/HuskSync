package me.william278.crossserversync.bungeecord.listener;

import me.william278.crossserversync.CrossServerSyncBungeeCord;
import me.william278.crossserversync.PlayerData;
import me.william278.crossserversync.Settings;
import me.william278.crossserversync.bungeecord.data.DataManager;
import me.william278.crossserversync.redis.RedisListener;
import me.william278.crossserversync.redis.RedisMessage;
import net.md_5.bungee.api.ProxyServer;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

public class BungeeRedisListener extends RedisListener {

    private static final CrossServerSyncBungeeCord plugin = CrossServerSyncBungeeCord.getInstance();

    // Initialize the listener on the bungee
    public BungeeRedisListener() {
        listen();
    }

    private PlayerData getPlayerCachedData(UUID uuid) {
        // Get the player data from the cache
        PlayerData cachedData = DataManager.playerDataCache.getPlayer(uuid);
        if (cachedData != null) {
            return cachedData;
        }

        // If the cache does not contain player data:
        DataManager.ensurePlayerExists(uuid); // Make sure the player is registered on MySQL

        final PlayerData data = DataManager.getPlayerData(uuid); // Get their player data from MySQL
        DataManager.playerDataCache.updatePlayer(data); // Update the cache
        return data; // Return the data
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

        switch (message.getMessageType()) {
            case PLAYER_DATA_REQUEST -> {
                // Get the UUID of the requesting player
                final UUID requestingPlayerUUID = UUID.fromString(message.getMessageData());
                ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
                    try {
                        // Send the reply, serializing the message data
                        new RedisMessage(RedisMessage.MessageType.PLAYER_DATA_SET,
                                new RedisMessage.MessageTarget(Settings.ServerType.BUKKIT, requestingPlayerUUID),
                                RedisMessage.serialize(getPlayerCachedData(requestingPlayerUUID))).send();
                    } catch (IOException e) {
                        log(Level.SEVERE, "Failed to serialize data when replying to a data request");
                        e.printStackTrace();
                    }
                });
            }
            case PLAYER_DATA_UPDATE -> {
                // Deserialize the PlayerData received
                PlayerData playerData;
                final String serializedPlayerData = message.getMessageData();
                try {
                    playerData = (PlayerData) RedisMessage.deserialize(serializedPlayerData);
                } catch (IOException | ClassNotFoundException e) {
                    log(Level.SEVERE, "Failed to deserialize PlayerData when handling a player update request");
                    e.printStackTrace();
                    return;
                }

                // Update the data in the cache and SQL
                DataManager.updatePlayerData(playerData);
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