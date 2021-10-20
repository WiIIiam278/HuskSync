package me.william278.crossserversync.bukkit.data;

import java.util.HashMap;
import java.util.UUID;

public class LastDataUpdateUUIDCache {

    /**
     * Map of Player UUIDs to last-updated PlayerData version UUIDs
     */
    private static HashMap<UUID, UUID> lastUpdatedPlayerDataUUIDs;

    public LastDataUpdateUUIDCache() {
        lastUpdatedPlayerDataUUIDs = new HashMap<>();
    }

    public UUID getVersionUUID(UUID playerUUID) {
        return lastUpdatedPlayerDataUUIDs.get(playerUUID);
    }

    public void setVersionUUID(UUID playerUUID, UUID dataVersionUUID) {
        lastUpdatedPlayerDataUUIDs.put(playerUUID, dataVersionUUID);
    }

}
