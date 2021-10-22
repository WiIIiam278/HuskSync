package me.william278.husksync.bukkit.listener;

import de.themoep.minedown.MineDown;
import me.william278.husksync.PlayerData;
import me.william278.husksync.HuskSyncBukkit;
import me.william278.husksync.bukkit.PlayerSetter;
import me.william278.husksync.redis.RedisListener;
import me.william278.husksync.MessageStrings;
import me.william278.husksync.Settings;
import me.william278.husksync.redis.RedisMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

public class BukkitRedisListener extends RedisListener {

    private static final HuskSyncBukkit plugin = HuskSyncBukkit.getInstance();

    // Initialize the listener on the bukkit server
    public BukkitRedisListener() {
        listen();
    }

    /**
     * Handle an incoming {@link me.william278.husksync.redis.RedisMessage}
     *
     * @param message The {@link me.william278.husksync.redis.RedisMessage} to handle
     */
    @Override
    public void handleMessage(me.william278.husksync.redis.RedisMessage message) {
        // Ignore messages for proxy servers
        if (!message.getMessageTarget().targetServerType().equals(Settings.ServerType.BUKKIT)) {
            return;
        }


        // Handle the message for the player
        if (message.getMessageTarget().targetPlayerUUID() == null) {
            if (message.getMessageType() == me.william278.husksync.redis.RedisMessage.MessageType.REQUEST_DATA_ON_JOIN) {
                UUID playerUUID = UUID.fromString(message.getMessageDataElements()[1]);
                switch (me.william278.husksync.redis.RedisMessage.RequestOnJoinUpdateType.valueOf(message.getMessageDataElements()[0])) {
                    case ADD_REQUESTER -> HuskSyncBukkit.bukkitCache.setRequestOnJoin(playerUUID);
                    case REMOVE_REQUESTER -> HuskSyncBukkit.bukkitCache.removeRequestOnJoin(playerUUID);
                }
            }
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getUniqueId().equals(message.getMessageTarget().targetPlayerUUID())) {
                    switch (message.getMessageType()) {
                        case PLAYER_DATA_SET -> {
                            try {
                                // Deserialize the received PlayerData
                                PlayerData data = (PlayerData) RedisMessage.deserialize(message.getMessageData());

                                // Set the player's data
                                PlayerSetter.setPlayerFrom(player, data);
                            } catch (IOException | ClassNotFoundException e) {
                                log(Level.SEVERE, "Failed to deserialize PlayerData when handling a reply from the proxy with PlayerData");
                                e.printStackTrace();
                            }
                        }
                        case SEND_PLUGIN_INFORMATION -> {
                            String proxyBrand = message.getMessageDataElements()[0];
                            String proxyVersion = message.getMessageDataElements()[1];
                            assert plugin.getDescription().getDescription() != null;
                            player.spigot().sendMessage(new MineDown(MessageStrings.PLUGIN_INFORMATION.toString()
                                    .replaceAll("%plugin_description%", plugin.getDescription().getDescription())
                                    .replaceAll("%proxy_brand%", proxyBrand)
                                    .replaceAll("%proxy_version%", proxyVersion)
                                    .replaceAll("%bukkit_brand%", Bukkit.getName())
                                    .replaceAll("%bukkit_version%", plugin.getDescription().getVersion()))
                                    .toComponent());
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
