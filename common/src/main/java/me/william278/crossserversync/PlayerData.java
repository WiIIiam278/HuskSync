package me.william278.crossserversync;

import java.io.Serializable;
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

    //todo add more stuff, like ender chest, player health, max health, hunger and status effects, et cetera

    /**
     * Create a new PlayerData object; a random data version UUID will be selected.
     * @param playerUUID The UUID of the player
     * @param serializedInventory The player's serialized inventory data
     */
    public PlayerData(UUID playerUUID, String serializedInventory, String serializedEnderChest) {
        this.dataVersionUUID = UUID.randomUUID();
        this.playerUUID = playerUUID;
        this.serializedInventory = serializedInventory;
        this.serializedEnderChest = serializedEnderChest;
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
