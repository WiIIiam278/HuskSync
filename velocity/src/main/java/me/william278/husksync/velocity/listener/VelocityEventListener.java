package me.william278.husksync.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import me.william278.husksync.HuskSyncVelocity;
import me.william278.husksync.PlayerData;
import me.william278.husksync.Settings;
import me.william278.husksync.redis.RedisMessage;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

public class VelocityEventListener {

    private static final HuskSyncVelocity plugin = HuskSyncVelocity.getInstance();

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        final Player player = event.getPlayer();
        plugin.getProxyServer().getScheduler().buildTask(plugin, () -> {
            // Ensure the player has data on SQL and that it is up-to-date
            HuskSyncVelocity.dataManager.ensurePlayerExists(player.getUniqueId(), player.getUsername());

            // Get the player's data from SQL
            final Map<Settings.SynchronisationCluster, PlayerData> data = HuskSyncVelocity.dataManager.getPlayerData(player.getUniqueId());

            // Update the player's data from SQL onto the cache
            assert data != null;
            for (Settings.SynchronisationCluster cluster : data.keySet()) {
                HuskSyncVelocity.dataManager.playerDataCache.get(cluster).updatePlayer(data.get(cluster));
            }

            // Send a message asking the bukkit to request data on join
            try {
                new RedisMessage(RedisMessage.MessageType.REQUEST_DATA_ON_JOIN,
                        new RedisMessage.MessageTarget(Settings.ServerType.BUKKIT, null, null),
                        RedisMessage.RequestOnJoinUpdateType.ADD_REQUESTER.toString(), player.getUniqueId().toString()).send();
            } catch (IOException e) {
                plugin.getVelocityLogger().log(Level.SEVERE, "Failed to serialize request data on join message data");
                e.printStackTrace();
            }
        });
    }
}
