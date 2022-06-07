package net.william278.husksync;

import java.io.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Cross-platform class used to represent a player's data. Data from this can be deserialized using the DataSerializer class on Bukkit platforms.
 */
public class PlayerData implements Serializable {

    /**
     * The UUID of the player who this data belongs to
     */
    private final UUID playerUUID;

    /**
     * The unique version UUID of this data
     */
    private final UUID dataVersionUUID;

    /**
     * Epoch time identifying when the data was last updated or created
     */
    private long timestamp;

    /**
     * A special flag that will be {@code true} if the player is new to the network and should not have their data set when joining the Bukkit
     */
    public boolean useDefaultData = false;

    /*
     * Player data records
     */
    private String serializedInventory;
    private String serializedEnderChest;
    private double health;
    private double maxHealth;
    private double healthScale;
    private int hunger;
    private float saturation;
    private float saturationExhaustion;
    private int selectedSlot;
    private String serializedEffectData;
    private int totalExperience;
    private int expLevel;
    private float expProgress;
    private String gameMode;
    private String serializedStatistics;
    private boolean isFlying;
    private String serializedAdvancements;
    private String serializedLocation;

    /**
     * Constructor to create new PlayerData from a bukkit {@code Player}'s data
     *
     * @param playerUUID              The Player's UUID
     * @param serializedInventory     Their serialized inventory
     * @param serializedEnderChest    Their serialized ender chest
     * @param health                  Their health
     * @param maxHealth               Their max health
     * @param healthScale             Their health scale
     * @param hunger                  Their hunger
     * @param saturation              Their saturation
     * @param saturationExhaustion    Their saturation exhaustion
     * @param selectedSlot            Their selected hot bar slot
     * @param serializedStatusEffects Their serialized status effects
     * @param totalExperience         Their total experience points ("Score")
     * @param expLevel                Their exp level
     * @param expProgress             Their exp progress to the next level
     * @param gameMode                Their game mode ({@code SURVIVAL}, {@code CREATIVE}, etc.)
     * @param serializedStatistics    Their serialized statistics data (Displayed in Statistics menu in ESC menu)
     */
    public PlayerData(UUID playerUUID, String serializedInventory, String serializedEnderChest, double health, double maxHealth,
                      double healthScale, int hunger, float saturation, float saturationExhaustion, int selectedSlot,
                      String serializedStatusEffects, int totalExperience, int expLevel, float expProgress, String gameMode,
                      String serializedStatistics, boolean isFlying, String serializedAdvancements, String serializedLocation) {
        this.dataVersionUUID = UUID.randomUUID();
        this.timestamp = Instant.now().getEpochSecond();
        this.playerUUID = playerUUID;
        this.serializedInventory = serializedInventory;
        this.serializedEnderChest = serializedEnderChest;
        this.health = health;
        this.maxHealth = maxHealth;
        this.healthScale = healthScale;
        this.hunger = hunger;
        this.saturation = saturation;
        this.saturationExhaustion = saturationExhaustion;
        this.selectedSlot = selectedSlot;
        this.serializedEffectData = serializedStatusEffects;
        this.totalExperience = totalExperience;
        this.expLevel = expLevel;
        this.expProgress = expProgress;
        this.gameMode = gameMode;
        this.serializedStatistics = serializedStatistics;
        this.isFlying = isFlying;
        this.serializedAdvancements = serializedAdvancements;
        this.serializedLocation = serializedLocation;
    }

    /**
     * Constructor for a PlayerData object from an existing object that was stored in SQL
     *
     * @param playerUUID              The player whose data this is' UUID
     * @param dataVersionUUID         The PlayerData version UUID
     * @param serializedInventory     Their serialized inventory
     * @param serializedEnderChest    Their serialized ender chest
     * @param health                  Their health
     * @param maxHealth               Their max health
     * @param healthScale             Their health scale
     * @param hunger                  Their hunger
     * @param saturation              Their saturation
     * @param saturationExhaustion    Their saturation exhaustion
     * @param selectedSlot            Their selected hot bar slot
     * @param serializedStatusEffects Their serialized status effects
     * @param totalExperience         Their total experience points ("Score")
     * @param expLevel                Their exp level
     * @param expProgress             Their exp progress to the next level
     * @param gameMode                Their game mode ({@code SURVIVAL}, {@code CREATIVE}, etc.)
     * @param serializedStatistics    Their serialized statistics data (Displayed in Statistics menu in ESC menu)
     */
    public PlayerData(UUID playerUUID, UUID dataVersionUUID, long timestamp, String serializedInventory, String serializedEnderChest,
                      double health, double maxHealth, double healthScale, int hunger, float saturation, float saturationExhaustion,
                      int selectedSlot, String serializedStatusEffects, int totalExperience, int expLevel, float expProgress,
                      String gameMode, String serializedStatistics, boolean isFlying, String serializedAdvancements,
                      String serializedLocation) {
        this.playerUUID = playerUUID;
        this.dataVersionUUID = dataVersionUUID;
        this.timestamp = timestamp;
        this.serializedInventory = serializedInventory;
        this.serializedEnderChest = serializedEnderChest;
        this.health = health;
        this.maxHealth = maxHealth;
        this.healthScale = healthScale;
        this.hunger = hunger;
        this.saturation = saturation;
        this.saturationExhaustion = saturationExhaustion;
        this.selectedSlot = selectedSlot;
        this.serializedEffectData = serializedStatusEffects;
        this.totalExperience = totalExperience;
        this.expLevel = expLevel;
        this.expProgress = expProgress;
        this.gameMode = gameMode;
        this.serializedStatistics = serializedStatistics;
        this.isFlying = isFlying;
        this.serializedAdvancements = serializedAdvancements;
        this.serializedLocation = serializedLocation;
    }

