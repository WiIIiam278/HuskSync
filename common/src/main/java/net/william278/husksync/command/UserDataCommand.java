package net.william278.husksync.command;

import net.william278.husksync.HuskSync;
import net.william278.husksync.data.DataSaveCause;
import net.william278.husksync.data.UserData;
import net.william278.husksync.util.DataSnapshotList;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.util.DataDumper;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class UserDataCommand extends CommandBase implements TabCompletable {

    private final String[] COMMAND_ARGUMENTS = {"view", "list", "delete", "restore", "pin", "dump"};

    public UserDataCommand(@NotNull HuskSync implementor) {
        super("userdata", Permission.COMMAND_USER_DATA, implementor, "playerdata");
    }

    @Override
    public void onExecute(@NotNull OnlineUser player, @NotNull String[] args) {
        if (args.length < 1) {
            plugin.getLocales().getLocale("error_invalid_syntax",
                            "/userdata <view/list/delete/restore/pin/dump> <username> [version_uuid]")
                    .ifPresent(player::sendMessage);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "view" -> {
                if (args.length < 2) {
                    plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/userdata view <username> [version_uuid]")
                            .ifPresent(player::sendMessage);
                    return;
                }
                final String username = args[1];
                if (args.length >= 3) {
                    try {
                        final UUID versionUuid = UUID.fromString(args[2]);
                        CompletableFuture.runAsync(() -> plugin.getDatabase()
                                .getUserByName(username.toLowerCase())
                                .thenAccept(optionalUser -> optionalUser
                                        .ifPresentOrElse(user -> plugin.getDatabase().getUserData(user, versionUuid)
                                                        .thenAccept(data -> data.ifPresentOrElse(
                                                                userData -> userData.displayDataOverview(player, user, plugin.getLocales()),
                                                                () -> plugin.getLocales().getLocale("error_invalid_version_uuid")
                                                                        .ifPresent(player::sendMessage))),
                                                () -> plugin.getLocales().getLocale("error_invalid_player")
                                                        .ifPresent(player::sendMessage))));
                    } catch (IllegalArgumentException e) {
                        plugin.getLocales().getLocale("error_invalid_syntax",
                                        "/userdata view <username> [version_uuid]")
                                .ifPresent(player::sendMessage);
                    }
                } else {
                    CompletableFuture.runAsync(() -> plugin.getDatabase()
                            .getUserByName(username.toLowerCase())
                            .thenAccept(optionalUser -> optionalUser
                                    .ifPresentOrElse(user -> plugin.getDatabase().getCurrentUserData(user)
                                                    .thenAccept(latestData -> latestData.ifPresentOrElse(
                                                            userData -> userData.displayDataOverview(player, user, plugin.getLocales()),
                                                            () -> plugin.getLocales().getLocale("error_no_data_to_display")
                                                                    .ifPresent(player::sendMessage))),
                                            () -> plugin.getLocales().getLocale("error_invalid_player")
                                                    .ifPresent(player::sendMessage))));
                }
            }
            case "list" -> {
                if (!player.hasPermission(Permission.COMMAND_USER_DATA_MANAGE.node)) {
                    plugin.getLocales().getLocale("error_no_permission").ifPresent(player::sendMessage);
                    return;
                }
                if (args.length < 2) {
                    plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/userdata list <username> [page]")
                            .ifPresent(player::sendMessage);
                    return;
                }
                final String username = args[1];
                CompletableFuture.runAsync(() -> plugin.getDatabase()
                        .getUserByName(username.toLowerCase())
                        .thenAccept(optionalUser -> optionalUser.ifPresentOrElse(
                                user -> plugin.getDatabase().getUserData(user).thenAccept(dataList -> {
                                    // Check if there is data to display
                                    if (dataList.isEmpty()) {
                                        plugin.getLocales().getLocale("error_no_data_to_display")
                                                .ifPresent(player::sendMessage);
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
                                                    .ifPresent(player::sendMessage);
                                            return;
                                        }
                                    }

                                    // Show the list to the player
                                    DataSnapshotList.create(dataList, user, plugin.getLocales())
                                            .displayPage(player, page);
                                }),
                                () -> plugin.getLocales().getLocale("error_invalid_player")
                                        .ifPresent(player::sendMessage))));
            }
            case "delete" -> {
                if (!player.hasPermission(Permission.COMMAND_USER_DATA_MANAGE.node)) {
                    plugin.getLocales().getLocale("error_no_permission").ifPresent(player::sendMessage);
                    return;
                }
                // Delete user data by specified UUID
                if (args.length < 3) {
                    plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/userdata delete <username> <version_uuid>")
                            .ifPresent(player::sendMessage);
                    return;
                }
                final String username = args[1];
                try {
                    final UUID versionUuid = UUID.fromString(args[2]);
                    CompletableFuture.runAsync(() -> plugin.getDatabase()
                            .getUserByName(username.toLowerCase())
                            .thenAccept(optionalUser -> optionalUser.ifPresentOrElse(
                                    user -> plugin.getDatabase().deleteUserData(user, versionUuid).thenAccept(deleted -> {
                                        if (deleted) {
                                            plugin.getLocales().getLocale("data_deleted",
                                                            versionUuid.toString().split("-")[0],
                                                            versionUuid.toString(),
                                                            user.username,
                                                            user.uuid.toString())
                                                    .ifPresent(player::sendMessage);
                                        } else {
                                            plugin.getLocales().getLocale("error_invalid_version_uuid")
                                                    .ifPresent(player::sendMessage);
                                        }
                                    }),
                                    () -> plugin.getLocales().getLocale("error_invalid_player")
                                            .ifPresent(player::sendMessage))));
                } catch (IllegalArgumentException e) {
                    plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/userdata delete <username> <version_uuid>")
                            .ifPresent(player::sendMessage);
                }
            }
            case "restore" -> {
                if (!player.hasPermission(Permission.COMMAND_USER_DATA_MANAGE.node)) {
                    plugin.getLocales().getLocale("error_no_permission").ifPresent(player::sendMessage);
                    return;
                }
                // Get user data by specified uuid and username
                if (args.length < 3) {
                    plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/userdata restore <username> <version_uuid>")
                            .ifPresent(player::sendMessage);
                    return;
                }
                final String username = args[1];
                try {
                    final UUID versionUuid = UUID.fromString(args[2]);
                    CompletableFuture.runAsync(() -> plugin.getDatabase()
                            .getUserByName(username.toLowerCase())
                            .thenAccept(optionalUser -> optionalUser.ifPresentOrElse(
                                    user -> plugin.getDatabase().getUserData(user, versionUuid).thenAccept(data -> {
                                        if (data.isEmpty()) {
                                            plugin.getLocales().getLocale("error_invalid_version_uuid")
                                                    .ifPresent(player::sendMessage);
                                            return;
                                        }

                                        // Restore users with a minimum of one health (prevent restoring players with <=0 health)
                                        final UserData userData = data.get().userData();
                                        userData.getStatus().ifPresent(status -> status.health = Math.max(1, status.health));

                                        // Set the users data and send a message
                                        plugin.getDatabase().setUserData(user, userData, DataSaveCause.BACKUP_RESTORE);
                                        plugin.getRedisManager().sendUserDataUpdate(user, data.get().userData()).join();
                                        plugin.getLocales().getLocale("data_restored",
                                                        user.username,
                                                        user.uuid.toString(),
                                                        versionUuid.toString().split("-")[0],
                                                        versionUuid.toString())
                                                .ifPresent(player::sendMessage);
                                    }),
                                    () -> plugin.getLocales().getLocale("error_invalid_player")
                                            .ifPresent(player::sendMessage))));
                } catch (IllegalArgumentException e) {
                    plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/userdata restore <username> <version_uuid>")
                            .ifPresent(player::sendMessage);
                }
            }
            case "pin" -> {
                if (!player.hasPermission(Permission.COMMAND_USER_DATA_MANAGE.node)) {
                    plugin.getLocales().getLocale("error_no_permission").ifPresent(player::sendMessage);
                    return;
                }
                if (args.length < 3) {
                    plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/userdata pin <username> <version_uuid>")
                            .ifPresent(player::sendMessage);
                    return;
                }

                final String username = args[1];
                try {
                    final UUID versionUuid = UUID.fromString(args[2]);
                    CompletableFuture.runAsync(() -> plugin.getDatabase()
                            .getUserByName(username.toLowerCase())
                            .thenAccept(optionalUser -> optionalUser.ifPresentOrElse(
                                    user -> plugin.getDatabase().getUserData(user, versionUuid).thenAccept(
                                            optionalUserData -> optionalUserData.ifPresentOrElse(userData -> {
                                                if (userData.pinned()) {
                                                    plugin.getDatabase().unpinUserData(user, versionUuid).join();
                                                    plugin.getLocales().getLocale("data_unpinned",
                                                                    versionUuid.toString().split("-")[0],
                                                                    versionUuid.toString(),
                                                                    user.username,
                                                                    user.uuid.toString())
                                                            .ifPresent(player::sendMessage);
                                                } else {
                                                    plugin.getDatabase().pinUserData(user, versionUuid).join();
                                                    plugin.getLocales().getLocale("data_pinned",
                                                                    versionUuid.toString().split("-")[0],
                                                                    versionUuid.toString(),
                                                                    user.username,
                                                                    user.uuid.toString())
                                                            .ifPresent(player::sendMessage);
                                                }
                                            }, () -> plugin.getLocales().getLocale("error_invalid_version_uuid")
                                                    .ifPresent(player::sendMessage))),
                                    () -> plugin.getLocales().getLocale("error_invalid_player")
                                            .ifPresent(player::sendMessage))));
                } catch (IllegalArgumentException e) {
                    plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/userdata pin <username> <version_uuid>")
                            .ifPresent(player::sendMessage);
                }
            }
            case "dump" -> {
                if (!player.hasPermission(Permission.COMMAND_USER_DATA_DUMP.node)) {
                    plugin.getLocales().getLocale("error_no_permission").ifPresent(player::sendMessage);
                    return;
                }
                if (args.length < 3) {
                    plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/userdata dump <username> <version_uuid>")
                            .ifPresent(player::sendMessage);
                    return;
                }

                final boolean toWeb = args.length > 3 && args[3].equalsIgnoreCase("web");
                final String username = args[1];
                try {
                    final UUID versionUuid = UUID.fromString(args[2]);
                    CompletableFuture.runAsync(() -> plugin.getDatabase()
                            .getUserByName(username.toLowerCase())
                            .thenAccept(optionalUser -> optionalUser.ifPresentOrElse(
                                    user -> plugin.getDatabase().getUserData(user, versionUuid).thenAccept(
                                            optionalUserData -> optionalUserData.ifPresentOrElse(userData -> {
                                                try {
                                                    final DataDumper dumper = DataDumper.create(userData, user, plugin);
                                                    final String result = toWeb ? dumper.toWeb() : dumper.toFile();
                                                    plugin.getLocales().getLocale("data_dumped", versionUuid.toString()
                                                                    .split("-")[0], user.username, result)
                                                            .ifPresent(player::sendMessage);
                                                } catch (IOException e) {
                                                    plugin.getLoggingAdapter().log(Level.SEVERE, "Failed to dump user data", e);
                                                }
                                            }, () -> plugin.getLocales().getLocale("error_invalid_version_uuid")
                                                    .ifPresent(player::sendMessage))),
                                    () -> plugin.getLocales().getLocale("error_invalid_player")
                                            .ifPresent(player::sendMessage))));
                } catch (IllegalArgumentException e) {
                    plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/userdata dump <username> <version_uuid>")
                            .ifPresent(player::sendMessage);
                }
            }
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull String[] args) {
        switch (args.length) {
            case 0, 1 -> {
                return Arrays.stream(COMMAND_ARGUMENTS)
                        .filter(argument -> argument.startsWith(args.length == 1 ? args[0] : ""))
                        .sorted().collect(Collectors.toList());
            }
            case 2 -> {
                return plugin.getOnlineUsers().stream().map(user -> user.username)
                        .filter(argument -> argument.startsWith(args[1]))
                        .sorted().collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }
}
