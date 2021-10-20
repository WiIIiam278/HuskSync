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

    /**
     * Serialized inventory data
     */
    private final String serializedInventory;

    /**
     * Serialized ender chest data
     */
    private final String serializedEnderChest;

    /**
     * Create a new PlayerData object; a random data version UUID will be selected.
     *
     * @param playerUUID          The UUID of the player
     * @param serializedInventory The player's serialized inventory data
     */
    //todo add more stuff, like player health, max health, hunger, saturation and status effects
    public PlayerData(UUID playerUUID, String serializedInventory, String serializedEnderChest) {
        this.dataVersionUUID = UUID.randomUUID();
        this.playerUUID = playerUUID;
        this.serializedInventory = serializedInventory;
        this.serializedEnderChest = serializedEnderChest;
    }

    public PlayerData(UUID playerUUID, UUID dataVersionUUID, String serializedInventory, String serializedEnderChest, double health, double maxHealth, double hunger, double saturation, String serializedStatusEffects) {
        this.playerUUID = playerUUID;
        this.dataVersionUUID = dataVersionUUID;
        this.serializedInventory = serializedInventory;
        this.serializedEnderChest = serializedEnderChest;

        //todo Incorporate more of these
    }

    public static PlayerData EMPTY_PLAYER_DATA(UUID playerUUID) {
        return new PlayerData(playerUUID, "", "");
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
}
