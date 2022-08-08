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
        try (InputStream pluginFile = plugin.getResourceReader()
                .getResource("commodore/" + command.command + ".commodore")) {
            CommodoreProvider.getCommodore(plugin).register(pluginCommand,
                    CommodoreFileReader.INSTANCE.parse(pluginFile),
                    player -> player.hasPermission(command.permission));
        } catch (IOException e) {
            plugin.getLoggingAdapter().log(Level.SEVERE,
                    "Failed to load " + command.command + ".commodore command definitions", e);
        }
    }

}
