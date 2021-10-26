package me.william278.husksync.bungeecord.listener;

import de.themoep.minedown.MineDown;
import me.william278.husksync.HuskSyncBungeeCord;
import me.william278.husksync.util.MessageManager;
import me.william278.husksync.PlayerData;
import me.william278.husksync.Settings;
import me.william278.husksync.bungeecord.data.DataManager;
import me.william278.husksync.bungeecord.migrator.MPDBMigrator;
import me.william278.husksync.redis.RedisListener;
import me.william278.husksync.redis.RedisMessage;
import net.md_5.bungee.api.ChatMessageType;
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
        // Only process redis messages when ready
        if (!HuskSyncBungeeCord.readyForRedis) {
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
                                RedisMessage.serialize(getPlayerCachedData(requestingPlayerUUID)))
                                .send();

                        // Send an update to all bukkit servers removing the player from the requester cache
                        new RedisMessage(RedisMessage.MessageType.REQUEST_DATA_ON_JOIN,
                                new RedisMessage.MessageTarget(Settings.ServerType.BUKKIT, null),
                                RedisMessage.RequestOnJoinUpdateType.REMOVE_REQUESTER.toString(), requestingPlayerUUID.toString())
                                .send();

                        // Send synchronisation complete message
                        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(requestingPlayerUUID);
                        if (player.isConnected()) {
                            player.sendMessage(ChatMessageType.ACTION_BAR, new MineDown(MessageManager.getMessage("synchronisation_complete")).toComponent());
                        }
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

                // Reply with the player data if they are still online (switching server)
                try {
                    ProxiedPlayer player = ProxyServer.getInstance().getPlayer(playerData.getPlayerUUID());
                    if (player != null) {
                        if (player.isConnected()) {
                            new RedisMessage(RedisMessage.MessageType.PLAYER_DATA_SET,
                                    new RedisMessage.MessageTarget(Settings.ServerType.BUKKIT, playerData.getPlayerUUID()),
                                    RedisMessage.serialize(playerData))
                                    .send();

                            // Send synchronisation complete message
                            player.sendMessage(ChatMessageType.ACTION_BAR, new MineDown(MessageManager.getMessage("synchronisation_complete")).toComponent());
                        }
                    }
                } catch (IOException e) {
                    log(Level.SEVERE, "Failed to re-serialize PlayerData when handling a player update request");
                    e.printStackTrace();
                }
            }
            case CONNECTION_HANDSHAKE -> {
                // Reply to a Bukkit server's connection handshake to complete the process
                final UUID serverUUID = UUID.fromString(message.getMessageDataElements()[0]);
                final boolean hasMySqlPlayerDataBridge = Boolean.parseBoolean(message.getMessageDataElements()[1]);
                final String bukkitBrand = message.getMessageDataElements()[2];
                final String huskSyncVersion = message.getMessageDataElements()[3];
                try {
                    new RedisMessage(RedisMessage.MessageType.CONNECTION_HANDSHAKE,
                            new RedisMessage.MessageTarget(Settings.ServerType.BUKKIT, null),
                            serverUUID.toString(), plugin.getProxy().getName())
                            .send();
                    HuskSyncBungeeCord.synchronisedServers.add(
                            new HuskSyncBungeeCord.Server(serverUUID, hasMySqlPlayerDataBridge,
                                    huskSyncVersion, bukkitBrand));
                    log(Level.INFO, "Completed handshake with " + bukkitBrand + " server (" + serverUUID + ")");
                } catch (IOException e) {
                    log(Level.SEVERE, "Failed to serialize handshake message data");
                    e.printStackTrace();
                }
            }
            case TERMINATE_HANDSHAKE -> {
                // Terminate the handshake with a Bukkit server
                final UUID serverUUID = UUID.fromString(message.getMessageDataElements()[0]);
                final String bukkitBrand = message.getMessageDataElements()[1];

                // Remove a server from the synchronised server list
                HuskSyncBungeeCord.Server serverToRemove = null;
                for (HuskSyncBungeeCord.Server server : HuskSyncBungeeCord.synchronisedServers) {
                    if (server.serverUUID().equals(serverUUID)) {
                        serverToRemove = server;
                        break;
                    }
                }
                HuskSyncBungeeCord.synchronisedServers.remove(serverToRemove);
                log(Level.INFO, "Terminated the handshake with " + bukkitBrand + " server (" + serverUUID + ")");
            }
            case DECODED_MPDB_DATA_SET -> {
                // Deserialize the PlayerData received
                PlayerData playerData;
                final String serializedPlayerData = message.getMessageDataElements()[0];
                final String playerName = message.getMessageDataElements()[1];
                try {
                    playerData = (PlayerData) RedisMessage.deserialize(serializedPlayerData);
                } catch (IOException | ClassNotFoundException e) {
                    log(Level.SEVERE, "Failed to deserialize PlayerData when handling incoming decoded MPDB data");
                    e.printStackTrace();
                    return;
                }

                // Add the incoming data to the data to be saved
                MPDBMigrator.incomingPlayerData.put(playerData, playerName);

                // Increment players migrated
                MPDBMigrator.playersMigrated++;
                plugin.getLogger().log(Level.INFO, "Migrated " + MPDBMigrator.playersMigrated + "/" + MPDBMigrator.migratedDataSent + " players.");

                // When all the data has been received, save it
                if (MPDBMigrator.migratedDataSent == MPDBMigrator.playersMigrated) {
                    MPDBMigrator.loadIncomingData(MPDBMigrator.incomingPlayerData);
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