package me.william278.husksync.bungeecord.listener;

import me.william278.husksync.HuskSyncBungeeCord;
import me.william278.husksync.PlayerData;
import me.william278.husksync.bungeecord.data.DataManager;
import me.william278.husksync.redis.RedisListener;
import me.william278.husksync.Settings;
import me.william278.husksync.redis.RedisMessage;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

public class BungeeRedisListener extends RedisListener {

    private static final HuskSyncBungeeCord plugin = HuskSyncBungeeCord.getInstance();

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
     * Handle an incoming {@link me.william278.husksync.redis.RedisMessage}
     *
     * @param message The {@link me.william278.husksync.redis.RedisMessage} to handle
     */
    @Override
    public void handleMessage(me.william278.husksync.redis.RedisMessage message) {
        // Ignore messages destined for Bukkit servers
        if (message.getMessageTarget().targetServerType() != me.william278.husksync.Settings.ServerType.BUNGEECORD) {
            return;
        }

        switch (message.getMessageType()) {
            case PLAYER_DATA_REQUEST -> {
                // Get the UUID of the requesting player
                final UUID requestingPlayerUUID = UUID.fromString(message.getMessageData());
                ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
                    try {
                        // Send the reply, serializing the message data
                        new me.william278.husksync.redis.RedisMessage(me.william278.husksync.redis.RedisMessage.MessageType.PLAYER_DATA_SET,
                                new me.william278.husksync.redis.RedisMessage.MessageTarget(me.william278.husksync.Settings.ServerType.BUKKIT, requestingPlayerUUID),
                                me.william278.husksync.redis.RedisMessage.serialize(getPlayerCachedData(requestingPlayerUUID)))
                                .send();

                        // Send an update to all bukkit servers removing the player from the requester cache
                        new me.william278.husksync.redis.RedisMessage(me.william278.husksync.redis.RedisMessage.MessageType.REQUEST_DATA_ON_JOIN,
                                new me.william278.husksync.redis.RedisMessage.MessageTarget(me.william278.husksync.Settings.ServerType.BUKKIT, null),
                                me.william278.husksync.redis.RedisMessage.RequestOnJoinUpdateType.REMOVE_REQUESTER.toString(), requestingPlayerUUID.toString())
                                .send();
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
                    playerData = (PlayerData) me.william278.husksync.redis.RedisMessage.deserialize(serializedPlayerData);
                } catch (IOException | ClassNotFoundException e) {
                    log(Level.SEVERE, "Failed to deserialize PlayerData when handling a player update request");
                    e.printStackTrace();
                    return;
                }

                // Update the data in the cache and SQL
                DataManager.updatePlayerData(playerData);

                // Reply with the player data if they are still online (switching server)
                try {
                    ProxiedPlayer player = ProxyServer.getInstance().getPlayer(playerData.getPlayerUUID());
                    if (player != null) {
                        if (player.isConnected()) {
                            new me.william278.husksync.redis.RedisMessage(me.william278.husksync.redis.RedisMessage.MessageType.PLAYER_DATA_SET,
                                    new me.william278.husksync.redis.RedisMessage.MessageTarget(Settings.ServerType.BUKKIT, playerData.getPlayerUUID()),
                                    RedisMessage.serialize(playerData))
                                    .send();
                        }
                    }
                } catch (IOException e) {
                    log(Level.SEVERE, "Failed to re-serialize PlayerData when handling a player update request");
                    e.printStackTrace();
                }
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