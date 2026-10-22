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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.william278.husksync.HuskSync;
import net.william278.husksync.user.CommandUser;
import net.william278.husksync.user.OnlineUser;
import net.william278.husksync.user.User;
import net.william278.uniform.BaseCommand;
import net.william278.uniform.Command;
import net.william278.uniform.Permission;
import net.william278.uniform.element.ArgumentElement;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class PluginCommand extends Command {

    private static final Cache<String, Optional<User>> USER_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(200)
            .build();
    private static final Cache<UUID, List<String>> SNAPSHOT_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .maximumSize(100)
            .build();
    private static final Map<UUID, CompletableFuture<List<String>>> IN_FLIGHT_SNAPSHOTS = new ConcurrentHashMap<>();

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
    @SuppressWarnings("SameParameterValue")
    protected <S> ArgumentElement<S, OnlineUser> onlineUser(@NotNull String name) {
        return new ArgumentElement<>(name, reader -> {
            final String username = reader.readString();
            return plugin.getOnlineUsers().stream()
                    .filter(user -> username.equals(user.getName()))
                    .findFirst().orElse(null);
        }, (context, builder) -> {
            plugin.getOnlineUsers().forEach(u -> builder.suggest(u.getName()));
            return builder.buildFuture();
        });
    }

    @NotNull
    protected <S> ArgumentElement<S, String> user(@NotNull String name) {
        return new ArgumentElement<>(name, reader -> reader.readString(),
                (context, builder) -> {
                    plugin.getOnlineUsers().forEach(u -> builder.suggest(u.getName()));
                    return builder.buildFuture();
                });
    }

    protected void executeWithUser(@NotNull CommandUser executor, @NotNull String username,
                                   @NotNull Consumer<User> action) {
        plugin.runAsync(() -> findUser(username).ifPresentOrElse(
                action,
                () -> plugin.getLocales().getLocale("error_invalid_player").ifPresent(executor::sendMessage)
        ));
    }

    protected Optional<User> findUser(@NotNull String username) {
        final Optional<User> online = plugin.getOnlineUsers().stream()
                .filter(u -> username.equalsIgnoreCase(u.getName()))
                .map(u -> (User) u)
                .findFirst();
        if (online.isPresent()) {
            return online;
        }
        final String key = username.toLowerCase();
        final Optional<User> cached = USER_CACHE.getIfPresent(key);
        if (cached != null) {
            return cached;
        }
        final Optional<User> user = plugin.getDatabase().getUserByName(username);
        USER_CACHE.put(key, user);
        return user;
    }

    @NotNull
    protected  <S> ArgumentElement<S, UUID> versionUuid() {
        return new ArgumentElement<>("version", reader -> {
            try {
                return UUID.fromString(reader.readString());
            } catch (IllegalArgumentException e) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader);
            }
        }, (context, builder) -> {
            try {
                final String username = context.getArgument("username", String.class);
                return getSnapshotSuggestions(username)
                        .thenApply(ids -> {
                            ids.forEach(builder::suggest);
                            return builder.build();
                        })
                        .exceptionally(throwable -> builder.build());
            } catch (IllegalArgumentException ignored) {
                return builder.buildFuture();
            }
        });
    }

    @NotNull
    private CompletableFuture<List<String>> getSnapshotSuggestions(@NotNull String username) {
        final Optional<User> online = plugin.getOnlineUsers().stream()
                .filter(u -> username.equalsIgnoreCase(u.getName()))
                .map(u -> (User) u)
                .findFirst();
        if (online.isPresent()) {
            return getSnapshotSuggestions(online.get());
        }
        final String key = username.toLowerCase();
        final Optional<User> cached = USER_CACHE.getIfPresent(key);
        if (cached != null) {
            return cached.map(this::getSnapshotSuggestions)
                    .orElseGet(() -> CompletableFuture.completedFuture(List.of()));
        }
        return plugin.supplyAsync(() -> findUser(username))
                .thenCompose(user -> user.map(this::getSnapshotSuggestions)
                        .orElseGet(() -> CompletableFuture.completedFuture(List.of())));
    }

    @NotNull
    private CompletableFuture<List<String>> getSnapshotSuggestions(@NotNull User user) {
        final UUID uuid = user.getUuid();
        final List<String> cached = SNAPSHOT_CACHE.getIfPresent(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return IN_FLIGHT_SNAPSHOTS.computeIfAbsent(uuid, k -> plugin.supplyAsync(() -> plugin.getDatabase().getAllSnapshots(user))
                .thenApply(snapshots -> snapshots.stream()
                        .sorted(Comparator.comparing(d -> d.getTimestamp().toEpochSecond()))
                        .map(snapshot -> snapshot.getId().toString())
                        .toList())
                .whenComplete((ids, throwable) -> {
                    IN_FLIGHT_SNAPSHOTS.remove(uuid);
                    SNAPSHOT_CACHE.put(uuid, ids != null ? ids : List.of());
                }));
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