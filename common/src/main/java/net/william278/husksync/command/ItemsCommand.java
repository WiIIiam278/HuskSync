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
import net.william278.husksync.user.OnlineUser;
import net.william278.husksync.user.User;
import net.william278.uniform.BaseCommand;
import net.william278.uniform.Permission;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public abstract class ItemsCommand extends PluginCommand {

    protected ItemsCommand(@NotNull String name, @NotNull List<String> aliases, @NotNull HuskSync plugin) {
        super(name, aliases, Permission.Default.IF_OP, ExecutionScope.IN_GAME, plugin);
    }

    @Override
    public void provide(@NotNull BaseCommand<?> command) {
        command.addSyntax((ctx) -> {
            final User user = ctx.getArgument("username", User.class);
            final UUID version = ctx.getArgument("version", UUID.class);
            final CommandUser executor = user(command, ctx);
            if (!(executor instanceof OnlineUser online)) {
                plugin.getLocales().getLocale("error_in_game_command_only")
                        .ifPresent(executor::sendMessage);
                return;
            }
            this.showSnapshotItems(online, user, version);
        }, user("username"), uuid("version"));
        command.addSyntax((ctx) -> {
            final User user = ctx.getArgument("username", User.class);
            final CommandUser executor = user(command, ctx);
            if (!(executor instanceof OnlineUser online)) {
                plugin.getLocales().getLocale("error_in_game_command_only")
                        .ifPresent(executor::sendMessage);
                return;
            }
            this.showLatestItems(online, user);
        }, user("username"));
    }

    // View (and edit) the latest user data
    private void showLatestItems(@NotNull OnlineUser viewer, @NotNull User user) {
        plugin.getRedisManager().getUserData(user.getUuid(), user).thenAccept(data -> data
                .or(() -> plugin.getDatabase().getLatestSnapshot(user))
                .or(() -> {
                    plugin.getLocales().getLocale("error_no_data_to_display")
                            .ifPresent(viewer::sendMessage);
                    return Optional.empty();
                })
                .flatMap(packed -> {
                    if (packed.isInvalid()) {
                        plugin.getLocales().getLocale("error_invalid_data", packed.getInvalidReason(plugin))
                                .ifPresent(viewer::sendMessage);
                        return Optional.empty();
                    }
                    return Optional.of(packed.unpack(plugin));
                })
                .ifPresent(snapshot -> this.showItems(
                        viewer, snapshot, user, viewer.hasPermission(getPermission("edit"))
                )));
    }

    // View a specific version of the user data
    private void showSnapshotItems(@NotNull OnlineUser viewer, @NotNull User user, @NotNull UUID version) {
        plugin.getDatabase().getSnapshot(user, version)
                .or(() -> {
                    plugin.getLocales().getLocale("error_invalid_version_uuid")
                            .ifPresent(viewer::sendMessage);
                    return Optional.empty();
                })
                .flatMap(packed -> {
                    if (packed.isInvalid()) {
                        plugin.getLocales().getLocale("error_invalid_data", packed.getInvalidReason(plugin))
                                .ifPresent(viewer::sendMessage);
                        return Optional.empty();
                    }
                    return Optional.of(packed.unpack(plugin));
                })
                .ifPresent(snapshot -> this.showItems(
                        viewer, snapshot, user, false
                ));
    }

    // Show a GUI menu with the correct item data from the snapshot
    protected abstract void showItems(@NotNull OnlineUser viewer, @NotNull DataSnapshot.Unpacked snapshot,
                                      @NotNull User user, boolean allowEdit);

}
