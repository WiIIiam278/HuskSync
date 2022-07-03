package net.william278.husksync.player;

import de.themoep.minedown.MineDown;
import net.william278.husksync.config.Settings;
import net.william278.husksync.data.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a logged-in {@link User}
 */
public abstract class OnlineUser extends User {

    public OnlineUser(@NotNull UUID uuid, @NotNull String username) {
        super(uuid, username);
    }

    /**
     * Get the player's {@link StatusData}
     *
     * @return the player's {@link StatusData}
     */
    public abstract CompletableFuture<StatusData> getStatus();

    /**
     * Set the player's {@link StatusData}
     *
     * @param statusData    the player's {@link StatusData}
     * @param setHealth     whether to set the player's health
     * @param setMaxHealth  whether to set the player's max health
     * @param setHunger     whether to set the player's hunger
     * @param setExperience whether to set the player's experience
     * @param setGameMode   whether to set the player's game mode
     * @return a future returning void when complete
     */
    public abstract CompletableFuture<Void> setStatus(@NotNull StatusData statusData,
                                                      final boolean setHealth, final boolean setMaxHealth,
                                                      final boolean setHunger, final boolean setExperience,
                                                      final boolean setGameMode, final boolean setFlying,
                                                      final boolean setSelectedItemSlot);

    /**
     * Get the player's inventory {@link InventoryData} contents
     *
     * @return The player's inventory {@link InventoryData} contents
     */
    public abstract CompletableFuture<InventoryData> getInventory();

    /**
     * Set the player's {@link InventoryData}
     *
     * @param inventoryData The player's {@link InventoryData}
     * @return a future returning void when complete
     */
    public abstract CompletableFuture<Void> setInventory(@NotNull InventoryData inventoryData);

    /**
     * Get the player's ender chest {@link InventoryData} contents
     *
     * @return The player's ender chest {@link InventoryData} contents
     */
    public abstract CompletableFuture<InventoryData> getEnderChest();

    /**
     * Set the player's {@link InventoryData}
     *
     * @param enderChestData The player's {@link InventoryData}
     * @return a future returning void when complete
     */
    public abstract CompletableFuture<Void> setEnderChest(@NotNull InventoryData enderChestData);


    /**
     * Get the player's {@link PotionEffectData}
     *
     * @return The player's {@link PotionEffectData}
     */
    public abstract CompletableFuture<PotionEffectData> getPotionEffects();

    /**
     * Set the player's {@link PotionEffectData}
     *
     * @param potionEffectData The player's {@link PotionEffectData}
     * @return a future returning void when complete
     */
    public abstract CompletableFuture<Void> setPotionEffects(@NotNull PotionEffectData potionEffectData);

    /**
     * Get the player's set of {@link AdvancementData}
     *
     * @return the player's set of {@link AdvancementData}
     */
    public abstract CompletableFuture<List<AdvancementData>> getAdvancements();

    /**
     * Set the player's {@link AdvancementData}
     *
     * @param advancementData List of the player's {@link AdvancementData}
     * @return a future returning void when complete
     */
    public abstract CompletableFuture<Void> setAdvancements(@NotNull List<AdvancementData> advancementData);

    /**
     * Get the player's {@link StatisticsData}
     *
     * @return The player's {@link StatisticsData}
     */
    public abstract CompletableFuture<StatisticsData> getStatistics();

    /**
     * Set the player's {@link StatisticsData}
     *
     * @param statisticsData The player's {@link StatisticsData}
     * @return a future returning void when complete
     */
    public abstract CompletableFuture<Void> setStatistics(@NotNull StatisticsData statisticsData);

    /**
     * Get the player's {@link LocationData}
     *
     * @return the player's {@link LocationData}
     */
    public abstract CompletableFuture<LocationData> getLocation();

    /**
     * Set the player's {@link LocationData}
     *
     * @param locationData the player's {@link LocationData}
     * @return a future returning void when complete
     */
    public abstract CompletableFuture<Void> setLocation(@NotNull LocationData locationData);

    /**
     * Get the player's {@link PersistentDataContainerData}
     *
     * @return The player's {@link PersistentDataContainerData} when fetched
     */
    public abstract CompletableFuture<PersistentDataContainerData> getPersistentDataContainer();

