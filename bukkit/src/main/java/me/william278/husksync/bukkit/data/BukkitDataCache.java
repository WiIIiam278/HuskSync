package me.william278.husksync.bukkit.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class BukkitDataCache {

    /**
     * Map of Player UUIDs to request on join
     */
    private static HashSet<UUID> requestOnJoin;

    public boolean isPlayerRequestingOnJoin(UUID uuid) {
        return requestOnJoin.contains(uuid);
    }

    public void setRequestOnJoin(UUID uuid) {
        requestOnJoin.add(uuid);
    }

    public void removeRequestOnJoin(UUID uuid) {
        requestOnJoin.remove(uuid);
    }

    /**
     * Map of Player UUIDs whose data has not been set yet
     */
    private static HashSet<UUID> awaitingDataFetch;

    public boolean isAwaitingDataFetch(UUID uuid) {
        return awaitingDataFetch.contains(uuid);
    }

    public void setAwaitingDataFetch(UUID uuid) {
        awaitingDataFetch.add(uuid);
    }

    public void removeAwaitingDataFetch(UUID uuid) {
        awaitingDataFetch.remove(uuid);
    }

    public HashSet<UUID> getAwaitingDataFetch() {
        return awaitingDataFetch;
    }

    /**
     * Map of data being viewed by players
     */
    private static HashMap<UUID, DataViewer.DataView> viewingPlayerData;

    public void setViewing(UUID uuid, DataViewer.DataView dataView) {
        viewingPlayerData.put(uuid, dataView);
    }

    public void removeViewing(UUID uuid) {
        viewingPlayerData.remove(uuid);
    }

    public boolean isViewing(UUID uuid) {
        return viewingPlayerData.containsKey(uuid);
    }

    public DataViewer.DataView getViewing(UUID uuid) {
        return viewingPlayerData.get(uuid);
    }

    // Cache object
    public BukkitDataCache() {
        requestOnJoin = new HashSet<>();
        viewingPlayerData = new HashMap<>();
        awaitingDataFetch = new HashSet<>();
    }
}