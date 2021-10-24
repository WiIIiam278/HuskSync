package me.william278.husksync;

import java.io.*;
import java.util.UUID;

public class PlayerData implements Serializable {

    /**
     * The UUID of the player who this data belongs to
     */
    private final UUID playerUUID;

    /**
     * The unique version UUID of this data
     */
    private final UUID dataVersionUUID;

    // Flag to indicate if the Bukkit server should use default data
    public boolean useDefaultData = false;

    // Player data
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
     * @param gameMode                Their game mode ({@code SURVIVAL}, {@code CREATIVE}, etc)
     * @param serializedStatistics    Their serialized statistics data (Displayed in Statistics menu in ESC menu)
     */
    public PlayerData(UUID playerUUID, String serializedInventory, String serializedEnderChest, double health, double maxHealth,
                      double healthScale, int hunger, float saturation, float saturationExhaustion, int selectedSlot,
                      String serializedStatusEffects, int totalExperience, int expLevel, float expProgress, String gameMode,
                      String serializedStatistics, boolean isFlying, String serializedAdvancements, String serializedLocation) {
        this.dataVersionUUID = UUID.randomUUID();
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
     * @param gameMode                Their game mode ({@code SURVIVAL}, {@code CREATIVE}, etc)
     * @param serializedStatistics    Their serialized statistics data (Displayed in Statistics menu in ESC menu)
     */
    public PlayerData(UUID playerUUID, UUID dataVersionUUID, String serializedInventory, String serializedEnderChest,
                      double health, double maxHealth, double healthScale, int hunger, float saturation, float saturationExhaustion,
                      int selectedSlot, String serializedStatusEffects, int totalExperience, int expLevel, float expProgress,
                      String gameMode, String serializedStatistics, boolean isFlying, String serializedAdvancements,
                      String serializedLocation) {
        this.playerUUID = playerUUID;
        this.dataVersionUUID = dataVersionUUID;
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
                20, 0, 20, 10, 1, 0,
                "", 0, 0, 0, "SURVIVAL",
                "", false, "", "");
        data.useDefaultData = true;
        return data;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public UUID getDataVersionUUID() {
        return dataVersionUUID;
    }

    public String getSerializedInventory() {
        return serializedInventory;
    }

    public String getSerializedEnderChest() {
        return serializedEnderChest;
    }

    public double getHealth() {
        return health;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public double getHealthScale() { return healthScale; }

    public int getHunger() {
        return hunger;
    }

    public float getSaturation() {
        return saturation;
    }

    public float getSaturationExhaustion() {
        return saturationExhaustion;
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public String getSerializedEffectData() {
        return serializedEffectData;
    }

    public int getTotalExperience() {
        return totalExperience;
    }

    public String getSerializedStatistics() {
        return serializedStatistics;
    }

    public int getExpLevel() {
        return expLevel;
    }

    public float getExpProgress() {
        return expProgress;
    }

    public String getGameMode() {
        return gameMode;
    }

    public boolean isFlying() {
        return isFlying;
    }

    public String getSerializedAdvancements() {
        return serializedAdvancements;
    }

    public String getSerializedLocation() {
        return serializedLocation;
    }

    public boolean isUseDefaultData() {
        return useDefaultData;
    }

    public void setSerializedInventory(String serializedInventory) {
        this.serializedInventory = serializedInventory;
    }

    public void setSerializedEnderChest(String serializedEnderChest) {
        this.serializedEnderChest = serializedEnderChest;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    public void setMaxHealth(double maxHealth) {
        this.maxHealth = maxHealth;
    }

    public void setHealthScale(double healthScale) {
        this.healthScale = healthScale;
    }

    public void setHunger(int hunger) {
        this.hunger = hunger;
    }

    public void setSaturation(float saturation) {
        this.saturation = saturation;
    }

    public void setSaturationExhaustion(float saturationExhaustion) {
        this.saturationExhaustion = saturationExhaustion;
    }

    public void setSelectedSlot(int selectedSlot) {
        this.selectedSlot = selectedSlot;
    }

    public void setSerializedEffectData(String serializedEffectData) {
        this.serializedEffectData = serializedEffectData;
    }

    public void setTotalExperience(int totalExperience) {
        this.totalExperience = totalExperience;
    }

    public void setExpLevel(int expLevel) {
        this.expLevel = expLevel;
    }

    public void setExpProgress(float expProgress) {
        this.expProgress = expProgress;
    }

    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
    }

    public void setSerializedStatistics(String serializedStatistics) {
        this.serializedStatistics = serializedStatistics;
    }

    public void setFlying(boolean flying) {
        isFlying = flying;
    }

    public void setSerializedAdvancements(String serializedAdvancements) {
        this.serializedAdvancements = serializedAdvancements;
    }

    public void setSerializedLocation(String serializedLocation) {
        this.serializedLocation = serializedLocation;
    }
}
