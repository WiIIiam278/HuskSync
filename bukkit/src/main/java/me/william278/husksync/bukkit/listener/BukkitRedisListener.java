package me.william278.husksync.bukkit.listener;

import de.themoep.minedown.MineDown;
import me.william278.husksync.HuskSyncBukkit;
import me.william278.husksync.PlayerData;
import me.william278.husksync.Settings;
import me.william278.husksync.bukkit.api.HuskSyncAPI;
import me.william278.husksync.bukkit.config.ConfigLoader;
import me.william278.husksync.bukkit.data.DataViewer;
import me.william278.husksync.bukkit.migrator.MPDBDeserializer;
import me.william278.husksync.bukkit.util.PlayerSetter;
import me.william278.husksync.migrator.MPDBPlayerData;
import me.william278.husksync.redis.RedisListener;
import me.william278.husksync.redis.RedisMessage;
import me.william278.husksync.util.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

public class BukkitRedisListener extends RedisListener {

    private static final HuskSyncBukkit plugin = HuskSyncBukkit.getInstance();

    // Initialize the listener on the bukkit server
    public BukkitRedisListener() {
        super();
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
        // Ignore messages if the plugin is disabled
        if (!plugin.isEnabled()) {
            return;
        }
        // Ignore messages for other clusters if applicable
        final String targetClusterId = message.getMessageTarget().targetClusterId();
        if (targetClusterId != null) {
            if (!targetClusterId.equalsIgnoreCase(Settings.cluster)) {
                return;
            }
        }

        // Handle the incoming redis message; either for a specific player or the system
        if (message.getMessageTarget().targetPlayerUUID() == null) {
            switch (message.getMessageType()) {
                case REQUEST_DATA_ON_JOIN -> {
                    UUID playerUUID = UUID.fromString(message.getMessageDataElements()[1]);
                    switch (RedisMessage.RequestOnJoinUpdateType.valueOf(message.getMessageDataElements()[0])) {
                        case ADD_REQUESTER -> HuskSyncBukkit.bukkitCache.setRequestOnJoin(playerUUID);
                        case REMOVE_REQUESTER -> HuskSyncBukkit.bukkitCache.removeRequestOnJoin(playerUUID);
                    }
                }
                case CONNECTION_HANDSHAKE -> {
                    UUID serverUUID = UUID.fromString(message.getMessageDataElements()[0]);
                    String proxyBrand = message.getMessageDataElements()[1];
                    if (serverUUID.equals(HuskSyncBukkit.serverUUID)) {
                        HuskSyncBukkit.handshakeCompleted = true;
                        log(Level.INFO, "Completed handshake with " + proxyBrand + " proxy (" + serverUUID + ")");

                        // If there are any players awaiting a data update, request it
                        for (UUID uuid : HuskSyncBukkit.bukkitCache.getAwaitingDataFetch()) {
                            try {
                                PlayerSetter.requestPlayerData(uuid);
                            } catch (IOException e) {
                                log(Level.SEVERE, "Failed to serialize handshake message data");
                            }
                        }
                    }
                }
                case TERMINATE_HANDSHAKE -> {
                    UUID serverUUID = UUID.fromString(message.getMessageDataElements()[0]);
                    String proxyBrand = message.getMessageDataElements()[1];
                    if (serverUUID.equals(HuskSyncBukkit.serverUUID)) {
                        HuskSyncBukkit.handshakeCompleted = false;
                        log(Level.WARNING, proxyBrand + " proxy has terminated communications; attempting to re-establish (" + serverUUID + ")");

                        // Attempt to re-establish communications via another handshake
                        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, HuskSyncBukkit::establishRedisHandshake, 20);
                    }
                }
                case DECODE_MPDB_DATA -> {
                    UUID serverUUID = UUID.fromString(message.getMessageDataElements()[0]);
                    String encodedData = message.getMessageDataElements()[1];
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        if (serverUUID.equals(HuskSyncBukkit.serverUUID)) {
                            try {
                                MPDBPlayerData data = (MPDBPlayerData) RedisMessage.deserialize(encodedData);
                                new RedisMessage(RedisMessage.MessageType.DECODED_MPDB_DATA_SET,
                                        new RedisMessage.MessageTarget(Settings.ServerType.PROXY, null, Settings.cluster),
                                        RedisMessage.serialize(MPDBDeserializer.convertMPDBData(data)),
                                        data.playerName)
                                        .send();
                            } catch (IOException | ClassNotFoundException e) {
                                log(Level.SEVERE, "Failed to serialize encoded MPDB data");
                            }
                        }
                    });
                }
                case API_DATA_RETURN -> {
                    final UUID requestUUID = UUID.fromString(message.getMessageDataElements()[0]);
                    if (HuskSyncAPI.apiRequests.containsKey(requestUUID)) {
                        try {
                            final PlayerData data = (PlayerData) RedisMessage.deserialize(message.getMessageDataElements()[1]);
                            HuskSyncAPI.apiRequests.get(requestUUID).complete(data);
                        } catch (IOException | ClassNotFoundException e) {
                            log(Level.SEVERE, "Failed to serialize returned API-requested player data");
                        }
                    }

                }
                case API_DATA_CANCEL -> {
                    final UUID requestUUID = UUID.fromString(message.getMessageDataElements()[0]);
                    // Cancel requests if no data could be found on the proxy
                    if (HuskSyncAPI.apiRequests.containsKey(requestUUID)) {
                        HuskSyncAPI.apiRequests.get(requestUUID).cancel(true);
                    }
                }
                case RELOAD_CONFIG -> {
                    plugin.reloadConfig();
                    ConfigLoader.loadSettings(plugin.getConfig());
                }
            }
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getUniqueId().equals(message.getMessageTarget().targetPlayerUUID())) {
                    switch (message.getMessageType()) {
                        case PLAYER_DATA_SET -> {
                            if (HuskSyncBukkit.isMySqlPlayerDataBridgeInstalled) return;
                            try {
                                // Deserialize the received PlayerData
                                PlayerData data = (PlayerData) RedisMessage.deserialize(message.getMessageData());

                                // Set the player's data
                                PlayerSetter.setPlayerFrom(player, data);
                            } catch (IOException | ClassNotFoundException e) {
                                log(Level.SEVERE, "Failed to deserialize PlayerData when handling data from the proxy");
                                e.printStackTrace();
                            }
                        }
                        case SEND_PLUGIN_INFORMATION -> {
                            String proxyBrand = message.getMessageDataElements()[0];
                            String proxyVersion = message.getMessageDataElements()[1];
                            assert plugin.getDescription().getDescription() != null;
                            player.spigot().sendMessage(new MineDown(MessageManager.PLUGIN_INFORMATION.toString()
                                    .replaceAll("%plugin_description%", plugin.getDescription().getDescription())
                                    .replaceAll("%proxy_brand%", proxyBrand)
                                    .replaceAll("%proxy_version%", proxyVersion)
                                    .replaceAll("%bukkit_brand%", Bukkit.getName())
                                    .replaceAll("%bukkit_version%", plugin.getDescription().getVersion()))
                                    .toComponent());
                        }
                        case OPEN_INVENTORY -> {
                            // Get the name of the inventory owner
                            String inventoryOwnerName = message.getMessageDataElements()[0];

                            // Synchronously do inventory setting, etc
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                try {
                                    // Get that player's data
                                    PlayerData data = (PlayerData) RedisMessage.deserialize(message.getMessageDataElements()[1]);

                                    // Show the data to the player
                                    DataViewer.showData(player, new DataViewer.DataView(data, inventoryOwnerName, DataViewer.DataView.InventoryType.INVENTORY));
                                } catch (IOException | ClassNotFoundException e) {
                                    log(Level.SEVERE, "Failed to deserialize PlayerData when handling inventory-see data from the proxy");
                                    e.printStackTrace();
                                }
                            });
                        }
                        case OPEN_ENDER_CHEST -> {
                            // Get the name of the inventory owner
                            String enderChestOwnerName = message.getMessageDataElements()[0];

                            // Synchronously do inventory setting, etc
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                try {
                                    // Get that player's data
                                    PlayerData data = (PlayerData) RedisMessage.deserialize(message.getMessageDataElements()[1]);

                                    // Show the data to the player
                                    DataViewer.showData(player, new DataViewer.DataView(data, enderChestOwnerName, DataViewer.DataView.InventoryType.ENDER_CHEST));
                                } catch (IOException | ClassNotFoundException e) {
                                    log(Level.SEVERE, "Failed to deserialize PlayerData when handling ender chest-see data from the proxy");
                                    e.printStackTrace();
                                }
                            });
                        }
                    }
                    return;
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
