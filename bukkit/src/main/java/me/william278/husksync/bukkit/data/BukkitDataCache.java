package me.william278.husksync.bukkit.data;

import java.util.HashSet;
import java.util.UUID;

public class BukkitDataCache {

    /**
     * Map of Player UUIDs to request on join
     */
    private static HashSet<UUID> requestOnJoin;

    public BukkitDataCache() {
        requestOnJoin = new HashSet<>();
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
