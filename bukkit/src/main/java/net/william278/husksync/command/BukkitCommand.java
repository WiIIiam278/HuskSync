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


import me.lucko.commodore.CommodoreProvider;
import net.william278.husksync.BukkitHuskSync;
import net.william278.husksync.user.BukkitUser;
import net.william278.husksync.user.CommandUser;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

public class BukkitCommand extends org.bukkit.command.Command {

    private final BukkitHuskSync plugin;
    private final Command command;

    public BukkitCommand(@NotNull Command command, @NotNull BukkitHuskSync plugin) {
        super(command.getName(), command.getDescription(), command.getUsage(), command.getAliases());
        this.command = command;
        this.plugin = plugin;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        this.command.onExecuted(sender instanceof Player p ? BukkitUser.adapt(p, plugin) : plugin.getConsole(), args);
        return true;
    }

    @NotNull
    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias,
                                    @NotNull String[] args) throws IllegalArgumentException {
        if (!(this.command instanceof TabProvider provider)) {
            return List.of();
        }
        final CommandUser user = sender instanceof Player p ? BukkitUser.adapt(p, plugin) : plugin.getConsole();
        if (getPermission() == null || user.hasPermission(getPermission())) {
            return provider.getSuggestions(user, args);
        }
        return List.of();
    }

    public void register() {
        // Register with bukkit
        plugin.getCommandRegistrar().getServerCommandMap().register("husksync", this);

        // Register permissions
        BukkitCommand.addPermission(
                plugin,
                command.getPermission(),
                command.getUsage(),
                BukkitCommand.getPermissionDefault(command.isOperatorCommand())
        );
        final List<Permission> childNodes = command.getAdditionalPermissions()
                .entrySet().stream()
                .map((entry) -> BukkitCommand.addPermission(
                        plugin,
                        entry.getKey(),
                        "",
                        BukkitCommand.getPermissionDefault(entry.getValue()))
                )
                .filter(Objects::nonNull)
                .toList();
        if (!childNodes.isEmpty()) {
            BukkitCommand.addPermission(
                    plugin,
                    command.getPermission("*"),
                    command.getUsage(),
                    PermissionDefault.FALSE,
                    childNodes.toArray(new Permission[0])
            );
        }

        // Register commodore TAB completion
        if (CommodoreProvider.isSupported() && plugin.getSettings().doBrigadierTabCompletion()) {
            BrigadierUtil.registerCommodore(plugin, this, command);
        }
    }

    @Nullable
    protected static Permission addPermission(@NotNull BukkitHuskSync plugin, @NotNull String node,
                                              @NotNull String description, @NotNull PermissionDefault permissionDefault,
                                              @NotNull Permission... children) {
        final Map<String, Boolean> childNodes = Arrays.stream(children)
                .map(Permission::getName)
                .collect(HashMap::new, (map, child) -> map.put(child, true), HashMap::putAll);

        final PluginManager manager = plugin.getServer().getPluginManager();
        if (manager.getPermission(node) != null) {
            return null;
        }

        Permission permission;
        if (description.isEmpty()) {
            permission = new Permission(node, permissionDefault, childNodes);
        } else {
            permission = new Permission(node, description, permissionDefault, childNodes);
        }
        manager.addPermission(permission);

        return permission;
    }

    @NotNull
    protected static PermissionDefault getPermissionDefault(boolean isOperatorCommand) {
        return isOperatorCommand ? PermissionDefault.OP : PermissionDefault.TRUE;
    }

    /**
     * Commands available on the Bukkit HuskSync implementation
     */
    public enum Type {

        HUSKSYNC_COMMAND(HuskSyncCommand::new),
        USERDATA_COMMAND(UserDataCommand::new),
        INVENTORY_COMMAND(InventoryCommand::new),
        ENDER_CHEST_COMMAND(EnderChestCommand::new);

        public final Function<BukkitHuskSync, Command> commandSupplier;

        Type(@NotNull Function<BukkitHuskSync, Command> supplier) {
            this.commandSupplier = supplier;
        }

        @NotNull
        public Command createCommand(@NotNull BukkitHuskSync plugin) {
            return commandSupplier.apply(plugin);
        }

        public static void registerCommands(@NotNull BukkitHuskSync plugin) {
            Arrays.stream(values())
                    .map((type) -> type.createCommand(plugin))
                    .forEach((command) -> new BukkitCommand(command, plugin).register());
        }


    }
}
