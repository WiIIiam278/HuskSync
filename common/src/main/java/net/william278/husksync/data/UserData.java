package net.william278.husksync.data;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.HashSet;
import java.util.UUID;

/***
 * Stores data about a user
 */
public class UserData implements Comparable<UserData> {

    /**
     * The unique identifier for this user data version
     */
    protected UUID dataUuidVersion;

    /**
     * An epoch milliseconds timestamp of when this data was created
     */
    protected long creationTimestamp;

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
    protected HashSet<AdvancementData> advancementData;

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
                    @NotNull HashSet<AdvancementData> advancementData, @NotNull StatisticsData statisticData,
                    @NotNull LocationData locationData, @NotNull PersistentDataContainerData persistentDataContainerData) {
        this.dataUuidVersion = UUID.randomUUID();
        this.creationTimestamp = Instant.now().toEpochMilli();
        this.statusData = statusData;
        this.inventoryData = inventoryData;
        this.enderChestData = enderChestData;
        this.potionEffectData = potionEffectData;
        this.advancementData = advancementData;
        this.statisticData = statisticData;
        this.locationData = locationData;
        this.persistentDataContainerData = persistentDataContainerData;
    }

    protected UserData() {
    }

    /**
     * Compare UserData by creation timestamp
     *
     * @param other the other UserData to be compared
     * @return the comparison result; the more recent UserData is greater than the less recent UserData
     */
    @Override
    public int compareTo(@NotNull UserData other) {
        return Long.compare(this.creationTimestamp, other.creationTimestamp);
    }

    @NotNull
    public static UserData fromJson(String json) throws JsonSyntaxException {
        return new GsonBuilder().create().fromJson(json, UserData.class);
    }

    @NotNull
    public String toJson() {
        return new GsonBuilder().create().toJson(this);
    }

    public void setMetadata(@NotNull UUID dataUuidVersion, long creationTimestamp) {
        this.dataUuidVersion = dataUuidVersion;
        this.creationTimestamp = creationTimestamp;
    }

    public UUID getDataUuidVersion() {
        return dataUuidVersion;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
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

    public HashSet<AdvancementData> getAdvancementData() {
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
