package net.william278.husksync.data;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

/**
 * Stores information about a player's inventory or ender chest
 */
public class InventoryData {

    /**
     * A base64 string of platform-serialized inventory data
     */
    @SerializedName("serialized_inventory")
    public String serializedInventory;

    public InventoryData(@NotNull final String serializedInventory) {
        this.serializedInventory = serializedInventory;
    }

    @SuppressWarnings("unused")
    protected InventoryData() {
    }

}
