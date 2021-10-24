package me.william278.husksync.migrator;

import java.io.Serializable;
import java.util.UUID;

/**
 * A class that stores player data taken from MPDB's database, that can then be converted into HuskSync's format
 */
public class MPDBPlayerData implements Serializable {

    /*
     * Player information
     */
    public final UUID playerUUID;
    public final String playerName;

    /*
     * Inventory, ender chest and armor data
     */
    public String inventoryData;
    public String armorData;
    public String enderChestData;

    /*
     * Experience data
     */
    public int expLevel;
    public float expProgress;
    public int totalExperience;

    public MPDBPlayerData(UUID playerUUID, String playerName) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
    }
}
