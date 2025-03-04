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

import com.google.common.collect.Lists;
import net.william278.husksync.BukkitHuskSync;
import net.william278.husksync.data.BukkitData;
import net.william278.husksync.user.BukkitUser;
import net.william278.husksync.user.OnlineUser;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

import static net.william278.husksync.config.Settings.SynchronizationSettings.SaveOnDeathSettings;

public class PaperEventListener extends BukkitEventListener {

    public PaperEventListener(@NotNull BukkitHuskSync plugin) {
        super(plugin);
    }

    @Override
    public void onEnable() {
        getPlugin().getServer().getPluginManager().registerEvents(this, getPlugin());
        lockedHandler.onEnable();
    }

    @Override
    public void handlePlayerDeath(@NotNull PlayerDeathEvent event) {
        // If the player is locked or the plugin disabling, clear their drops
        final OnlineUser user = BukkitUser.adapt(event.getEntity(), plugin);
        if (lockedHandler.cancelPlayerEvent(user.getUuid())) {
            event.getDrops().clear();
            event.getItemsToKeep().clear();
            return;
        }

        // Handle saving player data snapshots on death
        final SaveOnDeathSettings settings = plugin.getSettings().getSynchronization().getSaveOnDeath();
        if (!settings.isEnabled()) {
            return;
        }

        // Paper - support saving the player's items to keep if enabled
        final int maxInventorySize = BukkitData.Items.Inventory.INVENTORY_SLOT_COUNT;
        final List<ItemStack> itemsToSave = switch (settings.getItemsToSave()) {
            case DROPS -> event.getDrops();
            case ITEMS_TO_KEEP -> preserveOrder(event.getEntity().getInventory(), event.getItemsToKeep());
        };
        if (itemsToSave.size() > maxInventorySize) {
            itemsToSave.subList(maxInventorySize, itemsToSave.size()).clear();
        }
        super.saveOnPlayerDeath(user, BukkitData.Items.ItemArray.adapt(itemsToSave));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerAdvancementDone(@NotNull PlayerAdvancementDoneEvent event) {
        if (lockedHandler.cancelPlayerEvent(event.getPlayer().getUniqueId())) {
            event.message(null);
        }
    }

    @NotNull
    private List<ItemStack> preserveOrder(@NotNull PlayerInventory inventory, @NotNull List<ItemStack> toKeep) {
        final List<ItemStack> preserved = Lists.newArrayList();
        final List<ItemStack> items = Lists.newArrayList(inventory.getContents());
        for (ItemStack item : toKeep) {
            final Iterator<ItemStack> iterator = items.iterator();
            while (iterator.hasNext()) {
                final ItemStack originalItem = iterator.next();
                if (originalItem != null && originalItem.equals(item)) {
                    preserved.add(originalItem);
                    iterator.remove();
                    break;
                }
            }
        }
        return preserved;
    }

}
