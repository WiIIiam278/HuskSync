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

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.william278.husksync.HuskSync;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.redis.RedisKeyType;
import net.william278.husksync.redis.RedisManager;
import net.william278.husksync.user.CommandUser;
import net.william278.husksync.user.User;
import net.william278.husksync.util.DataDumper;
import net.william278.husksync.util.DataSnapshotList;
import net.william278.husksync.util.DataSnapshotOverview;
import net.william278.uniform.BaseCommand;
import net.william278.uniform.CommandProvider;
import net.william278.uniform.Permission;
import net.william278.uniform.element.ArgumentElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class UserDataCommand extends PluginCommand {

    public UserDataCommand(@NotNull HuskSync plugin) {
        super("userdata", List.of("playerdata"), Permission.Default.IF_OP, ExecutionScope.ALL, plugin);
    }

    @Override
    public void provide(@NotNull BaseCommand<?> command) {
        command.addSubCommand("view", needsOp("view"), view());
        command.addSubCommand("list", needsOp("list"), list());
        command.addSubCommand("delete", needsOp("delete"), delete());
        command.addSubCommand("restore", needsOp("restore"), restore());
        command.addSubCommand("pin", needsOp("pin"), pin());
        command.addSubCommand("dump", needsOp("dump"), dump());
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
    private void dumpSnapshot(@NotNull CommandUser executor, @NotNull User user, @NotNull UUID version,
                              @NotNull DumpType type) {
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
                            (type == DumpType.WEB ? dumper.toWeb() : dumper.toFile()))
                    .ifPresent(executor::sendMessage);
        } catch (Throwable e) {
            plugin.log(Level.SEVERE, "Failed to dump user data", e);
        }
    }

    @NotNull
    private CommandProvider view() {
        return (sub) -> {
            sub.addSyntax((ctx) -> {
                final User user = ctx.getArgument("username", User.class);
                viewLatestSnapshot(user(sub, ctx), user);
            }, user("username"));
            sub.addSyntax((ctx) -> {
                final User user = ctx.getArgument("username", User.class);
                final UUID version = ctx.getArgument("version", UUID.class);
                viewSnapshot(user(sub, ctx), user, version);
            }, user("username"), uuid("version"));
        };
    }

    @NotNull
    private CommandProvider list() {
        return (sub) -> {
            sub.addSyntax((ctx) -> {
                final User user = ctx.getArgument("username", User.class);
                listSnapshots(user(sub, ctx), user, 1);
            }, user("username"));
            sub.addSyntax((ctx) -> {
                final User user = ctx.getArgument("username", User.class);
                final int page = ctx.getArgument("page", Integer.class);
                listSnapshots(user(sub, ctx), user, page);
            }, user("username"), BaseCommand.intNum("page", 1));
        };
    }

    @NotNull
    private CommandProvider delete() {
        return (sub) -> sub.addSyntax((ctx) -> {
            final User user = ctx.getArgument("username", User.class);
            final UUID version = ctx.getArgument("version", UUID.class);
            deleteSnapshot(user(sub, ctx), user, version);
        }, user("username"), uuid("version"));
    }

    @NotNull
    private CommandProvider restore() {
        return (sub) -> sub.addSyntax((ctx) -> {
            final User user = ctx.getArgument("username", User.class);
            final UUID version = ctx.getArgument("version", UUID.class);
            restoreSnapshot(user(sub, ctx), user, version);
        }, user("username"), uuid("version"));
    }

    @NotNull
    private CommandProvider pin() {
        return (sub) -> sub.addSyntax((ctx) -> {
            final User user = ctx.getArgument("username", User.class);
            final UUID version = ctx.getArgument("version", UUID.class);
            pinSnapshot(user(sub, ctx), user, version);
        }, user("username"), uuid("version"));
    }

    @NotNull
    private CommandProvider dump() {
        return (sub) -> sub.addSyntax((ctx) -> {
            final User user = ctx.getArgument("username", User.class);
            final UUID version = ctx.getArgument("version", UUID.class);
            final DumpType type = ctx.getArgument("type", DumpType.class);
            dumpSnapshot(user(sub, ctx), user, version, type);
        }, user("username"), uuid("version"), dumpType());
    }

    private <S> ArgumentElement<S, DumpType> dumpType() {
        return new ArgumentElement<>("type", reader -> {
            final String type = reader.readString();
            return switch (type.toLowerCase(Locale.ENGLISH)) {
                case "web" -> DumpType.WEB;
                case "file" -> DumpType.FILE;
                default -> throw CommandSyntaxException.BUILT_IN_EXCEPTIONS
                        .dispatcherUnknownArgument().createWithContext(reader);
            };
        }, (context, builder) -> {
            builder.suggest("web");
            builder.suggest("file");
            return builder.buildFuture();
        });
    }

    enum DumpType {
        WEB,
        FILE
    }

}
