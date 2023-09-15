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
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.player.CommandUser;
import net.william278.husksync.player.User;
import net.william278.husksync.util.DataDumper;
import net.william278.husksync.util.DataSnapshotList;
import net.william278.husksync.util.DataSnapshotOverview;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class UserDataCommand extends Command implements TabProvider {

    private static final Map<String, Boolean> SUB_COMMANDS = Map.of(
            "view", false,
            "list", false,
            "delete", true,
            "restore", true,
            "pin", true,
            "dump", true
    );

    public UserDataCommand(@NotNull HuskSync plugin) {
        super("userdata", List.of("playerdata"), String.format(
                "<%s> <username> [version_uuid]", String.join("/", SUB_COMMANDS.keySet())
        ), plugin);
        setOperatorCommand(true);
        addAdditionalPermissions(SUB_COMMANDS);
    }

    @Override
    public void execute(@NotNull CommandUser executor, @NotNull String[] args) {
        if (args.length < 1) {
            plugin.getLocales().getLocale("error_invalid_syntax", getUsage())
                    .ifPresent(executor::sendMessage);
            return;
        }

        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "view" -> {
                if (args.length < 2) {
                    plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/userdata view <username> [version_uuid]")
                            .ifPresent(executor::sendMessage);
                    return;
                }
                final String username = args[1];
                if (args.length >= 3) {
                    try {
                        final UUID versionUuid = UUID.fromString(args[2]);
                        plugin.getDatabase()
                                .getUserByName(username.toLowerCase(Locale.ENGLISH))
                                .ifPresentOrElse(user -> plugin.getDatabase().getDataSnapshot(user, versionUuid).ifPresentOrElse(
                                                data -> DataSnapshotOverview.of(
                                                        data.unpack(plugin), data.getFileSize(plugin), user, plugin
                                                ).show(executor),
                                                () -> plugin.getLocales().getLocale("error_invalid_version_uuid")
                                                        .ifPresent(executor::sendMessage)),
                                        () -> plugin.getLocales().getLocale("error_invalid_player")
                                                .ifPresent(executor::sendMessage));
                    } catch (IllegalArgumentException e) {
                        plugin.getLocales().getLocale("error_invalid_syntax",
                                        "/userdata view <username> [version_uuid]")
                                .ifPresent(executor::sendMessage);
                    }
                } else {
                    plugin.getDatabase()
                            .getUserByName(username.toLowerCase(Locale.ENGLISH))
                            .ifPresentOrElse(user -> plugin.getDatabase().getCurrentUserData(user).ifPresentOrElse(
                                            data -> DataSnapshotOverview.of(
                                                    data.unpack(plugin), data.getFileSize(plugin), user, plugin
                                            ).show(executor),
                                            () -> plugin.getLocales().getLocale("error_no_data_to_display")
                                                    .ifPresent(executor::sendMessage)),
                                    () -> plugin.getLocales().getLocale("error_invalid_player")
                                            .ifPresent(executor::sendMessage));
                }
            }
            case "list" -> {
                if (args.length < 2) {
                    plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/userdata list <username> [page]")
                            .ifPresent(executor::sendMessage);
                    return;
                }
                final String username = args[1];
                plugin.getDatabase().getUserByName(username.toLowerCase(Locale.ENGLISH)).ifPresentOrElse(
                        (user) -> {
                            // Check if there is data to display
                            final List<DataSnapshot.Packed> dataList = plugin.getDatabase().getDataSnapshots(user);
                            if (dataList.isEmpty()) {
                                plugin.getLocales().getLocale("error_no_data_to_display")
                                        .ifPresent(executor::sendMessage);
                                return;
                            }

                            // Determine page to display
                            int page = 1;
                            if (args.length >= 3) {
                                try {
                                    page = Integer.parseInt(args[2]);
                                } catch (NumberFormatException e) {
                                    plugin.getLocales().getLocale("error_invalid_syntax",
                                                    "/userdata list <username> [page]")
                                            .ifPresent(executor::sendMessage);
                                    return;
                                }
                            }

                            // Show the list to the player
                            DataSnapshotList.create(dataList, user, plugin).displayPage(executor, page);
                        },
                        () -> plugin.getLocales().getLocale("error_invalid_player")
                                .ifPresent(executor::sendMessage));
            }
            case "delete" -> {
                // Delete user data by specified UUID
                if (args.length < 3) {
                    plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/userdata delete <username> <version_uuid>")
                            .ifPresent(executor::sendMessage);
                    return;
                }
                final String username = args[1];
                try {
                    final UUID versionUuid = UUID.fromString(args[2]);
                    plugin.getDatabase().getUserByName(username.toLowerCase(Locale.ENGLISH)).ifPresentOrElse(user -> {
                                if (plugin.getDatabase().deleteUserData(user, versionUuid)) {
                                    plugin.getLocales().getLocale("data_deleted",
                                                    versionUuid.toString().split("-")[0],
                                                    versionUuid.toString(),
                                                    user.getUsername(),
                                                    user.getUuid().toString())
                                            .ifPresent(executor::sendMessage);
                                } else {
                                    plugin.getLocales().getLocale("error_invalid_version_uuid")
                                            .ifPresent(executor::sendMessage);
                                }
                            },
                            () -> plugin.getLocales().getLocale("error_invalid_player")
                                    .ifPresent(executor::sendMessage));
                } catch (IllegalArgumentException e) {
                    plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/userdata delete <username> <version_uuid>")
                            .ifPresent(executor::sendMessage);
                }
            }
            case "restore" -> {
                // Get user data by specified uuid and username
                if (args.length < 3) {
                    plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/userdata restore <username> <version_uuid>")
                            .ifPresent(executor::sendMessage);
                    return;
                }
                final String username = args[1];
                try {
                    final UUID versionUuid = UUID.fromString(args[2]);
                    plugin.getDatabase().getUserByName(username.toLowerCase(Locale.ENGLISH)).ifPresentOrElse(
                            user -> {
                                final Optional<DataSnapshot.Packed> optionalData = plugin.getDatabase().getDataSnapshot(user, versionUuid);
                                if (optionalData.isEmpty()) {
                                    plugin.getLocales().getLocale("error_invalid_version_uuid")
                                            .ifPresent(executor::sendMessage);
                                    return;
                                }

                                // Restore users with a minimum of one health (prevent restoring players with <=0 health)
                                final DataSnapshot.Packed data = optionalData.get().copy();
                                data.edit(plugin, (unpacked -> unpacked.getHealth().ifPresent(
                                        status -> status.setHealth(Math.max(1, status.getHealth()))
                                )));

                                // Set the user's data and send a message
                                plugin.getDatabase().setUserData(user, data);
                                plugin.getRedisManager().sendUserDataUpdate(user, data);
                                plugin.getLocales().getLocale("data_restored",
                                                user.getUsername(),
                                                user.getUuid().toString(),
                                                versionUuid.toString().split("-")[0],
                                                versionUuid.toString())
                                        .ifPresent(executor::sendMessage);
                            },
                            () -> plugin.getLocales().getLocale("error_invalid_player")
                                    .ifPresent(executor::sendMessage));
                } catch (IllegalArgumentException e) {
                    plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/userdata restore <username> <version_uuid>")
                            .ifPresent(executor::sendMessage);
                }
            }
            case "pin" -> {
                if (args.length < 3) {
                    plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/userdata pin <username> <version_uuid>")
                            .ifPresent(executor::sendMessage);
                    return;
                }

                final String username = args[1];
                try {
                    final UUID versionUuid = UUID.fromString(args[2]);
                    plugin.getDatabase().getUserByName(username.toLowerCase(Locale.ENGLISH)).ifPresentOrElse(
                            user -> plugin.getDatabase().getDataSnapshot(user, versionUuid).ifPresentOrElse(userData -> {
                                if (userData.isPinned()) {
                                    plugin.getDatabase().unpinUserData(user, versionUuid);
                                    plugin.getLocales().getLocale("data_unpinned",
                                                    versionUuid.toString().split("-")[0],
                                                    versionUuid.toString(),
                                                    user.getUsername(),
                                                    user.getUuid().toString())
                                            .ifPresent(executor::sendMessage);
                                } else {
                                    plugin.getDatabase().pinUserData(user, versionUuid);
                                    plugin.getLocales().getLocale("data_pinned",
                                                    versionUuid.toString().split("-")[0],
                                                    versionUuid.toString(),
                                                    user.getUsername(),
                                                    user.getUuid().toString())
                                            .ifPresent(executor::sendMessage);
                                }
                            }, () -> plugin.getLocales().getLocale("error_invalid_version_uuid")
                                    .ifPresent(executor::sendMessage)),
                            () -> plugin.getLocales().getLocale("error_invalid_player")
                                    .ifPresent(executor::sendMessage));
                } catch (IllegalArgumentException e) {
                    plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/userdata pin <username> <version_uuid>")
                            .ifPresent(executor::sendMessage);
                }
            }
            case "dump" -> {
                if (args.length < 3) {
                    plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/userdata dump <username> <version_uuid>")
                            .ifPresent(executor::sendMessage);
                    return;
                }

                final boolean toWeb = args.length > 3 && args[3].equalsIgnoreCase("web");
                final String username = args[1];
                try {
                    final UUID versionUuid = UUID.fromString(args[2]);
                    plugin.getDatabase()
                            .getUserByName(username.toLowerCase(Locale.ENGLISH))
                            .ifPresentOrElse(
                                    user -> plugin.getDatabase().getDataSnapshot(user, versionUuid).ifPresentOrElse(userData -> {
                                        try {
                                            final DataDumper dumper = DataDumper.create(userData, user, plugin);
                                            final String result = toWeb ? dumper.toWeb() : dumper.toFile();
                                            plugin.getLocales().getLocale("data_dumped", versionUuid.toString()
                                                            .split("-")[0], user.getUsername(), result)
                                                    .ifPresent(executor::sendMessage);
                                        } catch (IOException e) {
                                            plugin.log(Level.SEVERE, "Failed to dump user data", e);
                                        }
                                    }, () -> plugin.getLocales().getLocale("error_invalid_version_uuid")
                                            .ifPresent(executor::sendMessage)),
                                    () -> plugin.getLocales().getLocale("error_invalid_player")
                                            .ifPresent(executor::sendMessage));
                } catch (IllegalArgumentException e) {
                    plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/userdata dump <username> <version_uuid>")
                            .ifPresent(executor::sendMessage);
                    plugin.log(Level.SEVERE, "Failed to dump user data", e);
                }
            }
            default -> plugin.getLocales().getLocale("error_invalid_syntax", getUsage())
                    .ifPresent(executor::sendMessage);
        }
    }

    @Nullable
    @Override
    public List<String> suggest(@NotNull CommandUser executor, @NotNull String[] args) {
        return switch (args.length) {
            case 0, 1 -> SUB_COMMANDS.keySet().stream().sorted().toList();
            case 2 -> plugin.getOnlineUsers().stream().map(User::getUsername).toList();
            default -> null;
        };
    }
}
