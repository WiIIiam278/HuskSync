package me.william278.crossserversync.bungeecord.listener;

import me.william278.crossserversync.CrossServerSyncBungeeCord;
import me.william278.crossserversync.PlayerData;
import me.william278.crossserversync.bungeecord.data.DataManager;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

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
        });
    }

}
