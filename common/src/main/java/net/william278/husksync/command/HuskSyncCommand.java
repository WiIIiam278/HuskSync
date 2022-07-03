package net.william278.husksync.command;

import de.themoep.minedown.MineDown;
import net.william278.husksync.HuskSync;
import net.william278.husksync.config.Locales;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.util.UpdateChecker;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.logging.Level;

public class HuskSyncCommand extends CommandBase implements TabCompletable, ConsoleExecutable {

    public HuskSyncCommand(@NotNull HuskSync implementor) {
        super("husksync", Permission.COMMAND_HUSKSYNC, implementor);
    }

    @Override
    public void onExecute(@NotNull OnlineUser player, @NotNull String[] args) {
        if (args.length < 1) {
            displayPluginInformation(player);
            return;
        }
        switch (args[0].toLowerCase()) {
            case "update", "version" -> {
                if (!player.hasPermission(Permission.COMMAND_HUSKSYNC_UPDATE.node)) {
                    plugin.getLocales().getLocale("error_no_permission").ifPresent(player::sendMessage);
                    return;
                }
                final UpdateChecker updateChecker = new UpdateChecker(plugin.getVersion(), plugin.getLoggingAdapter());
                updateChecker.fetchLatestVersion().thenAccept(latestVersion -> {
                    if (updateChecker.isUpdateAvailable(latestVersion)) {
                        player.sendMessage(new MineDown("[HuskSync](#00fb9a bold) [| A new update is available:](#00fb9a) [HuskSync " + updateChecker.fetchLatestVersion() + "](#00fb9a bold)" +
                                "[•](white) [Currently running:](#00fb9a) [Version " + updateChecker.getCurrentVersion() + "](gray)" +
                                "[•](white) [Download links:](#00fb9a) [[⏩ Spigot]](gray open_url=https://www.spigotmc.org/resources/husksync.97144/updates) [•](#262626) [[⏩ Polymart]](gray open_url=https://polymart.org/resource/husksync.1634/updates) [•](#262626) [[⏩ Songoda]](gray open_url=https://songoda.com/marketplace/product/husksync-a-modern-cross-server-player-data-synchronization-system.758)"));
                    } else {
                        player.sendMessage(new MineDown("[HuskSync](#00fb9a bold) [| HuskSync is up-to-date, running version " + latestVersion));
                    }
                });
            }
            case "info", "about" -> displayPluginInformation(player);
            case "reload" -> {
                if (!player.hasPermission(Permission.COMMAND_HUSKSYNC_RELOAD.node)) {
                    plugin.getLocales().getLocale("error_no_permission").ifPresent(player::sendMessage);
                    return;
                }
                plugin.reload();
                player.sendMessage(new MineDown("[HuskSync](#00fb9a bold) &#00fb9a&| Reloaded config & message files."));
            }
            default ->
                    plugin.getLocales().getLocale("error_invalid_syntax", "/husksync <update|info|reload>").ifPresent(player::sendMessage);
        }
    }

    @Override
    public void onConsoleExecute(@NotNull String[] args) {
        if (args.length < 1) {
            plugin.getLoggingAdapter().log(Level.INFO, "Console usage: /husksync <update|info|reload|migrate>");
            return;
        }
        switch (args[0].toLowerCase()) {
            case "update", "version" -> new UpdateChecker(plugin.getVersion(), plugin.getLoggingAdapter()).logToConsole();
            case "info", "about" -> plugin.getLoggingAdapter().log(Level.INFO, plugin.getLocales().stripMineDown(
                    Locales.PLUGIN_INFORMATION.replace("%version%", plugin.getVersion())));
            case "reload" -> {
                plugin.reload();
                plugin.getLoggingAdapter().log(Level.INFO, "Reloaded config & message files.");
            }
            case "migrate" -> {
                //todo - MPDB migrator
            }
            default ->
                    plugin.getLoggingAdapter().log(Level.INFO, "Invalid syntax. Console usage: /husksync <update|info|reload|migrate>");
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull OnlineUser player, @NotNull String[] args) {
        return null;
    }

    private void displayPluginInformation(@NotNull OnlineUser player) {
        if (!player.hasPermission(Permission.COMMAND_HUSKSYNC_INFO.node)) {
            plugin.getLocales().getLocale("error_no_permission").ifPresent(player::sendMessage);
            return;
        }
        player.sendMessage(new MineDown(Locales.PLUGIN_INFORMATION.replace("%version%", plugin.getVersion())));
    }
}
