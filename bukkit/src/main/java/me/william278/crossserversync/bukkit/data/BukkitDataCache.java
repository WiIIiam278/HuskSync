package me.william278.crossserversync.bukkit.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class BukkitDataCache {

    /**
     * Map of Player UUIDs to last-updated PlayerData version UUIDs
     */
    private static HashMap<UUID, UUID> bukkitDataCache;

    /**
     * Map of Player UUIDs to request on join
     */
    private static HashSet<UUID> requestOnJoin;

    public BukkitDataCache() {
        bukkitDataCache = new HashMap<>();
        requestOnJoin = new HashSet<>();
    }

    public UUID getVersionUUID(UUID playerUUID) {
        return bukkitDataCache.get(playerUUID);
    }

    public void setVersionUUID(UUID playerUUID, UUID dataVersionUUID) {
        bukkitDataCache.put(playerUUID, dataVersionUUID);
    }

    public boolean isPlayerRequestingOnJoin(UUID uuid) {
        return requestOnJoin.contains(uuid);
    }

    public void setRequestOnJoin(UUID uuid) {
        requestOnJoin.add(uuid);
    }

    public void removeRequestOnJoin(UUID uuid) {
        requestOnJoin.remove(uuid);
    }
}
