package net.william278.husksync.data;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/***
 * Stores data about a user
 */
public class UserData {

    /**
     * Stores the user's status data, including health, food, etc.
     */
    @SerializedName("status")
    protected StatusData statusData;

    /**
     * Stores the user's inventory contents
     */
    @SerializedName("inventory")
    protected InventoryData inventoryData;

    /**
     * Stores the user's ender chest contents
     */
    @SerializedName("ender_chest")
    protected InventoryData enderChestData;

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

    public UserData(@NotNull StatusData statusData, @NotNull InventoryData inventoryData,
                    @NotNull InventoryData enderChestData, @NotNull PotionEffectData potionEffectData,
                    @NotNull List<AdvancementData> advancementData, @NotNull StatisticsData statisticData,
                    @NotNull LocationData locationData, @NotNull PersistentDataContainerData persistentDataContainerData) {
        this.statusData = statusData;
        this.inventoryData = inventoryData;
        this.enderChestData = enderChestData;
        this.potionEffectData = potionEffectData;
        this.advancementData = advancementData;
        this.statisticData = statisticData;
        this.locationData = locationData;
        this.persistentDataContainerData = persistentDataContainerData;
    }

    // Empty constructor to facilitate json serialization
    @SuppressWarnings("unused")
    protected UserData() {
    }

    public StatusData getStatusData() {
        return statusData;
    }

    public InventoryData getInventoryData() {
        return inventoryData;
    }

    public InventoryData getEnderChestData() {
        return enderChestData;
    }

    public PotionEffectData getPotionEffectData() {
        return potionEffectData;
    }

    public List<AdvancementData> getAdvancementData() {
        return advancementData;
    }

    public StatisticsData getStatisticData() {
        return statisticData;
    }

    public LocationData getLocationData() {
        return locationData;
    }

    public PersistentDataContainerData getPersistentDataContainerData() {
        return persistentDataContainerData;
    }

}
