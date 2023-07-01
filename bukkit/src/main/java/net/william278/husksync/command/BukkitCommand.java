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
import net.william278.husksync.player.BukkitPlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Bukkit executor that implements and executes {@link CommandBase}s
 */
public class BukkitCommand implements CommandExecutor, TabExecutor {

    /**
     * The {@link CommandBase} that will be executed
     */
    protected final CommandBase command;

    /**
     * The implementing plugin
     */
    private final BukkitHuskSync plugin;

    public BukkitCommand(@NotNull CommandBase command, @NotNull BukkitHuskSync implementor) {
        this.command = command;
        this.plugin = implementor;
    }

    /**
     * Registers a {@link PluginCommand} to this implementation
     *
     * @param pluginCommand {@link PluginCommand} to register
     */
    public void register(@NotNull PluginCommand pluginCommand) {
        pluginCommand.setExecutor(this);
        pluginCommand.setTabCompleter(this);
        pluginCommand.setPermission(command.permission);
        pluginCommand.setDescription(command.getDescription());
        if (CommodoreProvider.isSupported()) {
            BrigadierUtil.registerCommodore(plugin, pluginCommand, command);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            this.command.onExecute(BukkitPlayer.adapt(player), args);
        } else {
            if (this.command instanceof ConsoleExecutable consoleExecutable) {
                consoleExecutable.onConsoleExecute(args);
            } else {
                plugin.getLocales().getLocale("error_in_game_command_only")
                        .ifPresent(locale -> plugin.getAudiences().sender(sender)
                                .sendMessage(locale.toComponent()));
            }
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (this.command instanceof TabCompletable tabCompletable) {
            return tabCompletable.onTabComplete(args);
        }
        return Collections.emptyList();
    }

}
