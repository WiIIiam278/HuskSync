package me.william278.crossserversync;

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


    // Player data
    private final String serializedInventory;
    private final String serializedEnderChest;
    private final double health;
    private final double maxHealth;
    private final int hunger;
    private final float saturation;
    private final float saturationExhaustion;
    private final int selectedSlot;
    private final String serializedEffectData;
    private final int experience;

    /**
     * Create a new PlayerData object; a random data version UUID will be selected.
     * @param playerUUID UUID of the player
     * @param serializedInventory Serialized inventory data
     * @param serializedEnderChest Serialized ender chest data
     * @param health Player health
     * @param maxHealth Player max health
     * @param hunger Player hunger
     * @param saturation Player saturation
     * @param selectedSlot Player selected slot
     * @param serializedStatusEffects Serialized status effect data
     */
    public PlayerData(UUID playerUUID, String serializedInventory, String serializedEnderChest, double health, double maxHealth, int hunger, float saturation, float saturationExhaustion, int selectedSlot, String serializedStatusEffects, int experience) {
        this.dataVersionUUID = UUID.randomUUID();
        this.playerUUID = playerUUID;
        this.serializedInventory = serializedInventory;
        this.serializedEnderChest = serializedEnderChest;
        this.health = health;
        this.maxHealth = maxHealth;
        this.hunger = hunger;
        this.saturation = saturation;
        this.saturationExhaustion = saturationExhaustion;
        this.selectedSlot = selectedSlot;
        this.serializedEffectData = serializedStatusEffects;
        this.experience = experience;
    }

    public PlayerData(UUID playerUUID, UUID dataVersionUUID, String serializedInventory, String serializedEnderChest, double health, double maxHealth, int hunger, float saturation, float saturationExhaustion, int selectedSlot, String serializedStatusEffects, int experience) {
        this.playerUUID = playerUUID;
        this.dataVersionUUID = dataVersionUUID;
        this.serializedInventory = serializedInventory;
        this.serializedEnderChest = serializedEnderChest;
        this.health = health;
        this.maxHealth = maxHealth;
        this.hunger = hunger;
        this.saturation = saturation;
        this.saturationExhaustion = saturationExhaustion;
        this.selectedSlot = selectedSlot;
        this.serializedEffectData = serializedStatusEffects;
        this.experience = experience;
    }

    public static PlayerData DEFAULT_PLAYER_DATA(UUID playerUUID) {
        return new PlayerData(playerUUID, "", "", 20,
                20, 20, 10, 1, 0, "", 0);
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

    public int getHunger() {
        return hunger;
    }

    public float getSaturation() {
        return saturation;
    }

    public float getSaturationExhaustion() { return saturationExhaustion; }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public String getSerializedEffectData() {
        return serializedEffectData;
    }

    public int getExperience() { return experience; }
}