    /**
     * Set the player's {@link PersistentDataContainerData}
     *
     * @param persistentDataContainerData The player's {@link PersistentDataContainerData} to set
     * @return A future returning void when complete
     */
    public abstract CompletableFuture<Void> setPersistentDataContainer(@NotNull PersistentDataContainerData persistentDataContainerData);

    /**
     * Indicates if the player is currently dead
     *
     * @return {@code true} if the player is dead (health <= 0); {@code false} otherwise
     */
    public abstract boolean isDead();

    /**
     * Indicates if the player has gone offline
     *
     * @return {@code true} if the player has left the server; {@code false} otherwise
     */
    public abstract boolean isOffline();

    /**
     * Set {@link UserData} to a player
     *
     * @param data     The data to set
     * @param settings Plugin settings, for determining what needs setting
     * @return a future that will be completed when done
     */
    public final CompletableFuture<Void> setData(@NotNull UserData data, @NotNull Settings settings) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Don't set offline players
                if (isOffline()) {
                    return;
                }
                // Don't set dead players
                if (isDead()) {
                    return;
                }
                if (settings.getBooleanValue(Settings.ConfigOption.SYNCHRONIZATION_SYNC_INVENTORIES)) {
                    setInventory(data.getInventoryData()).join();
                }
                if (settings.getBooleanValue(Settings.ConfigOption.SYNCHRONIZATION_SYNC_ENDER_CHESTS)) {
                    setEnderChest(data.getEnderChestData()).join();
                }
                setStatus(data.getStatusData(), settings.getBooleanValue(Settings.ConfigOption.SYNCHRONIZATION_SYNC_HEALTH),
                        settings.getBooleanValue(Settings.ConfigOption.SYNCHRONIZATION_SYNC_MAX_HEALTH),
                        settings.getBooleanValue(Settings.ConfigOption.SYNCHRONIZATION_SYNC_HUNGER),
                        settings.getBooleanValue(Settings.ConfigOption.SYNCHRONIZATION_SYNC_EXPERIENCE),
                        settings.getBooleanValue(Settings.ConfigOption.SYNCHRONIZATION_SYNC_GAME_MODE),
                        settings.getBooleanValue(Settings.ConfigOption.SYNCHRONIZATION_SYNC_LOCATION),
                        settings.getBooleanValue(Settings.ConfigOption.SYNCHRONIZATION_SYNC_INVENTORIES)).join();
                if (settings.getBooleanValue(Settings.ConfigOption.SYNCHRONIZATION_SYNC_POTION_EFFECTS)) {
                    setPotionEffects(data.getPotionEffectData()).join();
                }
                if (settings.getBooleanValue(Settings.ConfigOption.SYNCHRONIZATION_SYNC_ADVANCEMENTS)) {
                    setAdvancements(data.getAdvancementData()).join();
                }
                if (settings.getBooleanValue(Settings.ConfigOption.SYNCHRONIZATION_SYNC_STATISTICS)) {
                    setStatistics(data.getStatisticData()).join();
                }
                if (settings.getBooleanValue(Settings.ConfigOption.SYNCHRONIZATION_SYNC_PERSISTENT_DATA_CONTAINER)) {
                    setPersistentDataContainer(data.getPersistentDataContainerData()).join();
                }
                if (settings.getBooleanValue(Settings.ConfigOption.SYNCHRONIZATION_SYNC_LOCATION)) {
                    setLocation(data.getLocationData()).join();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Dispatch a MineDown-formatted message to this player
     *
     * @param mineDown the parsed {@link MineDown} to send
     */
    public abstract void sendMessage(@NotNull MineDown mineDown);

    /**
     * Dispatch a MineDown-formatted action bar message to this player
     *
     * @param mineDown the parsed {@link MineDown} to send
     */
    public abstract void sendActionBar(@NotNull MineDown mineDown);

    /**
     * Returns if the player has the permission node
     *
     * @param node The permission node string
     * @return {@code true} if the player has permission node; {@code false} otherwise
     */
    public abstract boolean hasPermission(@NotNull String node);

    /**
     * Get the player's current {@link UserData}
     *
     * @return the player's current {@link UserData}
     */
    public final CompletableFuture<VersionedUserData> getUserData() {
        return CompletableFuture.supplyAsync(
                () -> VersionedUserData.version(new UserData(getStatus().join(), getInventory().join(),
                        getEnderChest().join(), getPotionEffects().join(), getAdvancements().join(),
                        getStatistics().join(), getLocation().join(), getPersistentDataContainer().join())));
    }

}
