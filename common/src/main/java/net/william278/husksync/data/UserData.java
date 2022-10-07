package net.william278.husksync.data;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Stores data about a user
 */
public class UserData {

    /**
     * Indicates the version of the {@link UserData} format being used.
     * </p>
     * This value is to be incremented whenever the format changes.
     */
    public static final int CURRENT_FORMAT_VERSION = 3;

    /**
     * Stores the user's status data, including health, food, etc.
     */
    @SerializedName("status")
    @Nullable
    protected StatusData statusData;

    /**
     * Stores the user's inventory contents
     */
    @SerializedName("inventory")
    @Nullable
    protected ItemData inventoryData;

    /**
     * Stores the user's ender chest contents
     */
    @SerializedName("ender_chest")
    @Nullable
    protected ItemData enderChestData;

    /**
     * Store's the user's potion effects
     */
    @SerializedName("potion_effects")
    @Nullable
    protected PotionEffectData potionEffectData;

    /**
     * Stores the set of this user's advancements
     */
    @SerializedName("advancements")
    @Nullable
    protected List<AdvancementData> advancementData;

    /**
     * Stores the user's set of statistics
     */
    @SerializedName("statistics")
    @Nullable
    protected StatisticsData statisticData;

    /**
     * Store's the user's world location and coordinates
     */
    @SerializedName("location")
    @Nullable
    protected LocationData locationData;

    /**
     * Stores the user's serialized persistent data container, which contains metadata keys applied by other plugins
     */
    @SerializedName("persistent_data_container")
    @Nullable
    protected PersistentDataContainerData persistentDataContainerData;

    /**
     * Stores the version of Minecraft this data was generated in
     */
    @SerializedName("minecraft_version")
    @NotNull
    protected String minecraftVersion;

    /**
     * Stores the version of the data format being used
     */
    @SerializedName("format_version")
    protected int formatVersion;

    public UserData(@NotNull StatusData statusData, @NotNull ItemData inventoryData,
                    @NotNull ItemData enderChestData, @NotNull PotionEffectData potionEffectData,
                    @NotNull List<AdvancementData> advancementData, @NotNull StatisticsData statisticData,
                    @NotNull LocationData locationData, @NotNull PersistentDataContainerData persistentDataContainerData,
                    @NotNull String minecraftVersion) {
        this.statusData = statusData;
        this.inventoryData = inventoryData;
        this.enderChestData = enderChestData;
        this.potionEffectData = potionEffectData;
        this.advancementData = advancementData;
        this.statisticData = statisticData;
        this.locationData = locationData;
        this.persistentDataContainerData = persistentDataContainerData;
        this.minecraftVersion = minecraftVersion;
        this.formatVersion = CURRENT_FORMAT_VERSION;
    }

    // Empty constructor to facilitate json serialization
    @SuppressWarnings("unused")
    protected UserData() {
    }

    /**
     * Gets the {@link StatusData} from this user data
     *
     * @return the {@link StatusData} of this user data
     * @since 2.0
     * @deprecated Use {@link #getStatus()}, which returns an optional instead
     */
    @Nullable
    @Deprecated(since = "2.1")
    public StatusData getStatusData() {
        return statusData;
    }

    /**
     * Gets the {@link StatusData} from this user data
     *
     * @return an optional containing the {@link StatusData} if it is present in this user data
     * @since 2.1
     */
    public Optional<StatusData> getStatus() {
        return Optional.ofNullable(statusData);
    }

    /**
     * Gets the {@link ItemData} representing the player's inventory from this user data
     *
     * @return the inventory {@link ItemData} of this user data
     * @since 2.0
     * @deprecated Use {@link #getInventory()}, which returns an optional instead
     */
    @Nullable
    @Deprecated(since = "2.1")
    public ItemData getInventoryData() {
        return inventoryData;
    }

    /**
     * Gets the {@link ItemData} representing the player's inventory from this user data
     *
     * @return an optional containing the inventory {@link ItemData} if it is present in this user data
     * @since 2.1
     */
    public Optional<ItemData> getInventory() {
        return Optional.ofNullable(inventoryData);
    }

