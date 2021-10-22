package me.william278.crossserversync.bukkit.listener;

import me.william278.crossserversync.CrossServerSyncBukkit;
import me.william278.crossserversync.PlayerData;
import me.william278.crossserversync.Settings;
import me.william278.crossserversync.bukkit.DataSerializer;
import me.william278.crossserversync.redis.RedisMessage;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

public class EventListener implements Listener {

    private static final CrossServerSyncBukkit plugin = CrossServerSyncBukkit.getInstance();

    /**
     * Returns the new serialized PlayerData for a player.
     * @param player The {@link Player} to get the new serialized PlayerData for
     * @return The {@link PlayerData}, serialized as a {@link String}
     * @throws IOException If the serialization fails
     */
    private static String getNewSerializedPlayerData(Player player) throws IOException {
        return RedisMessage.serialize(new PlayerData(player.getUniqueId(),
                DataSerializer.getSerializedInventoryContents(player),
                DataSerializer.getSerializedEnderChestContents(player),
                player.getHealth(),
                Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue(),
                player.getFoodLevel(),
                player.getSaturation(),
                player.getExhaustion(),
                player.getInventory().getHeldItemSlot(),
                DataSerializer.getSerializedEffectData(player),
                player.getTotalExperience(),
                player.getLevel(),
                player.getExp(),
                player.getGameMode().toString(),
                DataSerializer.getSerializedStatisticData(player)));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // When a player leaves a Bukkit server
        final Player player = event.getPlayer();

        try {
            // Get the player's last updated PlayerData version UUID
            final UUID lastUpdatedDataVersion = CrossServerSyncBukkit.bukkitCache.getVersionUUID(player.getUniqueId());
            if (lastUpdatedDataVersion == null) return; // Return if the player has not been properly updated.

            // Send a redis message with the player's last updated PlayerData version UUID and their new PlayerData
            final String serializedPlayerData = getNewSerializedPlayerData(player);
            new RedisMessage(RedisMessage.MessageType.PLAYER_DATA_UPDATE,
                    new RedisMessage.MessageTarget(Settings.ServerType.BUNGEECORD, null),
                    serializedPlayerData).send();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to send a PlayerData update to the proxy", e);
        }
    }

   @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // When a player joins a Bukkit server
        final Player player = event.getPlayer();

        if (CrossServerSyncBukkit.bukkitCache.isPlayerRequestingOnJoin(player.getUniqueId())) {
            try {
                // Send a redis message requesting the player data
                new RedisMessage(RedisMessage.MessageType.PLAYER_DATA_REQUEST,
                        new RedisMessage.MessageTarget(Settings.ServerType.BUNGEECORD, null),
                        player.getUniqueId().toString()).send();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to send a PlayerData fetch request", e);
            }
        }
    }
}
