package me.william278.husksync.bukkit.api;

import me.william278.husksync.PlayerData;
import me.william278.husksync.Settings;
import me.william278.husksync.redis.RedisMessage;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * API method class for HuskSync. To access methods, use the {@link #getInstance()} entrypoint.
 */
public class HuskSyncAPI {

    private HuskSyncAPI() {
    }

    private static HuskSyncAPI instance;

    /**
     * API entry point. Returns an instance of the {@link HuskSyncAPI}
     *
     * @return instance of the {@link HuskSyncAPI}
     */
    public static HuskSyncAPI getInstance() {
        if (instance == null) {
            instance = new HuskSyncAPI();
        }
        return instance;
    }

    /**
     * (INTERNAL) Map of API requests that are processed by the bukkit plugin that implements the API.
     */
    public static HashMap<UUID, CompletableFuture<PlayerData>> apiRequests = new HashMap<>();

    /**
     * Returns a {@link CompletableFuture} that will fetch the {@link PlayerData} for a user given their {@link UUID}, which contains synchronised data that can then be deserialized into ItemStacks and other usable values using the {@link me.william278.husksync.bukkit.data.DataSerializer} class. If no data could be returned, such as if an invalid UUID is specified, the CompletableFuture will be cancelled. Note that this only returns the last cached data of the user; not necessarily the current state of their inventory if they are online.
     *
     * @param playerUUID The {@link UUID} of the player to get data for
     * @return a {@link CompletableFuture} with the user's {@link PlayerData} accessible on completion
     * @throws IOException If an exception occurs with serializing during processing of the request
     */
    public CompletableFuture<PlayerData> getPlayerData(UUID playerUUID) throws IOException {
        // Create the request to be completed
        final UUID requestUUID = UUID.randomUUID();
        apiRequests.put(requestUUID,  new CompletableFuture<>());

        // Remove the request from the map on completion
        apiRequests.get(requestUUID).whenComplete((playerData, throwable) -> apiRequests.remove(requestUUID));

        // Request the data via the proxy
        new RedisMessage(RedisMessage.MessageType.API_DATA_REQUEST,
                new RedisMessage.MessageTarget(Settings.ServerType.PROXY, null, Settings.cluster),
                playerUUID.toString(), requestUUID.toString()).send();

        return apiRequests.get(requestUUID);
    }

    /**
     * Updates a player's {@link PlayerData} to the central cache and database. If the player is online on the Proxy network, they will be updated and overwritten with this data.
     *
     * @param playerData The {@link PlayerData} (which contains the {@link UUID}) of the player data to update to the central cache and database
     * @throws IOException If an exception occurs with serializing during processing of the update
     */
    public void updatePlayerData(PlayerData playerData) throws IOException {
        // Serialize and send the updated player data
        final String serializedPlayerData = RedisMessage.serialize(playerData);
        new RedisMessage(RedisMessage.MessageType.PLAYER_DATA_UPDATE,
                new RedisMessage.MessageTarget(Settings.ServerType.PROXY, null, Settings.cluster),
                serializedPlayerData).send();
    }

}