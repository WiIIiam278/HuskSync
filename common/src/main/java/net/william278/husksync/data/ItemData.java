package net.william278.husksync.data;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

/**
 * Stores information about the contents of a player's inventory or Ender Chest.
 */
public class ItemData {

    /**
     * A Base-64 string of platform-serialized items
     */
    @SerializedName("serialized_items")
    public String serializedItems;

    /**
     * Get an empty item data object, representing an empty inventory or Ender Chest
     *
     * @return an empty item data object
     */
    @NotNull
    public static ItemData empty() {
        return new ItemData("");
    }

    public ItemData(@NotNull final String serializedItems) {
        this.serializedItems = serializedItems;
    }

    @SuppressWarnings("unused")
    protected ItemData() {
    }

}
