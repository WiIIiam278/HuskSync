/*
 * This file is part of HuskSync by William278. Do not redistribute!
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  All rights reserved.
 *
 *  This source code is provided as reference to licensed individuals that have purchased the HuskSync
 *  plugin once from any of the official sources it is provided. The availability of this code does
 *  not grant you the rights to modify, re-distribute, compile or redistribute this source code or
 *  "plugin" outside this intended purpose. This license does not cover libraries developed by third
 *  parties that are utilised in the plugin.
 */

package net.william278.husksync.command;

import me.lucko.commodore.CommodoreProvider;
import me.lucko.commodore.file.CommodoreFileReader;
import net.william278.husksync.BukkitHuskSync;
import org.bukkit.command.PluginCommand;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

/**
 * Used for registering Brigadier hooks on platforms that support commodore for rich command syntax
 */
public class BrigadierUtil {

    protected static void registerCommodore(@NotNull BukkitHuskSync plugin, @NotNull PluginCommand pluginCommand,
                                            @NotNull CommandBase command) {
        // Register command descriptions via commodore (brigadier wrapper)
        try (InputStream pluginFile = plugin.getResource("commodore/" + command.command + ".commodore")) {
            CommodoreProvider.getCommodore(plugin).register(pluginCommand,
                    CommodoreFileReader.INSTANCE.parse(pluginFile),
                    player -> player.hasPermission(command.permission));
        } catch (IOException e) {
            plugin.log(Level.SEVERE,
                    "Failed to load " + command.command + ".commodore command definitions", e);
        }
    }

}
