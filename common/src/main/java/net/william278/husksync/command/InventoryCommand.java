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

package net.william278.husksync.command;

import de.themoep.minedown.adventure.MineDown;
import net.william278.husksync.HuskSync;
import net.william278.husksync.data.Data;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.user.OnlineUser;
import net.william278.husksync.user.User;
import org.jetbrains.annotations.NotNull;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class InventoryCommand extends ItemsCommand {

    public InventoryCommand(@NotNull HuskSync plugin) {
        super(plugin, List.of("inventory", "invsee", "openinv"));
    }

    @Override
    protected void showItems(@NotNull OnlineUser viewer, @NotNull DataSnapshot.Unpacked snapshot,
                             @NotNull User user, boolean allowEdit) {
        final Optional<Data.Items.Inventory> optionalInventory = snapshot.getInventory();
        if (optionalInventory.isEmpty()) {
            plugin.getLocales().getLocale("error_no_data_to_display")
                    .ifPresent(viewer::sendMessage);
            return;
        }

        // Display opening message
        plugin.getLocales().getLocale("inventory_viewer_opened", user.getUsername(),
                        snapshot.getTimestamp().format(DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm")))
                .ifPresent(viewer::sendMessage);

        // Show GUI
        final Data.Items.Inventory inventory = optionalInventory.get();
        viewer.showGui(
                inventory,
                plugin.getLocales().getLocale("inventory_viewer_menu_title", user.getUsername())
                        .orElse(new MineDown(String.format("%s's Inventory", user.getUsername()))),
                allowEdit,
                inventory.getSlotCount(),
                (itemsOnClose) -> {
                    if (allowEdit && !inventory.equals(itemsOnClose)) {
                        plugin.runAsync(() -> this.updateItems(viewer, itemsOnClose, user));
                    }
                }
        );
    }

    // Creates a new snapshot with the updated inventory
    @SuppressWarnings("DuplicatedCode")
    private void updateItems(@NotNull OnlineUser viewer, @NotNull Data.Items.Items items, @NotNull User user) {
        final Optional<DataSnapshot.Packed> latestData = plugin.getDatabase().getLatestSnapshot(user);
        if (latestData.isEmpty()) {
            plugin.getLocales().getLocale("error_no_data_to_display")
                    .ifPresent(viewer::sendMessage);
            return;
        }

        // Create and pack the snapshot with the updated inventory
        final DataSnapshot.Packed snapshot = latestData.get().copy();
        snapshot.edit(plugin, (data) -> {
            data.setSaveCause(DataSnapshot.SaveCause.INVENTORY_COMMAND);
            data.setPinned(plugin.getSettings().doAutoPin(DataSnapshot.SaveCause.INVENTORY_COMMAND));
            data.getInventory().ifPresent(inventory -> inventory.setContents(items));
        });
        plugin.getDatabase().addSnapshot(user, snapshot);
        plugin.getRedisManager().sendUserDataUpdate(user, snapshot);
    }

}
