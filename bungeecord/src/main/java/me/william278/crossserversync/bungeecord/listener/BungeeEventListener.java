package me.william278.crossserversync.bungeecord.listener;

import me.william278.crossserversync.CrossServerSyncBungeeCord;
import me.william278.crossserversync.PlayerData;
import me.william278.crossserversync.Settings;
import me.william278.crossserversync.bungeecord.data.DataManager;
import me.william278.crossserversync.redis.RedisMessage;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.IOException;
import java.util.logging.Level;

public class BungeeEventListener implements Listener {

    private static final CrossServerSyncBungeeCord plugin = CrossServerSyncBungeeCord.getInstance();

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        final ProxiedPlayer player = event.getPlayer();
        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            // Ensure the player has data on SQL
            DataManager.ensurePlayerExists(player.getUniqueId());

            // Get the player's data from SQL
            final PlayerData data = DataManager.getPlayerData(player.getUniqueId());

            // Update the player's data from SQL onto the cache
            DataManager.playerDataCache.updatePlayer(data);

            // Send a message asking the bukkit to request data on join
            try {
                new RedisMessage(RedisMessage.MessageType.REQUEST_DATA_ON_JOIN,
                        new RedisMessage.MessageTarget(Settings.ServerType.BUKKIT, null),
                        RedisMessage.RequestOnJoinUpdateType.ADD_REQUESTER.toString(), player.getUniqueId().toString()).send();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to serialize request data on join message data");
                e.printStackTrace();
            }
        });
    }

}
