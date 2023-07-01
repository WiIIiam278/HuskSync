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
