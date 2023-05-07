/*
 * This file is part of HuskSync by William278. Do not redistribute!
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  All rights reserved.
 *
 *  This source code is provided as reference to licensed individuals that have purchased the HuskSync
 *  plugin once from any of the official sources it is provided. The availability of this code does
 *  not grant you the rights to modify, re-distribute, compile or redistribute this source code or
 *  "plugin" outside this intended purpose. This license does not cover libraries developed by third
 *  parties that are utilised in the plugin.
 */

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

    /**
     * Check if the item data is empty
     *
     * @return {@code true} if the item data is empty; {@code false} otherwise
     */
    public boolean isEmpty() {
        return serializedItems.isEmpty();
    }

}
