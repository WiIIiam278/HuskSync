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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public abstract class ItemsCommand extends Command implements TabProvider {

    protected ItemsCommand(@NotNull HuskSync plugin, @NotNull List<String> aliases) {
        super(aliases.get(0), aliases.subList(1, aliases.size()), "<player> [version_uuid]", plugin);
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

        // Find the user to view the items for
        final Optional<User> optionalUser = parseStringArg(args, 0)
                .flatMap(name -> plugin.getDatabase().getUserByName(name));
        if (optionalUser.isEmpty()) {
            plugin.getLocales().getLocale(
                    args.length >= 1 ? "error_invalid_player" : "error_invalid_syntax", getUsage()
            ).ifPresent(player::sendMessage);
            return;
        }

        // Show the user data
        final User user = optionalUser.get();
        parseUUIDArg(args, 1).ifPresentOrElse(
                version -> this.showSnapshotItems(player, user, version),
                () -> this.showLatestItems(player, user)
        );
    }

    // View (and edit) the latest user data
    private void showLatestItems(@NotNull OnlineUser viewer, @NotNull User user) {
        plugin.getRedisManager().getUserData(user.getUuid(), user).thenAccept(data -> data
                .or(() -> plugin.getDatabase().getLatestSnapshot(user))
                .ifPresentOrElse(
                        snapshot -> this.showItems(
                                viewer, snapshot.unpack(plugin), user,
                                viewer.hasPermission(getPermission("edit"))
                        ),
                        () -> plugin.getLocales().getLocale("error_no_data_to_display")
                                .ifPresent(viewer::sendMessage)
                ));
    }

    // View a specific version of the user data
    private void showSnapshotItems(@NotNull OnlineUser viewer, @NotNull User user, @NotNull UUID version) {
        plugin.getDatabase().getSnapshot(user, version)
                .ifPresentOrElse(
                        snapshot -> this.showItems(
                                viewer, snapshot.unpack(plugin), user, false
                        ),
                        () -> plugin.getLocales().getLocale("error_invalid_version_uuid")
                                .ifPresent(viewer::sendMessage)
                );
    }

    // Show a GUI menu with the correct item data from the snapshot
    protected abstract void showItems(@NotNull OnlineUser viewer, @NotNull DataSnapshot.Unpacked snapshot,
                                      @NotNull User user, boolean allowEdit);

    @Nullable
    @Override
    public List<String> suggest(@NotNull CommandUser executor, @NotNull String[] args) {
        return switch (args.length) {
            case 0, 1 -> plugin.getOnlineUsers().stream().map(User::getUsername).toList();
            default -> null;
        };
    }
}
