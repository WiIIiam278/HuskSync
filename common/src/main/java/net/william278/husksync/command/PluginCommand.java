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

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.william278.husksync.HuskSync;
import net.william278.husksync.user.CommandUser;
import net.william278.husksync.user.User;
import net.william278.uniform.BaseCommand;
import net.william278.uniform.Command;
import net.william278.uniform.Permission;
import net.william278.uniform.element.ArgumentElement;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public abstract class PluginCommand extends Command {

    protected final HuskSync plugin;

    protected PluginCommand(@NotNull String name, @NotNull List<String> aliases, @NotNull Permission.Default defPerm,
                            @NotNull ExecutionScope scope, @NotNull HuskSync plugin) {
        super(name, aliases, getDescription(plugin, name), new Permission(createPermission(name), defPerm), scope);
        this.plugin = plugin;
    }

    private static String getDescription(@NotNull HuskSync plugin, @NotNull String name) {
        return plugin.getLocales().getRawLocale("%s_command_description".formatted(name)).orElse("");
    }

    @NotNull
    private static String createPermission(@NotNull String name, @NotNull String... sub) {
        return "husksync.command." + name + (sub.length > 0 ? "." + String.join(".", sub) : "");
    }

    @NotNull
    protected String getPermission(@NotNull String... sub) {
        return createPermission(this.getName(), sub);
    }

    @NotNull
    @SuppressWarnings("rawtypes")
    protected CommandUser user(@NotNull BaseCommand base, @NotNull CommandContext context) {
        return adapt(base.getUser(context.getSource()));
    }

    @NotNull
    protected Permission needsOp(@NotNull String... nodes) {
        return new Permission(getPermission(nodes), Permission.Default.IF_OP);
    }

    @NotNull
    protected CommandUser adapt(net.william278.uniform.CommandUser user) {
        return user.getUuid() == null ? plugin.getConsole() : plugin.getOnlineUser(user.getUuid()).orElseThrow();
    }

    @NotNull
    protected <S> ArgumentElement<S, User> user(@NotNull String name) {
        return new ArgumentElement<>(name, reader -> {
            final String username = reader.readString();
            return plugin.getDatabase().getUserByName(username).orElseThrow(
                    () -> CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader)
            );
        }, (context, builder) -> {
            plugin.getOnlineUsers().forEach(u -> builder.suggest(u.getUsername()));
            return builder.buildFuture();
        });
    }

    @NotNull
    protected <S> ArgumentElement<S, UUID> uuid(@NotNull String name) {
        return new ArgumentElement<>(name, reader -> {
            try {
                return UUID.fromString(reader.readString());
            } catch (IllegalArgumentException e) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader);
            }
        }, (context, builder) -> builder.buildFuture());
    }

    public enum Type {

        HUSKSYNC_COMMAND(HuskSyncCommand::new),
        USERDATA_COMMAND(UserDataCommand::new),
        INVENTORY_COMMAND(InventoryCommand::new),
        ENDER_CHEST_COMMAND(EnderChestCommand::new);

        public final Function<HuskSync, PluginCommand> commandSupplier;

        Type(@NotNull Function<HuskSync, PluginCommand> supplier) {
            this.commandSupplier = supplier;
        }

        @NotNull
        public PluginCommand supply(@NotNull HuskSync plugin) {
            return commandSupplier.apply(plugin);
        }

        @NotNull
        public static PluginCommand[] create(@NotNull HuskSync plugin) {
            return Arrays.stream(values()).map(type -> type.supply(plugin))
                    .filter(command -> !plugin.getSettings().isCommandDisabled(command))
                    .toArray(PluginCommand[]::new);
        }

    }

}