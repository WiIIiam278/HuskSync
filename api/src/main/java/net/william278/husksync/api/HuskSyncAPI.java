package net.william278.husksync.api;

import net.william278.husksync.BukkitHuskSync;
import net.william278.husksync.data.*;
import net.william278.husksync.player.BukkitPlayer;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.player.User;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The HuskSync API for the Bukkit platform, providing methods to access and modify player {@link UserData} held by {@link User}s.
 * </p>
 * Retrieve an instance of the API class via {@link #getInstance()}.
 */
@SuppressWarnings("unused")
public class HuskSyncAPI {

    /**
     * <b>(Internal use only)</b> - Instance of the API class.
     */
    private static final HuskSyncAPI INSTANCE = new HuskSyncAPI();
    /**
     * <b>(Internal use only)</b> - Instance of the implementing plugin.
     */
    private static final BukkitHuskSync PLUGIN = BukkitHuskSync.getInstance();

    /**
     * <b>(Internal use only)</b> - Constructor.
     */
    private HuskSyncAPI() {
    }

    /**
     * Entrypoint to the HuskSync API - returns an instance of the API
     *
     * @return instance of the HuskSync API
     */
    public static @NotNull HuskSyncAPI getInstance() {
        return INSTANCE;
    }

    /**
     * Returns a {@link User} instance for the given bukkit {@link Player}.
     *
     * @param player the bukkit player to get the {@link User} instance for
     * @return the {@link User} instance for the given bukkit player
     */
    @NotNull
    public OnlineUser getUser(@NotNull Player player) {
        return BukkitPlayer.adapt(player);
    }

    /**
     * Returns a {@link User} by the given player's account {@link UUID}, if they exist.
     *
     * @param uuid the unique id of the player to get the {@link User} instance for
     * @return future returning the {@link User} instance for the given player's unique id if they exist, otherwise an empty {@link Optional}
     * @apiNote The player does not have to be online
     */
    public CompletableFuture<Optional<User>> getUser(@NotNull UUID uuid) {
        return PLUGIN.getDatabase().getUser(uuid);
    }

    /**
     * Returns a {@link User} by the given player's username (case-insensitive), if they exist.
     *
     * @param username the username of the {@link User} instance for
     * @return future returning the {@link User} instance for the given player's username if they exist, otherwise an empty {@link Optional}
     * @apiNote The player does not have to be online, though their username has to be the username
     * they had when they last joined the server.
     */
    public CompletableFuture<Optional<User>> getUser(@NotNull String username) {
        return PLUGIN.getDatabase().getUserByName(username);
    }

    /**
     * Returns a {@link User}'s current {@link UserData}
     *
     * @param user the {@link User} to get the {@link UserData} for
     * @return future returning the {@link UserData} for the given {@link User} if they exist, otherwise an empty {@link Optional}
     * @apiNote If the user is not online on the implementing bukkit server,
     * the {@link UserData} returned will be their last database-saved UserData.</p>
     * If the user happens to be online on another server on the network,
     * then the {@link UserData} returned here may not be reflective of their actual current UserData.
     */
    public CompletableFuture<Optional<UserData>> getUserData(@NotNull User user) {
        return CompletableFuture.supplyAsync(() -> {
            if (user instanceof OnlineUser) {
                return Optional.of(((OnlineUser) user).getUserData().join());
            } else {
                return PLUGIN.getDatabase().getCurrentUserData(user).join().map(VersionedUserData::userData);
            }
        });
    }

    /**
     * Returns the saved {@link VersionedUserData} records for the given {@link User}
     *
     * @param user the {@link User} to get the {@link VersionedUserData} for
     * @return future returning a list {@link VersionedUserData} for the given {@link User} if they exist,
     * otherwise an empty {@link Optional}
     * @apiNote The length of the list of VersionedUserData will correspond to the configured
     * {@code max_user_data_records} config option
     */
    public CompletableFuture<List<VersionedUserData>> getSavedUserData(@NotNull User user) {
        return CompletableFuture.supplyAsync(() -> PLUGIN.getDatabase().getUserData(user).join());
    }

    /**
     * Returns the JSON string representation of the given {@link UserData}
     *
     * @param userData    the {@link UserData} to get the JSON string representation of
     * @param prettyPrint whether to pretty print the JSON string
     * @return the JSON string representation of the given {@link UserData}
     */
    @NotNull
    public String getUserDataJson(@NotNull UserData userData, boolean prettyPrint) {
        return PLUGIN.getDataAdapter().toJson(userData, prettyPrint);
    }

    /**
     * Returns a {@link BukkitInventoryMap} for the given {@link User}, containing their current inventory item data
     *
     * @param user the {@link User} to get the {@link BukkitInventoryMap} for
     * @return future returning the {@link BukkitInventoryMap} for the given {@link User} if they exist,
     * otherwise an empty {@link Optional}
     */
    public CompletableFuture<Optional<BukkitInventoryMap>> getPlayerInventory(@NotNull User user) {
        return CompletableFuture.supplyAsync(() -> getUserData(user).join()
                .map(userData -> BukkitSerializer.deserializeInventory(userData
                        .getInventoryData().serializedItems).join()));
    }

    /**
     * Returns the {@link ItemStack}s array contents of the given {@link User}'s Ender Chest data
     *
     * @param user the {@link User} to get the Ender Chest contents of
     * @return future returning the {@link ItemStack} array of Ender Chest items for the user if they exist,
     * otherwise an empty {@link Optional}
     */
    public CompletableFuture<Optional<ItemStack[]>> getPlayerEnderChest(@NotNull User user) {
        return CompletableFuture.supplyAsync(() -> getUserData(user).join()
                .map(userData -> BukkitSerializer.deserializeItemStackArray(userData
                        .getEnderChestData().serializedItems).join()));
    }

    /**
     * Deserialize a Base-64 encoded inventory array string into a {@link ItemStack} array.
     *
     * @param serializedItemStackArray The Base-64 encoded inventory array string.
     * @return The deserialized {@link ItemStack} array.
     * @throws DataDeserializationException If an error occurs during deserialization.
     */
    public CompletableFuture<ItemStack[]> deserializeItemStackArray(@NotNull String serializedItemStackArray)
            throws DataDeserializationException {
        return CompletableFuture.supplyAsync(() -> BukkitSerializer
                .deserializeItemStackArray(serializedItemStackArray).join());
    }

    /**
     * Deserialize a Base-64 encoded potion effect array string into a {@link PotionEffect} array.
     *
     * @param serializedPotionEffectArray The Base-64 encoded potion effect array string.
     * @return The deserialized {@link PotionEffect} array.
     * @throws DataDeserializationException If an error occurs during deserialization.
     */
    public CompletableFuture<PotionEffect[]> deserializePotionEffectArray(@NotNull String serializedPotionEffectArray)
            throws DataDeserializationException {
        return CompletableFuture.supplyAsync(() -> BukkitSerializer
                .deserializePotionEffects(serializedPotionEffectArray).join());
    }

}
