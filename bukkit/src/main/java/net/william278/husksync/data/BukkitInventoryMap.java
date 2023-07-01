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

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A mapped player inventory, providing methods to easily access a player's inventory.
 */
@SuppressWarnings("unused")
public class BukkitInventoryMap {

    public static final int INVENTORY_SLOT_COUNT = 41;

    private ItemStack[] contents;

    /**
     * Creates a new mapped inventory from the given contents.
     *
     * @param contents the contents of the inventory
     */
    protected BukkitInventoryMap(ItemStack[] contents) {
        this.contents = contents;
    }

    /**
     * Gets the contents of the inventory.
     *
     * @return the contents of the inventory
     */
    public ItemStack[] getContents() {
        return contents;
    }

    /**
     * Set the contents of the inventory.
     *
     * @param contents the contents of the inventory
     */
    public void setContents(ItemStack[] contents) {
        this.contents = contents;
    }

    /**
     * Gets the size of the inventory.
     *
     * @return the size of the inventory
     */
    public int getSize() {
        return contents.length;
    }

    /**
     * Gets the item at the given index.
     *
     * @param index the index of the item to get
     * @return the item at the given index
     */
    public Optional<ItemStack> getItemAt(int index) {
        if (contents.length >= index) {
            if (contents[index] == null) {
                return Optional.empty();
            }
            return Optional.of(contents[index]);
        }
        return Optional.empty();
    }

    /**
     * Sets the item at the given index.
     *
     * @param itemStack the item to set at the given index
     * @param index     the index of the item to set
     * @throws IllegalArgumentException if the index is out of bounds
     */
    public void setItemAt(@NotNull ItemStack itemStack, int index) throws IllegalArgumentException {
        contents[index] = itemStack;
    }

    /**
     * Returns the main inventory contents.
     *
     * @return the main inventory contents
     */
    public ItemStack[] getInventory() {
        final ItemStack[] inventory = new ItemStack[36];
        System.arraycopy(contents, 0, inventory, 0, Math.min(contents.length, inventory.length));
        return inventory;
    }

    public ItemStack[] getHotbar() {
        final ItemStack[] armor = new ItemStack[9];
        for (int i = 0; i <= 9; i++) {
            armor[i] = getItemAt(i).orElse(null);
        }
        return armor;
    }

    public Optional<ItemStack> getOffHand() {
        return getItemAt(40);
    }

    public Optional<ItemStack> getHelmet() {
        return getItemAt(39);
    }

    public Optional<ItemStack> getChestplate() {
        return getItemAt(38);
    }

    public Optional<ItemStack> getLeggings() {
        return getItemAt(37);
    }

    public Optional<ItemStack> getBoots() {
        return getItemAt(36);
    }

    public ItemStack[] getArmor() {
        final ItemStack[] armor = new ItemStack[4];
        for (int i = 36; i < 40; i++) {
            armor[i - 36] = getItemAt(i).orElse(null);
        }
        return armor;
    }

}
