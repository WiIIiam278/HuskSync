package net.william278.husksync.command;

import de.themoep.minedown.MineDown;
import net.william278.husksync.HuskSync;
import net.william278.husksync.config.Locales;
import net.william278.husksync.migrator.Migrator;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.util.UpdateChecker;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class HuskSyncCommand extends CommandBase implements TabCompletable, ConsoleExecutable {

    private final String[] COMMAND_ARGUMENTS = {"update", "about", "reload", "migrate"};

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
                final UpdateChecker updateChecker = new UpdateChecker(plugin.getPluginVersion(), plugin.getLoggingAdapter());
                updateChecker.fetchLatestVersion().thenAccept(latestVersion -> {
                    if (updateChecker.isUpdateAvailable(latestVersion)) {
                        player.sendMessage(new MineDown("[HuskSync](#00fb9a bold) [| A new update is available:](#00fb9a) [HuskSync " + latestVersion + "](#00fb9a bold)" +
                                                        "[•](white) [Currently running:](#00fb9a) [Version " + updateChecker.getCurrentVersion() + "](gray)" +
                                                        "[•](white) [Download links:](#00fb9a) [[⏩ Spigot]](gray open_url=https://www.spigotmc.org/resources/husksync.97144/updates) [•](#262626) [[⏩ Polymart]](gray open_url=https://polymart.org/resource/husksync.1634/updates) [•](#262626) [[⏩ Songoda]](gray open_url=https://songoda.com/marketplace/product/husksync-a-modern-cross-server-player-data-synchronization-system.758)"));
                    } else {
                        player.sendMessage(new MineDown("[HuskSync](#00fb9a bold) [| HuskSync is up-to-date, running version " + updateChecker.getCurrentVersion() + "](#00fb9a)"));
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
                plugin.getLocales().getLocale("reload_complete").ifPresent(player::sendMessage);
            }
            case "migrate" ->
                    plugin.getLocales().getLocale("error_console_command_only").ifPresent(player::sendMessage);
            default -> plugin.getLocales().getLocale("error_invalid_syntax",
                            "/husksync <update/about/reload>")
                    .ifPresent(player::sendMessage);
        }
    }

    @Override
    public void onConsoleExecute(@NotNull String[] args) {
        if (args.length < 1) {
            plugin.getLoggingAdapter().log(Level.INFO, "Console usage: \"husksync <update/about/reload/migrate>\"");
            return;
        }
        switch (args[0].toLowerCase()) {
            case "update", "version" ->
                    new UpdateChecker(plugin.getPluginVersion(), plugin.getLoggingAdapter()).logToConsole();
            case "info", "about" -> plugin.getLoggingAdapter().log(Level.INFO, new MineDown(plugin.getLocales().stripMineDown(
                    Locales.PLUGIN_INFORMATION.replace("%version%", plugin.getPluginVersion().toString()))));
            case "reload" -> {
                plugin.reload();
                plugin.getLoggingAdapter().log(Level.INFO, "Reloaded config & message files.");
            }
            case "migrate" -> {
                if (args.length < 2) {
                    plugin.getLoggingAdapter().log(Level.INFO,
                            "Please choose a migrator, then run \"husksync migrate <migrator>\"");
                    logMigratorsList();
                    return;
                }
                final Optional<Migrator> selectedMigrator = plugin.getAvailableMigrators().stream().filter(availableMigrator ->
                        availableMigrator.getIdentifier().equalsIgnoreCase(args[1])).findFirst();
                selectedMigrator.ifPresentOrElse(migrator -> {
                    if (args.length < 3) {
                        plugin.getLoggingAdapter().log(Level.INFO, migrator.getHelpMenu());
                        return;
                    }
                    switch (args[2]) {
                        case "start" -> migrator.start();
                        case "set" -> migrator.handleConfigurationCommand(Arrays.copyOfRange(args, 3, args.length));
                        default -> plugin.getLoggingAdapter().log(Level.INFO,
                                "Invalid syntax. Console usage: \"husksync migrate " + args[1] + " <start/set>");
                    }
                }, () -> {
                    plugin.getLoggingAdapter().log(Level.INFO,
                            "Please specify a valid migrator.\n" +
                            "If a migrator is not available, please verify that you meet the prerequisites to use it.");
                    logMigratorsList();
                });
            }
            default -> plugin.getLoggingAdapter().log(Level.INFO,
                    "Invalid syntax. Console usage: \"husksync <update/about/reload/migrate>\"");
        }
    }

    private void logMigratorsList() {
        plugin.getLoggingAdapter().log(Level.INFO,
                "List of available migrators:\nMigrator ID / Migrator Name:\n" +
                plugin.getAvailableMigrators().stream()
                        .map(migrator -> migrator.getIdentifier() + " - " + migrator.getName())
                        .collect(Collectors.joining("\n")));
    }

    @Override
    public List<String> onTabComplete(@NotNull String[] args) {
        return Arrays.stream(COMMAND_ARGUMENTS)
                .filter(argument -> argument.startsWith(args.length >= 1 ? args[0] : ""))
                .sorted().collect(Collectors.toList());
    }

    private void displayPluginInformation(@NotNull OnlineUser player) {
        if (!player.hasPermission(Permission.COMMAND_HUSKSYNC_INFO.node)) {
            plugin.getLocales().getLocale("error_no_permission").ifPresent(player::sendMessage);
            return;
        }
        player.sendMessage(new MineDown(Locales.PLUGIN_INFORMATION.replace("%version%", plugin.getPluginVersion().toString())));
    }
}
