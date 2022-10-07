package net.william278.husksync.command;

import de.themoep.minedown.adventure.MineDown;
import net.william278.desertwell.AboutMenu;
import net.william278.husksync.HuskSync;
import net.william278.husksync.migrator.Migrator;
import net.william278.husksync.player.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class HuskSyncCommand extends CommandBase implements TabCompletable, ConsoleExecutable {

    private final String[] SUB_COMMANDS = {"update", "about", "reload", "migrate"};
    private final AboutMenu aboutMenu;

    public HuskSyncCommand(@NotNull HuskSync implementor) {
        super("husksync", Permission.COMMAND_HUSKSYNC, implementor);
        this.aboutMenu = AboutMenu.create("HuskSync")
                .withDescription("A modern, cross-server player data synchronization system")
                .withVersion(implementor.getPluginVersion())
                .addAttribution("Author",
                        AboutMenu.Credit.of("William278").withDescription("Click to visit website").withUrl("https://william278.net"))
                .addAttribution("Contributors",
                        AboutMenu.Credit.of("HarvelsX").withDescription("Code"),
                        AboutMenu.Credit.of("HookWoods").withDescription("Code"))
                .addAttribution("Translators",
                        AboutMenu.Credit.of("Namiu").withDescription("Japanese (ja-jp)"),
                        AboutMenu.Credit.of("anchelthe").withDescription("Spanish (es-es)"),
                        AboutMenu.Credit.of("Melonzio").withDescription("Spanish (es-es)"),
                        AboutMenu.Credit.of("Ceddix").withDescription("German (de-de)"),
                        AboutMenu.Credit.of("Pukejoy_1").withDescription("Bulgarian (bg-bg)"),
                        AboutMenu.Credit.of("mateusneresrb").withDescription("Brazilian Portuguese (pt-br)"),
                        AboutMenu.Credit.of("小蔡").withDescription("Traditional Chinese (zh-tw)"),
                        AboutMenu.Credit.of("Ghost-chu").withDescription("Simplified Chinese (zh-cn)"),
                        AboutMenu.Credit.of("DJelly4K").withDescription("Simplified Chinese (zh-cn)"),
                        AboutMenu.Credit.of("Thourgard").withDescription("Ukrainian (uk-ua)"),
                        AboutMenu.Credit.of("xF3d3").withDescription("Italian (it-it)"))
                .addButtons(
                        AboutMenu.Link.of("https://william278.net/docs/husksync").withText("Documentation").withIcon("⛏"),
                        AboutMenu.Link.of("https://github.com/WiIIiam278/HuskSync/issues").withText("Issues").withIcon("❌").withColor("#ff9f0f"),
                        AboutMenu.Link.of("https://discord.gg/tVYhJfyDWG").withText("Discord").withIcon("⭐").withColor("#6773f5"));
    }

    @Override
    public void onExecute(@NotNull OnlineUser player, @NotNull String[] args) {
        if (args.length < 1) {
            sendAboutMenu(player);
            return;
        }
        switch (args[0].toLowerCase()) {
            case "update", "version" -> {
                if (!player.hasPermission(Permission.COMMAND_HUSKSYNC_UPDATE.node)) {
                    plugin.getLocales().getLocale("error_no_permission").ifPresent(player::sendMessage);
                    return;
                }
                plugin.getLatestVersionIfOutdated().thenAccept(newestVersion ->
                        newestVersion.ifPresentOrElse(
                                newVersion -> player.sendMessage(
                                        new MineDown("[HuskSync](#00fb9a bold) [| A new version of HuskSync is available!"
                                                + " (v" + newVersion + " (Running: v" + plugin.getPluginVersion() + ")](#00fb9a)")),
                                () -> player.sendMessage(
                                        new MineDown("[HuskSync](#00fb9a bold) [| HuskSync is up-to-date."
                                                + " (Running: v" + plugin.getPluginVersion() + ")](#00fb9a)"))));
            }
            case "about", "info" -> sendAboutMenu(player);
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
            case "update", "version" -> plugin.getLatestVersionIfOutdated().thenAccept(newestVersion ->
                    newestVersion.ifPresentOrElse(newVersion -> plugin.getLoggingAdapter().log(Level.WARNING,
                                    "An update is available for HuskSync, v" + newVersion
                                            + " (Running v" + plugin.getPluginVersion() + ")"),
                            () -> plugin.getLoggingAdapter().log(Level.INFO,
                                    "HuskSync is up to date" +
                                            " (Running v" + plugin.getPluginVersion() + ")")));
            case "about", "info" -> aboutMenu.toString().lines().forEach(plugin.getLoggingAdapter()::info);
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
                        case "start" -> migrator.start().thenAccept(succeeded -> {
                            if (succeeded) {
                                plugin.getLoggingAdapter().log(Level.INFO, "Migration completed successfully!");
                            } else {
                                plugin.getLoggingAdapter().log(Level.WARNING, "Migration failed!");
                            }
                        });
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
        if (args.length <= 1) {
            return Arrays.stream(SUB_COMMANDS)
                    .filter(argument -> argument.startsWith(args.length == 1 ? args[0] : ""))
                    .sorted().collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private void sendAboutMenu(@NotNull OnlineUser player) {
        if (!player.hasPermission(Permission.COMMAND_HUSKSYNC_ABOUT.node)) {
            plugin.getLocales().getLocale("error_no_permission").ifPresent(player::sendMessage);
            return;
        }
        player.sendMessage(aboutMenu.toMineDown());
    }
}
