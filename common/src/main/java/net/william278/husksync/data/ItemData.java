/*
 * This file is part of HuskSync, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
