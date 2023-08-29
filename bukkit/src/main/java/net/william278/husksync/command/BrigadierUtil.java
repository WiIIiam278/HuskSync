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
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

public class BrigadierUtil {

    /**
     * Uses commodore to register command completions.
     *
     * @param plugin        instance of the registering Bukkit plugin
     * @param bukkitCommand the Bukkit PluginCommand to register completions for
     * @param command       the {@link Command} to register completions for
     */
    protected static void registerCommodore(@NotNull BukkitHuskSync plugin,
                                            @NotNull org.bukkit.command.Command bukkitCommand,
                                            @NotNull Command command) {
        final InputStream commodoreFile = plugin.getResource(
                "commodore/" + bukkitCommand.getName() + ".commodore"
        );
        if (commodoreFile == null) {
            return;
        }
        try {
            CommodoreProvider.getCommodore(plugin).register(bukkitCommand,
                    CommodoreFileReader.INSTANCE.parse(commodoreFile),
                    player -> player.hasPermission(command.getPermission()));
        } catch (IOException e) {
            plugin.log(Level.SEVERE, String.format(
                    "Failed to read command commodore completions for %s", bukkitCommand.getName()), e
            );
        }
    }

}
