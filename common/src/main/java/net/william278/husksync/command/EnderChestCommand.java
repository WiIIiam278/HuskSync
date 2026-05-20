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
import net.william278.husksync.redis.RedisManager;
import net.william278.husksync.user.OnlineUser;
import net.william278.husksync.user.User;
import org.jetbrains.annotations.NotNull;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class EnderChestCommand extends ItemsCommand {

    public EnderChestCommand(@NotNull HuskSync plugin) {
        super("enderchest", List.of("echest", "openechest"), DataSnapshot.SaveCause.ENDERCHEST_COMMAND, plugin);
    }

    @Override
    protected void showItems(@NotNull OnlineUser viewer, @NotNull DataSnapshot.Unpacked snapshot,
                             @NotNull User user, boolean allowEdit) {
        final Optional<Data.Items.EnderChest> optionalEnderChest = snapshot.getEnderChest();
        if (optionalEnderChest.isEmpty()) {
            plugin.getLocales().getLocale("error_no_data_to_display")
                    .ifPresent(viewer::sendMessage);
            return;
        }

        // Display opening message
        plugin.getLocales().getLocale("ender_chest_viewer_opened", user.getName(),
                        snapshot.getTimestamp().format(DateTimeFormatter
                                .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)))
                .ifPresent(viewer::sendMessage);

        // Show GUI
        final Data.Items.EnderChest enderChest = optionalEnderChest.get();
        viewer.showGui(
                enderChest,
                plugin.getLocales().getLocale("ender_chest_viewer_menu_title", user.getName())
                        .orElse(new MineDown(String.format("%s's Ender Chest", user.getName()))),
                allowEdit,
                enderChest.getSlotCount(),
                (itemsOnClose) -> {
                    if (allowEdit && !itemsEqual(enderChest, itemsOnClose)) {
                        plugin.runAsync(() -> this.updateItems(
                                viewer, enderChest, itemsOnClose, user
                        ));
                    }
                }
        );
    }

    // Creates a new snapshot with the updated enderChest
    @SuppressWarnings("DuplicatedCode")
    private void updateItems(@NotNull OnlineUser viewer, @NotNull Data.Items.Items openedItems,
                             @NotNull Data.Items.Items items, @NotNull User holder) {
        final Optional<DataSnapshot.Packed> latestData = plugin.getDatabase().getLatestSnapshot(holder);
        if (latestData.isEmpty()) {
            plugin.getLocales().getLocale("error_no_data_to_display")
                    .ifPresent(viewer::sendMessage);
            return;
        }

        plugin.getRedisManager().getOnlineUserData(UUID.randomUUID(), holder, saveCause).thenAccept(currentData -> {
            final Optional<Data.Items.EnderChest> currentEnderChest = currentData
                    .or(() -> latestData)
                    .flatMap(snapshot -> snapshot.unpack(plugin).getEnderChest());

            if (currentEnderChest.isEmpty()) {
                plugin.getLocales().getLocale("error_no_data_to_display")
                        .ifPresent(viewer::sendMessage);
                return;
            }

            if (itemsEqual(currentEnderChest.get(), items)) {
                return;
            }

            if (!itemsEqual(currentEnderChest.get(), openedItems)) {
                plugin.getLocales().getLocale("error_ender_chest_changed").ifPresent(viewer::sendMessage);
                return;
            }

            // Create and pack the snapshot with the updated enderChest
            final DataSnapshot.Packed snapshot = latestData.get().copy();
            boolean pin = plugin.getSettings().getSynchronization().doAutoPin(saveCause);

            snapshot.edit(plugin, (data) -> {
                data.getEnderChest().ifPresent(enderChest -> enderChest.setContents(items));
                data.setSaveCause(saveCause);
                data.setPinned(pin);
            });

            // Save data
            final RedisManager redis = plugin.getRedisManager();
            plugin.getDataSyncer().saveData(holder, snapshot, (user, data) -> {
                redis.getUserData(user).ifPresent(d -> redis.setUserData(user, snapshot));
                redis.sendUserDataUpdate(user, data);
            });
        });
    }

    private boolean itemsEqual(@NotNull Data.Items first, @NotNull Data.Items second) {
        final Data.Items.Stack[] firstStacks = first.getStack();
        final Data.Items.Stack[] secondStacks = second.getStack();
        final int slotCount = first.getSlotCount();

        for (int slot = 0; slot < slotCount; slot++) {
            if (!stackEqual(getStack(firstStacks, slot), getStack(secondStacks, slot))) {
                return false;
            }
        }
        return true;
    }

    private Data.Items.Stack getStack(@NotNull Data.Items.Stack[] stacks, int slot) {
        return slot < stacks.length ? stacks[slot] : null;
    }

    private boolean stackEqual(Data.Items.Stack first, Data.Items.Stack second) {
        if (first == second) {
            return true;
        }
        if (first == null || second == null) {
            return false;
        }
        return first.amount() == second.amount()
                && first.material().equals(second.material())
                && Objects.equals(first.name(), second.name())
                && Objects.equals(first.lore(), second.lore())
                && new HashSet<>(first.enchantments()).equals(new HashSet<>(second.enchantments()));
    }
}