    /**
     * Gets the {@link ItemData} representing the player's ender chest from this user data
     *
     * @return the ender chest {@link ItemData} of this user data
     * @since 2.0
     * @deprecated Use {@link #getEnderChest()}, which returns an optional instead
     */
    @Nullable
    @Deprecated(since = "2.1")
    public ItemData getEnderChestData() {
        return enderChestData;
    }

    /**
     * Gets the {@link ItemData} representing the player's ender chest from this user data
     *
     * @return an optional containing the ender chest {@link ItemData} if it is present in this user data
     * @since 2.1
     */
    public Optional<ItemData> getEnderChest() {
        return Optional.ofNullable(enderChestData);
    }

    /**
     * Gets the {@link PotionEffectData} representing player status effects from this user data
     *
     * @return the {@link PotionEffectData} of this user data
     * @since 2.0
     * @deprecated Use {@link #getPotionEffects()}, which returns an optional instead
     */
    @Nullable
    @Deprecated(since = "2.1")
    public PotionEffectData getPotionEffectsData() {
        return potionEffectData;
    }

    /**
     * Gets the {@link PotionEffectData} representing the player's potion effects from this user data
     *
     * @return an optional containing {@link PotionEffectData} if it is present in this user data
     * @since 2.1
     */
    public Optional<PotionEffectData> getPotionEffects() {
        return Optional.ofNullable(potionEffectData);
    }

    /**
     * Gets the list of {@link AdvancementData} from this user data
     *
     * @return the {@link AdvancementData} of this user data
     * @since 2.0
     * @deprecated Use {@link #getAdvancements()}, which returns an optional instead
     */
    @Nullable
    @Deprecated(since = "2.1")
    public List<AdvancementData> getAdvancementData() {
        return advancementData;
    }

    /**
     * Gets a list of {@link AdvancementData} representing the player's advancements from this user data
     *
     * @return an optional containing a {@link List} of {@link AdvancementData} if it is present in this user data
     * @since 2.1
     */
    public Optional<List<AdvancementData>> getAdvancements() {
        return Optional.ofNullable(advancementData);
    }

    /**
     * Gets the {@link StatisticsData} representing player statistics from this user data
     *
     * @return the {@link StatisticsData} of this user data
     * @since 2.0
     * @deprecated Use {@link #getStatistics()}, which returns an optional instead
     */
    @Nullable
    @Deprecated(since = "2.1")
    public StatisticsData getStatisticsData() {
        return statisticData;
    }

    /**
     * Gets {@link StatisticsData} representing player statistics from this user data
     *
     * @return an optional containing player {@link StatisticsData} if it is present in this user data
     * @since 2.1
     */
    public Optional<StatisticsData> getStatistics() {
        return Optional.ofNullable(statisticData);
    }

    /**
     * Gets the {@link LocationData} representing the player location from this user data
     *
     * @return the inventory {@link LocationData} of this user data
     * @since 2.0
     * @deprecated Use {@link #getLocation()}, which returns an optional instead
     */
    @Nullable
    @Deprecated(since = "2.1")
    public LocationData getLocationData() {
        return locationData;
    }

    /**
     * Gets {@link LocationData} representing the player location from this user data
     *
     * @return an optional containing player {@link LocationData} if it is present in this user data
     * @since 2.1
     */
    public Optional<LocationData> getLocation() {
        return Optional.ofNullable(locationData);
    }

    /**
     * Gets the {@link PersistentDataContainerData} from this user data
     *
     * @return the {@link PersistentDataContainerData} of this user data
     * @since 2.0
     * @deprecated Use {@link #getPersistentDataContainer()}, which returns an optional instead
     */
    @Nullable
    @Deprecated(since = "2.1")
    public PersistentDataContainerData getPersistentDataContainerData() {
        return persistentDataContainerData;
    }

    /**
     * Gets {@link PersistentDataContainerData} from this user data
     *
     * @return an optional containing the player's {@link PersistentDataContainerData} if it is present in this user data
     * @since 2.1
     */
    public Optional<PersistentDataContainerData> getPersistentDataContainer() {
        return Optional.ofNullable(persistentDataContainerData);
    }

    /**
     * Get the version of Minecraft this data was generated in
     *
     * @return the version of Minecraft this data was generated in
     */
    @NotNull
    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    /**
     * Gets the version of the data format being used
     *
     * @return the version of the data format being used
     */
    public int getFormatVersion() {
        return formatVersion;
    }

}
