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

import net.william278.husksync.HuskSync;
import net.william278.husksync.player.CommandUser;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.player.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class EnderChestCommand extends Command implements TabProvider {

    public EnderChestCommand(@NotNull HuskSync plugin) {
        super("enderchest", List.of("echest", "openechest"), "<player> [version_uuid]", plugin);
        setOperatorCommand(true);
        addAdditionalPermissions(Map.of("edit", true));
    }

    @Override
    public void execute(@NotNull CommandUser executor, @NotNull String[] args) {
        if (!(executor instanceof OnlineUser player)) {
            plugin.getLocales().getLocale("error_in_game_command_only")
                    .ifPresent(executor::sendMessage);
            return;
        }

        if (args.length == 0 || args.length > 2) {
            plugin.getLocales().getLocale("error_invalid_syntax", getUsage())
                    .ifPresent(player::sendMessage);
            return;
        }

        plugin.getDatabase().getUserByName(args[0].toLowerCase(Locale.ENGLISH)).ifPresentOrElse(user -> {
            if (args.length == 2) {
                // View user data by specified UUID
                try {
                    final UUID versionUuid = UUID.fromString(args[1]);
//                    plugin.getDatabase().getDataSnapshots(user, versionUuid).ifPresentOrElse(
//                            userData -> showEnderChestMenu(player, userData, user, false),
//                            () -> plugin.getLocales().getLocale("error_invalid_version_uuid")
//                                    .ifPresent(player::sendMessage));
                } catch (IllegalArgumentException e) {
                    plugin.getLocales().getLocale("error_invalid_syntax",
                            "/enderchest <player> [version_uuid]").ifPresent(player::sendMessage);
                }
            } else {
                // View (and edit) the latest user data
//                plugin.getDatabase().getCurrentUserData(user).ifPresentOrElse(
//                        versionedUserData -> showEnderChestMenu(player, versionedUserData, user,
//                                player.hasPermission(getPermission("edit"))),
//                        () -> plugin.getLocales().getLocale("error_no_data_to_display")
//                                .ifPresent(player::sendMessage));
            }
        }, () -> plugin.getLocales().getLocale("error_invalid_player")
                .ifPresent(player::sendMessage));
    }

    /*private void showEnderChestMenu(@NotNull OnlineUser player, @NotNull UserDataSnapshot userDataSnapshot,
                                    @NotNull User dataOwner, boolean allowEdit) {
        plugin.runAsync(() -> {
            final UserData data = userDataSnapshot.userData();
            data.getEnderChest().ifPresent(itemData -> {
                // Show message
                plugin.getLocales().getLocale("ender_chest_viewer_opened", dataOwner.getUsername(),
                                new SimpleDateFormat("MMM dd yyyy, HH:mm:ss.sss")
                                        .format(userDataSnapshot.versionTimestamp()))
                        .ifPresent(player::sendMessage);

                // Show inventory menu
                player.showMenu(itemData, allowEdit, 3, plugin.getLocales()
                                .getLocale("ender_chest_viewer_menu_title", dataOwner.getUsername())
                                .orElse(new MineDown("Ender Chest Viewer")))
                        .exceptionally(throwable -> {
                            plugin.log(Level.WARNING, "Exception displaying inventory menu to " + player.getUsername(), throwable);
                            return Optional.empty();
                        })
                        .thenAccept(dataOnClose -> {
                            if (dataOnClose.isEmpty() || !allowEdit) {
                                return;
                            }

                            // Create the updated data
                            final UserDataBuilder builder = UserData.builder(plugin.getMinecraftVersion());
                            data.getStatus().ifPresent(builder::setStatus);
                            data.getAdvancements().ifPresent(builder::setAdvancements);
                            data.getLocation().ifPresent(builder::setLocation);
                            data.getPersistentDataContainer().ifPresent(builder::setPersistentDataContainer);
                            data.getStatistics().ifPresent(builder::setStatistics);
                            data.getPotionEffects().ifPresent(builder::setPotionEffects);
                            data.getInventory().ifPresent(builder::setInventory);
                            builder.setEnderChest(dataOnClose.get());

                            // Set the updated data
                            final UserData updatedUserData = builder.build();
                            plugin.getDatabase().setUserData(dataOwner, updatedUserData, DataSaveCause.INVENTORY_COMMAND);
                            plugin.getRedisManager().sendUserDataUpdate(dataOwner, updatedUserData);
                        });
            });
        });
    }*/

    @Nullable
    @Override
    public List<String> suggest(@NotNull CommandUser executor, @NotNull String[] args) {
        return switch (args.length) {
            case 0, 1 -> plugin.getOnlineUsers().stream().map(User::getUsername).toList();
            default -> null;
        };
    }

}
