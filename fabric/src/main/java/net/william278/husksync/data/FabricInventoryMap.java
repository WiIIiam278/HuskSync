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

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class FabricInventoryMap {
    public static final int INVENTORY_SLOT_COUNT = 41;

    private final PlayerInventory inventory;

    public FabricInventoryMap(PlayerInventory inventory) {
        this.inventory = inventory;
    }

    public int getSize() {
        return inventory.size();
    }

    public Optional<ItemStack> getItemAt(int index) {
        if (index < inventory.size()) {
            return Optional.ofNullable(inventory.getStack(index));
        }

        return Optional.empty();
    }

    public void setItemAt(@NotNull ItemStack itemStack, int index) throws IllegalArgumentException {
        if (index < inventory.size()) {
            inventory.setStack(index, itemStack);
        } else {
            throw new IllegalArgumentException("Index out of bounds");
        }
    }

    public ItemStack[] getInventory() {
        final ItemStack[] inventory = new ItemStack[getSize()];
        for (int i = 0; i < getSize(); i++) {
            inventory[i] = getItemAt(i).orElse(null);
        }
        return inventory;
    }

    @SuppressWarnings("SpellCheckingInspection")
    public ItemStack[] getHotbar() {
        final ItemStack[] hotbar = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            hotbar[i] = getItemAt(i).orElse(null);
        }
        return hotbar;
    }

    public Optional<ItemStack> getOffHand() {
        return Optional.of(inventory.offHand.get(0));
    }

    public Optional<ItemStack> getHelmet() {
        return Optional.of(inventory.armor.get(3));
    }

    @SuppressWarnings("SpellCheckingInspection")
    // FIXME: typo?
    public Optional<ItemStack> getChestplate() {
        return Optional.of(inventory.armor.get(2));
    }

    public Optional<ItemStack> getLeggings() {
        return Optional.of(inventory.armor.get(1));
    }

    public Optional<ItemStack> getBoots() {
        return Optional.of(inventory.armor.get(0));
    }

    public ItemStack[] getArmor() {
        return inventory.armor.toArray(new ItemStack[0]);
    }
}