    /**
     * Get default PlayerData for a new user
     *
     * @param playerUUID The bukkit Player's UUID
     * @return Default {@link PlayerData}
     */
    public static PlayerData DEFAULT_PLAYER_DATA(UUID playerUUID) {
        PlayerData data = new PlayerData(playerUUID, "", "", 20,
                20, 20, 20, 10, 1, 0,
                "", 0, 0, 0, "SURVIVAL",
                "", false, "", "");
        data.useDefaultData = true;
        return data;
    }

    /**
     * Get the {@link UUID} of the player whose data this is
     *
     * @return the player's {@link UUID}
     */
    public UUID getPlayerUUID() {
        return playerUUID;
    }

    /**
     * Get the unique version {@link UUID} of the PlayerData
     *
     * @return The unique data version
     */
    public UUID getDataVersionUUID() {
        return dataVersionUUID;
    }

    /**
     * Get the timestamp when this data was created or last updated
     *
     * @return time since epoch of last data update or creation
     */
    public long getDataTimestamp() {
        return timestamp;
    }

    /**
     * Returns the serialized player {@code ItemStack[]} inventory
     *
     * @return The player's serialized inventory
     */
    public String getSerializedInventory() {
        return serializedInventory;
    }

    /**
     * Returns the serialized player {@code ItemStack[]} ender chest
     *
     * @return The player's serialized ender chest
     */
    public String getSerializedEnderChest() {
        return serializedEnderChest;
    }

    /**
     * Returns the player's health value
     *
     * @return the player's health
     */
    public double getHealth() {
        return health;
    }

    /**
     * Returns the player's max health value
     *
     * @return the player's max health
     */
    public double getMaxHealth() {
        return maxHealth;
    }

    /**
     * Returns the player's health scale value {@see https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/entity/Player.html#getHealthScale()}
     *
     * @return the player's health scaling value
     */
    public double getHealthScale() {
        return healthScale;
    }

    /**
     * Returns the player's hunger points
     *
     * @return the player's hunger level
     */
    public int getHunger() {
        return hunger;
    }

    /**
     * Returns the player's saturation points
     *
     * @return the player's saturation level
     */
    public float getSaturation() {
        return saturation;
    }

    /**
     * Returns the player's saturation exhaustion value {@see https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/entity/HumanEntity.html#getExhaustion()}
     *
     * @return the player's saturation exhaustion
     */
    public float getSaturationExhaustion() {
        return saturationExhaustion;
    }

    /**
     * Returns the number of the player's currently selected hotbar slot
     *
     * @return the player's selected hotbar slot
     */
    public int getSelectedSlot() {
        return selectedSlot;
    }

    /**
     * Returns a serialized {@link String} of the player's current status effects
     *
     * @return the player's serialized status effect data
     */
    public String getSerializedEffectData() {
        return serializedEffectData;
    }

    /**
     * Returns the player's total experience score (used for presenting the death screen score value)
     *
     * @return the player's total experience score
     */
    public int getTotalExperience() {
        return totalExperience;
    }

    /**
     * Returns a serialized {@link String} of the player's statistics
     *
     * @return the player's serialized statistic records
     */
    public String getSerializedStatistics() {
        return serializedStatistics;
    }

    /**
     * Returns the player's current experience level
     *
     * @return the player's exp level
     */
    public int getExpLevel() {
        return expLevel;
    }

    /**
     * Returns the player's progress to the next experience level
     *
     * @return the player's exp progress
     */
    public float getExpProgress() {
        return expProgress;
    }

    /**
     * Returns the player's current game mode as a string ({@code SURVIVAL}, {@code CREATIVE}, etc.)
     *
     * @return the player's game mode
     */
    public String getGameMode() {
        return gameMode;
    }

    /**
     * Returns if the player is currently flying
     *
     * @return {@code true} if the player is in flight; {@code false} otherwise
     */
    public boolean isFlying() {
        return isFlying;
    }

    /**
     * Returns a serialized {@link String} of the player's advancements
     *
     * @return the player's serialized advancement data
     */
    public String getSerializedAdvancements() {
        return serializedAdvancements;
    }

