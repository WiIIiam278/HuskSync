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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * The HuskSync API implementation for the Bukkit platform, providing methods to access and modify player {@link UserData} held by {@link User}s.
 * </p>
 * Retrieve an instance of the API class via {@link #getInstance()}.
 */
@SuppressWarnings("unused")
public class HuskSyncAPI extends BaseHuskSyncAPI {

    /**
     * <b>(Internal use only)</b> - Instance of the API class
     */
    private static final HuskSyncAPI INSTANCE = new HuskSyncAPI();

    /**
     * <b>(Internal use only)</b> - Constructor, instantiating the API
     */
    private HuskSyncAPI() {
        super(BukkitHuskSync.getInstance());
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
     * @return the {@link User} instance for the given bukkit {@link Player}
     * @since 2.0
     */
    @NotNull
    public OnlineUser getUser(@NotNull Player player) {
        return BukkitPlayer.adapt(player);
    }

    /**
     * Set the inventory in the database of the given {@link User} to the given {@link ItemStack} contents
     *
     * @param user              the {@link User} to set the inventory of
     * @param inventoryContents the {@link ItemStack} contents to set the inventory to
     * @return future returning void when complete
     * @since 2.0
     */
    public CompletableFuture<Void> setInventoryData(@NotNull User user, @NotNull ItemStack[] inventoryContents) {
        return CompletableFuture.runAsync(() -> getUserData(user).thenAccept(userData ->
                userData.ifPresent(data -> serializeItemStackArray(inventoryContents)
                        .thenAccept(serializedInventory -> {
                            data.getInventory().orElse(ItemData.empty()).serializedItems = serializedInventory;
                            setUserData(user, data).join();
                        }))));
    }

    /**
     * Set the inventory in the database of the given {@link User} to the given {@link BukkitInventoryMap} contents
     *
     * @param user         the {@link User} to set the inventory of
     * @param inventoryMap the {@link BukkitInventoryMap} contents to set the inventory to
     * @return future returning void when complete
     * @since 2.0
     */
    public CompletableFuture<Void> setInventoryData(@NotNull User user, @NotNull BukkitInventoryMap inventoryMap) {
        return setInventoryData(user, inventoryMap.getContents());
    }

    /**
     * Set the Ender Chest in the database of the given {@link User} to the given {@link ItemStack} contents
     *
     * @param user               the {@link User} to set the Ender Chest of
     * @param enderChestContents the {@link ItemStack} contents to set the Ender Chest to
     * @return future returning void when complete
     * @since 2.0
     */
    public CompletableFuture<Void> setEnderChestData(@NotNull User user, @NotNull ItemStack[] enderChestContents) {
        return CompletableFuture.runAsync(() -> getUserData(user).thenAccept(userData ->
                userData.ifPresent(data -> serializeItemStackArray(enderChestContents)
                        .thenAccept(serializedInventory -> {
                            data.getEnderChest().orElse(ItemData.empty()).serializedItems = serializedInventory;
                            setUserData(user, data).join();
                        }))));
    }

    /**
     * Returns a {@link BukkitInventoryMap} for the given {@link User}, containing their current inventory item data
     *
     * @param user the {@link User} to get the {@link BukkitInventoryMap} for
     * @return future returning the {@link BukkitInventoryMap} for the given {@link User} if they exist,
     * otherwise an empty {@link Optional}
     * @apiNote If the {@link UserData} does not contain an inventory (i.e. inventory synchronisation is disabled), the
     * returned {@link BukkitInventoryMap} will be equivalent an empty inventory.
     * @since 2.0
     */
    public CompletableFuture<Optional<BukkitInventoryMap>> getPlayerInventory(@NotNull User user) {
        return CompletableFuture.supplyAsync(() -> getUserData(user).join()
                .map(userData -> deserializeInventory(userData.getInventory()
                        .orElse(ItemData.empty()).serializedItems).join()));
    }

    /**
     * Returns the {@link ItemStack}s array contents of the given {@link User}'s Ender Chest data
     *
     * @param user the {@link User} to get the Ender Chest contents of
     * @return future returning the {@link ItemStack} array of Ender Chest items for the user if they exist,
     * otherwise an empty {@link Optional}
     * @apiNote If the {@link UserData} does not contain an Ender Chest (i.e. Ender Chest synchronisation is disabled),
     * the returned {@link BukkitInventoryMap} will be equivalent to an empty inventory.
     * @since 2.0
     */
    public CompletableFuture<Optional<ItemStack[]>> getPlayerEnderChest(@NotNull User user) {
        return CompletableFuture.supplyAsync(() -> getUserData(user).join()
                .map(userData -> deserializeItemStackArray(userData.getEnderChest()
                        .orElse(ItemData.empty()).serializedItems).join()));
    }

    /**
     * Deserialize a Base-64 encoded inventory array string into a {@link ItemStack} array.
     *
     * @param serializedItemStackArray The Base-64 encoded inventory array string.
     * @return The deserialized {@link ItemStack} array.
     * @throws DataSerializationException If an error occurs during deserialization.
     * @since 2.0
     */
    public CompletableFuture<ItemStack[]> deserializeItemStackArray(@NotNull String serializedItemStackArray)
            throws DataSerializationException {
        return CompletableFuture.supplyAsync(() -> BukkitSerializer
                .deserializeItemStackArray(serializedItemStackArray).join());
    }

    /**
     * Deserialize a serialized {@link ItemStack} array of player inventory contents into a {@link BukkitInventoryMap}
     *
     * @param serializedInventory The serialized {@link ItemStack} array of player inventory contents.
     * @return A {@link BukkitInventoryMap} of the deserialized {@link ItemStack} contents array
     * @throws DataSerializationException If an error occurs during deserialization.
     * @since 2.0
     */
    public CompletableFuture<BukkitInventoryMap> deserializeInventory(@NotNull String serializedInventory)
            throws DataSerializationException {
        return CompletableFuture.supplyAsync(() -> BukkitSerializer
                .deserializeInventory(serializedInventory).join());
    }

    /**
     * Serialize an {@link ItemStack} array into a Base-64 encoded string.
     *
     * @param itemStacks The {@link ItemStack} array to serialize.
     * @return The serialized Base-64 encoded string.
     * @throws DataSerializationException If an error occurs during serialization.
     * @see #deserializeItemStackArray(String)
     * @see ItemData
     * @since 2.0
     */
    public CompletableFuture<String> serializeItemStackArray(@NotNull ItemStack[] itemStacks)
            throws DataSerializationException {
        return CompletableFuture.supplyAsync(() -> BukkitSerializer.serializeItemStackArray(itemStacks).join());
    }

    /**
     * Deserialize a Base-64 encoded potion effect array string into a {@link PotionEffect} array.
     *
     * @param serializedPotionEffectArray The Base-64 encoded potion effect array string.
     * @return The deserialized {@link PotionEffect} array.
     * @throws DataSerializationException If an error occurs during deserialization.
     * @since 2.0
     */
    public CompletableFuture<PotionEffect[]> deserializePotionEffectArray(@NotNull String serializedPotionEffectArray)
            throws DataSerializationException {
        return CompletableFuture.supplyAsync(() -> BukkitSerializer
                .deserializePotionEffectArray(serializedPotionEffectArray).join());
    }

    /**
     * Serialize a {@link PotionEffect} array into a Base-64 encoded string.
     *
     * @param potionEffects The {@link PotionEffect} array to serialize.
     * @return The serialized Base-64 encoded string.
     * @throws DataSerializationException If an error occurs during serialization.
     * @see #deserializePotionEffectArray(String)
     * @see PotionEffectData
     * @since 2.0
     */
    public CompletableFuture<String> serializePotionEffectArray(@NotNull PotionEffect[] potionEffects)
            throws DataSerializationException {
        return CompletableFuture.supplyAsync(() -> BukkitSerializer.serializePotionEffectArray(potionEffects).join());
    }

}
