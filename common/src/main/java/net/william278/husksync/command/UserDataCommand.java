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
import net.william278.husksync.user.CommandUser;
import net.william278.husksync.user.User;
import net.william278.husksync.util.DataDumper;
import net.william278.husksync.util.DataSnapshotList;
import net.william278.husksync.util.DataSnapshotOverview;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
                "<%s> [username] [version_uuid]", String.join("/", SUB_COMMANDS.keySet())
        ), plugin);
        setOperatorCommand(true);
        addAdditionalPermissions(SUB_COMMANDS);
    }

    @Override
    public void execute(@NotNull CommandUser executor, @NotNull String[] args) {
        final String subCommand = parseStringArg(args, 0).orElse("view").toLowerCase(Locale.ENGLISH);
        final Optional<User> optionalUser = parseStringArg(args, 1)
                .flatMap(name -> plugin.getDatabase().getUserByName(name))
                .or(() -> parseStringArg(args, 0).flatMap(name -> plugin.getDatabase().getUserByName(name)))
                .or(() -> args.length < 2 && executor instanceof User userExecutor
                        ? Optional.of(userExecutor) : Optional.empty());
        final Optional<UUID> optionalUuid = parseUUIDArg(args, 2).or(() -> parseUUIDArg(args, 1));
        if (optionalUser.isEmpty()) {
            plugin.getLocales().getLocale("error_invalid_player")
                    .ifPresent(executor::sendMessage);
            return;
        }

        final User user = optionalUser.get();
        switch (subCommand) {
            case "view" -> optionalUuid.ifPresentOrElse(
                    // Show the specified snapshot
                    version -> plugin.getDatabase().getSnapshot(user, version).ifPresentOrElse(
                            data -> DataSnapshotOverview.of(
                                    data.unpack(plugin), data.getFileSize(plugin), user, plugin
                            ).show(executor),
                            () -> plugin.getLocales().getLocale("error_invalid_version_uuid")
                                    .ifPresent(executor::sendMessage)),

                    // Show the latest snapshot
                    () -> plugin.getDatabase().getLatestSnapshot(user).ifPresentOrElse(
                            data -> DataSnapshotOverview.of(
                                    data.unpack(plugin), data.getFileSize(plugin), user, plugin
                            ).show(executor),
                            () -> plugin.getLocales().getLocale("error_no_data_to_display")
                                    .ifPresent(executor::sendMessage))
            );

            case "list" -> {
                // Check if there is data to display
                final List<DataSnapshot.Packed> dataList = plugin.getDatabase().getAllSnapshots(user);
                if (dataList.isEmpty()) {
                    plugin.getLocales().getLocale("error_no_data_to_display")
                            .ifPresent(executor::sendMessage);
                    return;
                }

                // Show the list to the player
                DataSnapshotList.create(dataList, user, plugin).displayPage(
                        executor,
                        parseIntArg(args, 2).or(() -> parseIntArg(args, 1)).orElse(1)
                );
            }

            case "delete" -> {
                if (optionalUuid.isEmpty()) {
                    plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/userdata delete <username> <version_uuid>")
                            .ifPresent(executor::sendMessage);
                    return;
                }

                // Delete user data by specified UUID
                final UUID version = optionalUuid.get();
                if (!plugin.getDatabase().deleteSnapshot(user, version)) {
                    plugin.getLocales().getLocale("error_invalid_version_uuid")
                            .ifPresent(executor::sendMessage);
                    return;
                }

                plugin.getLocales().getLocale("data_deleted",
                                version.toString().split("-")[0],
                                version.toString(),
                                user.getUsername(),
                                user.getUuid().toString())
                        .ifPresent(executor::sendMessage);
            }

            case "restore" -> {
                if (optionalUuid.isEmpty()) {
                    plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/userdata restore <username> <version_uuid>")
                            .ifPresent(executor::sendMessage);
                    return;
                }

                // Restore user data by specified UUID
                final Optional<DataSnapshot.Packed> optionalData = plugin.getDatabase().getSnapshot(user, optionalUuid.get());
                if (optionalData.isEmpty()) {
                    plugin.getLocales().getLocale("error_invalid_version_uuid")
                            .ifPresent(executor::sendMessage);
                    return;
                }

                // Restore users with a minimum of one health (prevent restoring players with <=0 health)
                final DataSnapshot.Packed data = optionalData.get().copy();
                data.edit(plugin, (unpacked -> {
                    unpacked.getHealth().ifPresent(status -> status.setHealth(Math.max(1, status.getHealth())));
                    unpacked.setSaveCause(DataSnapshot.SaveCause.BACKUP_RESTORE);
                    unpacked.setPinned(plugin.getSettings().doAutoPin(DataSnapshot.SaveCause.BACKUP_RESTORE));
                }));

                // Set the user's data and send a message
                plugin.getDatabase().addSnapshot(user, data);
                plugin.getRedisManager().sendUserDataUpdate(user, data);
                plugin.getLocales().getLocale("data_restored", user.getUsername(), user.getUuid().toString(),
                        data.getShortId(), data.getId().toString()).ifPresent(executor::sendMessage);
            }

            case "pin" -> {
                if (optionalUuid.isEmpty()) {
                    plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/userdata pin <username> <version_uuid>")
                            .ifPresent(executor::sendMessage);
                    return;
                }

                // Check that the data exists
                final Optional<DataSnapshot.Packed> optionalData = plugin.getDatabase().getSnapshot(user, optionalUuid.get());
                if (optionalData.isEmpty()) {
                    plugin.getLocales().getLocale("error_invalid_version_uuid")
                            .ifPresent(executor::sendMessage);
                    return;
                }

                // Pin or unpin the data
                final DataSnapshot.Packed data = optionalData.get();
                if (data.isPinned()) {
                    plugin.getDatabase().unpinSnapshot(user, data.getId());
                } else {
                    plugin.getDatabase().pinSnapshot(user, data.getId());
                }
                plugin.getLocales().getLocale(data.isPinned() ? "data_unpinned" : "data_pinned", data.getShortId(),
                                data.getId().toString(), user.getUsername(), user.getUuid().toString())
                        .ifPresent(executor::sendMessage);
            }

            case "dump" -> {
                if (optionalUuid.isEmpty()) {
                    plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/userdata dump <username> <version_uuid>")
                            .ifPresent(executor::sendMessage);
                    return;
                }

                // Determine dump type
                final boolean webDump = parseStringArg(args, 3)
                        .map(arg -> arg.equalsIgnoreCase("web"))
                        .orElse(false);
                final Optional<DataSnapshot.Packed> data = plugin.getDatabase().getSnapshot(user, optionalUuid.get());
                if (data.isEmpty()) {
                    plugin.getLocales().getLocale("error_invalid_version_uuid")
                            .ifPresent(executor::sendMessage);
                    return;
                }

                // Dump the data
                final DataSnapshot.Packed userData = data.get();
                final DataDumper dumper = DataDumper.create(userData, user, plugin);
                try {
                    plugin.getLocales().getLocale("data_dumped", userData.getShortId(), user.getUsername(),
                            (webDump ? dumper.toWeb() : dumper.toFile())).ifPresent(executor::sendMessage);
                } catch (Throwable e) {
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
            case 4 -> parseStringArg(args, 0)
                    .map(arg -> arg.equalsIgnoreCase("dump") ? List.of("web", "file") : null)
                    .orElse(null);
            default -> null;
        };
    }
}