    /**
     * Returns a serialized {@link String} of the player's current location
     *
     * @return the player's serialized location
     */
    public String getSerializedLocation() {
        return serializedLocation;
    }

    /**
     * Update the player's inventory data
     *
     * @param serializedInventory A serialized {@code String}; new inventory data
     */
    public void setSerializedInventory(String serializedInventory) {
        this.serializedInventory = serializedInventory;
        this.timestamp = Instant.now().getEpochSecond();
    }

    /**
     * Update the player's ender chest data
     *
     * @param serializedEnderChest A serialized {@code String}; new ender chest inventory data
     */
    public void setSerializedEnderChest(String serializedEnderChest) {
        this.serializedEnderChest = serializedEnderChest;
        this.timestamp = Instant.now().getEpochSecond();
    }

    /**
     * Update the player's health
     *
     * @param health new health value
     */
    public void setHealth(double health) {
        this.health = health;
        this.timestamp = Instant.now().getEpochSecond();
    }

    /**
     * Update the player's max health
     *
     * @param maxHealth new maximum health value
     */
    public void setMaxHealth(double maxHealth) {
        this.maxHealth = maxHealth;
        this.timestamp = Instant.now().getEpochSecond();
    }

    /**
     * Update the player's health scale
     *
     * @param healthScale new health scaling value
     */
    public void setHealthScale(double healthScale) {
        this.healthScale = healthScale;
        this.timestamp = Instant.now().getEpochSecond();
    }

    /**
     * Update the player's hunger meter
     *
     * @param hunger new hunger value
     */
    public void setHunger(int hunger) {
        this.hunger = hunger;
        this.timestamp = Instant.now().getEpochSecond();
    }

    /**
     * Update the player's saturation level
     *
     * @param saturation new saturation value
     */
    public void setSaturation(float saturation) {
        this.saturation = saturation;
        this.timestamp = Instant.now().getEpochSecond();
    }

    /**
     * Update the player's saturation exhaustion value
     *
     * @param saturationExhaustion new exhaustion value
     */
    public void setSaturationExhaustion(float saturationExhaustion) {
        this.saturationExhaustion = saturationExhaustion;
        this.timestamp = Instant.now().getEpochSecond();
    }

    /**
     * Update the player's selected hotbar slot
     *
     * @param selectedSlot new hotbar slot number (0-9)
     */
    public void setSelectedSlot(int selectedSlot) {
        this.selectedSlot = selectedSlot;
        this.timestamp = Instant.now().getEpochSecond();
    }

    /**
     * Update the player's status effect data
     *
     * @param serializedEffectData A serialized {@code String} of the player's new status effect data
     */
    public void setSerializedEffectData(String serializedEffectData) {
        this.serializedEffectData = serializedEffectData;
        this.timestamp = Instant.now().getEpochSecond();
    }

    /**
     * Set the player's total experience points (used to display score on death screen)
     *
     * @param totalExperience the player's new total experience score
     */
    public void setTotalExperience(int totalExperience) {
        this.totalExperience = totalExperience;
        this.timestamp = Instant.now().getEpochSecond();
    }

    /**
     * Set the player's exp level
     *
     * @param expLevel the player's new exp level
     */
    public void setExpLevel(int expLevel) {
        this.expLevel = expLevel;
        this.timestamp = Instant.now().getEpochSecond();
    }

    /**
     * Set the player's progress to their next exp level
     *
     * @param expProgress the player's new experience progress
     */
    public void setExpProgress(float expProgress) {
        this.expProgress = expProgress;
        this.timestamp = Instant.now().getEpochSecond();
    }

    /**
     * Set the player's game mode
     *
     * @param gameMode the player's new game mode ({@code SURVIVAL}, {@code CREATIVE}, etc.)
     */
    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
        this.timestamp = Instant.now().getEpochSecond();
    }

    /**
     * Update the player's statistics data
     *
     * @param serializedStatistics A serialized {@code String}; new statistic data
     */
    public void setSerializedStatistics(String serializedStatistics) {
        this.serializedStatistics = serializedStatistics;
        this.timestamp = Instant.now().getEpochSecond();
    }

    /**
     * Set if the player is flying
     *
     * @param flying whether the player is flying
     */
    public void setFlying(boolean flying) {
        isFlying = flying;
        this.timestamp = Instant.now().getEpochSecond();
    }

    /**
     * Update the player's advancement data
     *
     * @param serializedAdvancements A serialized {@code String}; new advancement data
     */
    public void setSerializedAdvancements(String serializedAdvancements) {
        this.serializedAdvancements = serializedAdvancements;
        this.timestamp = Instant.now().getEpochSecond();
    }

    /**
     * Update the player's location data
     *
     * @param serializedLocation A serialized {@code String}; new location data
     */
    public void setSerializedLocation(String serializedLocation) {
        this.serializedLocation = serializedLocation;
        this.timestamp = Instant.now().getEpochSecond();
    }
}
