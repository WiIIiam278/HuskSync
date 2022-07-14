package net.william278.husksync.data;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/***
 * Stores data about a user
 */
public class UserData {

    /**
     * Indicates the version of the {@link UserData} format being used.
     * </p>
     * This value is to be incremented whenever the format changes.
     */
    public static final int CURRENT_FORMAT_VERSION = 1;

    /**
     * Stores the user's status data, including health, food, etc.
     */
    @SerializedName("status")
    protected StatusData statusData;

    /**
     * Stores the user's inventory contents
     */
    @SerializedName("inventory")
    protected ItemData inventoryData;

    /**
     * Stores the user's ender chest contents
     */
    @SerializedName("ender_chest")
    protected ItemData enderChestData;

    /**
     * Store's the user's potion effects
     */
    @SerializedName("potion_effects")
    protected PotionEffectData potionEffectData;

    /**
     * Stores the set of this user's advancements
     */
    @SerializedName("advancements")
    protected List<AdvancementData> advancementData;

    /**
     * Stores the user's set of statistics
     */
    @SerializedName("statistics")
    protected StatisticsData statisticData;

    /**
     * Store's the user's world location and coordinates
     */
    @SerializedName("location")
    protected LocationData locationData;

    /**
     * Stores the user's serialized persistent data container, which contains metadata keys applied by other plugins
     */
    @SerializedName("persistent_data_container")
    protected PersistentDataContainerData persistentDataContainerData;

    /**
     * Stores the version of Minecraft this data was generated in
     */
    @SerializedName("minecraft_version")
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

    public StatusData getStatusData() {
        return statusData;
    }

    public ItemData getInventoryData() {
        return inventoryData;
    }

    public ItemData getEnderChestData() {
        return enderChestData;
    }

    public PotionEffectData getPotionEffectsData() {
        return potionEffectData;
    }

    public List<AdvancementData> getAdvancementData() {
        return advancementData;
    }

    public StatisticsData getStatisticsData() {
        return statisticData;
    }

    public LocationData getLocationData() {
        return locationData;
    }

    public PersistentDataContainerData getPersistentDataContainerData() {
        return persistentDataContainerData;
    }

    @NotNull
    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    public int getFormatVersion() {
        return formatVersion;
    }

}
