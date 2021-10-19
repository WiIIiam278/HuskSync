package me.william278.crossserversync.bungeecord;

import me.william278.crossserversync.PlayerData;

import java.util.HashSet;
import java.util.UUID;

public class PlayerDataCache {

    // The cached player data
    public HashSet<PlayerData> playerData;

    public PlayerDataCache() {
        playerData = new HashSet<>();
    }

    /**
     * Update ar add data for a player to the cache
     * @param newData The player's new/updated {@link PlayerData}
     */
    public void updatePlayer(PlayerData newData) {
        // Remove the old data if it exists
        PlayerData oldData = null;
        for (PlayerData data : playerData) {
            if (data.getPlayerUUID() == newData.getPlayerUUID()) {
                oldData = data;
            }
        }
        if (oldData != null) {
            playerData.remove(oldData);
        }

        // Add the new data
        playerData.add(newData);
    }

    /**
     * Get a player's {@link PlayerData} by their {@link UUID}
     * @param playerUUID The {@link UUID} of the player to check
     * @return The player's {@link PlayerData}
     */
    public PlayerData getPlayer(UUID playerUUID) {
        for (PlayerData data : playerData) {
            if (data.getPlayerUUID() == playerUUID) {
                return data;
            }
        }
        return null;
    }

}
