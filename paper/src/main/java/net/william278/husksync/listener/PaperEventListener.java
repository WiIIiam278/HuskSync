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

package net.william278.husksync.listener;

import net.william278.husksync.BukkitHuskSync;
import net.william278.husksync.data.BukkitData;
import net.william278.husksync.user.BukkitUser;
import net.william278.husksync.user.OnlineUser;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PaperEventListener extends BukkitEventListener {

    public PaperEventListener(@NotNull BukkitHuskSync plugin) {
        super(plugin);
    }

    @Override
    public void handlePlayerDeath(@NotNull PlayerDeathEvent event) {
        // If the player is locked or the plugin disabling, clear their drops
        final OnlineUser user = BukkitUser.adapt(event.getEntity(), plugin);
        if (cancelPlayerEvent(user.getUuid())) {
            event.getDrops().clear();
            event.getItemsToKeep().clear();
            return;
        }

        // Handle saving player data snapshots on death
        if (!plugin.getSettings().doSaveOnDeath()) {
            return;
        }

        // Paper - support saving the player's items to keep if enabled
        final int maxInventorySize = BukkitData.Items.Inventory.INVENTORY_SLOT_COUNT;
        final List<ItemStack> itemsToSave = switch (plugin.getSettings().getDeathItemsMode()) {
            case DROPS -> event.getDrops();
            case ITEMS_TO_KEEP -> event.getItemsToKeep();
        };
        if (itemsToSave.size() > maxInventorySize) {
            itemsToSave.subList(maxInventorySize, itemsToSave.size()).clear();
        }
        super.saveOnPlayerDeath(user, BukkitData.Items.ItemArray.adapt(itemsToSave));
    }

}
