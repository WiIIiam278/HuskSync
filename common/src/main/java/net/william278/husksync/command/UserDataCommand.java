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
import net.william278.husksync.redis.RedisKeyType;
import net.william278.husksync.redis.RedisManager;
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
        final Optional<UUID> uuid = parseUUIDArg(args, 2).or(() -> parseUUIDArg(args, 1));
        if (optionalUser.isEmpty()) {
            plugin.getLocales().getLocale("error_invalid_player")
                    .ifPresent(executor::sendMessage);
            return;
        }

        final User user = optionalUser.get();
        switch (subCommand) {
            case "view" -> uuid.ifPresentOrElse(
                    version -> viewSnapshot(executor, user, version),
                    () -> viewLatestSnapshot(executor, user)
            );
            case "list" -> listSnapshots(
                    executor, user, parseIntArg(args, 2).or(() -> parseIntArg(args, 1)).orElse(1)
            );
            case "delete" -> uuid.ifPresentOrElse(
                    version -> deleteSnapshot(executor, user, version),
                    () -> plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/userdata delete <username> <version_uuid>")
                            .ifPresent(executor::sendMessage)
            );
            case "restore" -> uuid.ifPresentOrElse(
                    version -> restoreSnapshot(executor, user, version),
                    () -> plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/userdata restore <username> <version_uuid>")
                            .ifPresent(executor::sendMessage)
            );
            case "pin" -> uuid.ifPresentOrElse(
                    version -> pinSnapshot(executor, user, version),
                    () -> plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/userdata pin <username> <version_uuid>")
                            .ifPresent(executor::sendMessage)
            );
            case "dump" -> uuid.ifPresentOrElse(
                    version -> dumpSnapshot(executor, user, version, parseStringArg(args, 3)
                            .map(arg -> arg.equalsIgnoreCase("web")).orElse(false)),
                    () -> plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/userdata dump <web/file> <username> <version_uuid>")
                            .ifPresent(executor::sendMessage)
            );
            default -> plugin.getLocales().getLocale("error_invalid_syntax", getUsage())
                    .ifPresent(executor::sendMessage);
        }
    }

    // Show the latest snapshot
    private void viewLatestSnapshot(@NotNull CommandUser executor, @NotNull User user) {
        plugin.getDatabase().getLatestSnapshot(user).ifPresentOrElse(
                data -> {
                    if (data.isInvalid()) {
                        plugin.getLocales().getLocale("error_invalid_data", data.getInvalidReason(plugin))
                                .ifPresent(executor::sendMessage);
                        return;
                    }
                    DataSnapshotOverview.of(data.unpack(plugin), data.getFileSize(plugin), user, plugin)
                            .show(executor);
                },
                () -> plugin.getLocales().getLocale("error_no_data_to_display")
                        .ifPresent(executor::sendMessage)
        );
    }

    // Show the specified snapshot
    private void viewSnapshot(@NotNull CommandUser executor, @NotNull User user, @NotNull UUID version) {
        plugin.getDatabase().getSnapshot(user, version).ifPresentOrElse(
                data -> {
                    if (data.isInvalid()) {
                        plugin.getLocales().getLocale("error_invalid_data", data.getInvalidReason(plugin))
                                .ifPresent(executor::sendMessage);
                        return;
                    }
                    DataSnapshotOverview.of(data.unpack(plugin), data.getFileSize(plugin), user, plugin)
                            .show(executor);
                },
                () -> plugin.getLocales().getLocale("error_invalid_version_uuid")
                        .ifPresent(executor::sendMessage)
        );
    }

    // View a list of snapshots
    private void listSnapshots(@NotNull CommandUser executor, @NotNull User user, int page) {
        final List<DataSnapshot.Packed> dataList = plugin.getDatabase().getAllSnapshots(user);
        if (dataList.isEmpty()) {
            plugin.getLocales().getLocale("error_no_data_to_display")
                    .ifPresent(executor::sendMessage);
            return;
        }
        DataSnapshotList.create(dataList, user, plugin).displayPage(executor, page);
    }

    // Delete a snapshot
    private void deleteSnapshot(@NotNull CommandUser executor, @NotNull User user, @NotNull UUID version) {
        if (!plugin.getDatabase().deleteSnapshot(user, version)) {
            plugin.getLocales().getLocale("error_invalid_version_uuid")
                    .ifPresent(executor::sendMessage);
            return;
        }
        plugin.getRedisManager().clearUserData(user);
        plugin.getLocales().getLocale("data_deleted",
                        version.toString().split("-")[0],
                        version.toString(),
                        user.getUsername(),
                        user.getUuid().toString())
                .ifPresent(executor::sendMessage);
    }

    // Restore a snapshot
    private void restoreSnapshot(@NotNull CommandUser executor, @NotNull User user, @NotNull UUID version) {
        final Optional<DataSnapshot.Packed> optionalData = plugin.getDatabase().getSnapshot(user, version);
        if (optionalData.isEmpty()) {
            plugin.getLocales().getLocale("error_invalid_version_uuid")
                    .ifPresent(executor::sendMessage);
            return;
        }

        // Restore users with a minimum of one health (prevent restoring players with <= 0 health)
        final DataSnapshot.Packed data = optionalData.get().copy();
        if (data.isInvalid()) {
            plugin.getLocales().getLocale("error_invalid_data", data.getInvalidReason(plugin))
                    .ifPresent(executor::sendMessage);
            return;
        }
        data.edit(plugin, (unpacked -> {
            unpacked.getHealth().ifPresent(status -> status.setHealth(Math.max(1, status.getHealth())));
            unpacked.setSaveCause(DataSnapshot.SaveCause.BACKUP_RESTORE);
            unpacked.setPinned(
                    plugin.getSettings().getSynchronization().doAutoPin(DataSnapshot.SaveCause.BACKUP_RESTORE)
            );
        }));

        // Save data
        final RedisManager redis = plugin.getRedisManager();
        plugin.getDataSyncer().saveData(user, data, (u, s) -> {
            redis.getUserData(u).ifPresent(d -> redis.setUserData(u, s, RedisKeyType.TTL_1_YEAR));
            redis.sendUserDataUpdate(u, s);
            plugin.getLocales().getLocale("data_restored", u.getUsername(), u.getUuid().toString(),
                    s.getShortId(), s.getId().toString()).ifPresent(executor::sendMessage);
        });
    }

    // Pin a snapshot
    private void pinSnapshot(@NotNull CommandUser executor, @NotNull User user, @NotNull UUID version) {
        final Optional<DataSnapshot.Packed> optionalData = plugin.getDatabase().getSnapshot(user, version);
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

    // Dump a snapshot
    private void dumpSnapshot(@NotNull CommandUser executor, @NotNull User user, @NotNull UUID version, boolean webDump) {
        final Optional<DataSnapshot.Packed> data = plugin.getDatabase().getSnapshot(user, version);
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
