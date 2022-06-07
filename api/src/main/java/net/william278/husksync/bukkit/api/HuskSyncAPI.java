package net.william278.husksync.bukkit.api;

import net.william278.husksync.PlayerData;
import net.william278.husksync.Settings;
import net.william278.husksync.bukkit.listener.BukkitRedisListener;
import net.william278.husksync.redis.RedisMessage;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * HuskSync's API. To access methods, use the {@link #getInstance()} entrypoint.
 *
 * @author William
 */
public class HuskSyncAPI {

    private HuskSyncAPI() {
    }

    private static HuskSyncAPI instance;

    /**
     * The API entry point. Returns an instance of the {@link HuskSyncAPI}
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
     * Returns a {@link CompletableFuture} that will fetch the {@link PlayerData} for a user given their {@link UUID},
     * which contains serialized synchronised data.
     * <p>
     * This can then be deserialized into ItemStacks and other usable values using the {@code DataSerializer} class.
     * <p>
     * If no data could be returned, such as if an invalid UUID is specified, the CompletableFuture will be cancelled.
     *
     * @param playerUUID The {@link UUID} of the player to get data for
     * @return a {@link CompletableFuture} with the user's {@link PlayerData} accessible on completion
     * @throws IOException If an exception occurs with serializing during processing of the request
     * @apiNote This only returns the latest saved and cached data of the user. This is <b>not</b> necessarily the current state of their inventory if they are online.
     */
    public CompletableFuture<PlayerData> getPlayerData(UUID playerUUID) throws IOException {
        // Create the request to be completed
        final UUID requestUUID = UUID.randomUUID();
        BukkitRedisListener.apiRequests.put(requestUUID, new CompletableFuture<>());

        // Remove the request from the map on completion
        BukkitRedisListener.apiRequests.get(requestUUID).whenComplete((playerData, throwable) -> BukkitRedisListener.apiRequests.remove(requestUUID));

        // Request the data via the proxy
        new RedisMessage(RedisMessage.MessageType.API_DATA_REQUEST,
                new RedisMessage.MessageTarget(Settings.ServerType.PROXY, null, Settings.cluster),
                playerUUID.toString(), requestUUID.toString()).send();

        return BukkitRedisListener.apiRequests.get(requestUUID);
    }

    /**
     * Updates a player's {@link PlayerData} to the proxy cache and database.
     * <p>
     * If the player is online on the Proxy network, they will be updated and overwritten with this data.
     *
     * @param playerData The {@link PlayerData} (which contains the {@link UUID}) of the player data to update to the central cache and database
     * @throws IOException If an exception occurs with serializing during processing of the update
     */
    public void updatePlayerData(PlayerData playerData) throws IOException {
        // Serialize and send the updated player data
        final String serializedPlayerData = RedisMessage.serialize(playerData);
        new RedisMessage(RedisMessage.MessageType.PLAYER_DATA_UPDATE,
                new RedisMessage.MessageTarget(Settings.ServerType.PROXY, null, Settings.cluster),
                serializedPlayerData, Boolean.toString(true)).send();
    }

}