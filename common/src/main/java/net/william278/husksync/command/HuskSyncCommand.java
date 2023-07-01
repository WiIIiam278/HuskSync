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

import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.william278.desertwell.about.AboutMenu;
import net.william278.husksync.HuskSync;
import net.william278.husksync.migrator.Migrator;
import net.william278.husksync.player.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class HuskSyncCommand extends CommandBase implements TabCompletable, ConsoleExecutable {

    private final String[] SUB_COMMANDS = {"update", "about", "reload", "migrate"};
    private final AboutMenu aboutMenu;

    public HuskSyncCommand(@NotNull HuskSync implementor) {
        super("husksync", Permission.COMMAND_HUSKSYNC, implementor);
        this.aboutMenu = AboutMenu.builder()
                .title(Component.text("HuskSync"))
                .description(Component.text("A modern, cross-server player data synchronization system"))
                .version(implementor.getPluginVersion())
                .credits("Author",
                        AboutMenu.Credit.of("William278").description("Click to visit website").url("https://william278.net"))
                .credits("Contributors",
                        AboutMenu.Credit.of("HarvelsX").description("Code"),
                        AboutMenu.Credit.of("HookWoods").description("Code"))
                .credits("Translators",
                        AboutMenu.Credit.of("Namiu").description("Japanese (ja-jp)"),
                        AboutMenu.Credit.of("anchelthe").description("Spanish (es-es)"),
                        AboutMenu.Credit.of("Melonzio").description("Spanish (es-es)"),
                        AboutMenu.Credit.of("Ceddix").description("German (de-de)"),
                        AboutMenu.Credit.of("Pukejoy_1").description("Bulgarian (bg-bg)"),
                        AboutMenu.Credit.of("mateusneresrb").description("Brazilian Portuguese (pt-br)"),
                        AboutMenu.Credit.of("小蔡").description("Traditional Chinese (zh-tw)"),
                        AboutMenu.Credit.of("Ghost-chu").description("Simplified Chinese (zh-cn)"),
                        AboutMenu.Credit.of("DJelly4K").description("Simplified Chinese (zh-cn)"),
                        AboutMenu.Credit.of("Thourgard").description("Ukrainian (uk-ua)"),
                        AboutMenu.Credit.of("xF3d3").description("Italian (it-it)"))
                .buttons(
                        AboutMenu.Link.of("https://william278.net/docs/husksync").text("Documentation").icon("⛏"),
                        AboutMenu.Link.of("https://github.com/WiIIiam278/HuskSync/issues").text("Issues").icon("❌").color(TextColor.color(0xff9f0f)),
                        AboutMenu.Link.of("https://discord.gg/tVYhJfyDWG").text("Discord").icon("⭐").color(TextColor.color(0x6773f5)))
                .build();
    }

    @Override
    public void onExecute(@NotNull OnlineUser player, @NotNull String[] args) {
        if (args.length < 1) {
            sendAboutMenu(player);
            return;
        }
        switch (args[0].toLowerCase(Locale.ENGLISH)) {
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
            plugin.log(Level.INFO, "Console usage: \"husksync <update/about/reload/migrate>\"");
            return;
        }
        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "update", "version" -> plugin.getLatestVersionIfOutdated().thenAccept(newestVersion ->
                    newestVersion.ifPresentOrElse(newVersion -> plugin.log(Level.WARNING,
                                    "An update is available for HuskSync, v" + newVersion
                                    + " (Running v" + plugin.getPluginVersion() + ")"),
                            () -> plugin.log(Level.INFO,
                                    "HuskSync is up to date" +
                                    " (Running v" + plugin.getPluginVersion() + ")")));
            case "about", "info" -> aboutMenu.toString().lines().forEach(line -> plugin.log(Level.INFO, line));
            case "reload" -> {
                plugin.reload();
                plugin.log(Level.INFO, "Reloaded config & message files.");
            }
            case "migrate" -> {
                if (args.length < 2) {
                    plugin.log(Level.INFO,
                            "Please choose a migrator, then run \"husksync migrate <migrator>\"");
                    logMigratorsList();
                    return;
                }
                final Optional<Migrator> selectedMigrator = plugin.getAvailableMigrators().stream().filter(availableMigrator ->
                        availableMigrator.getIdentifier().equalsIgnoreCase(args[1])).findFirst();
                selectedMigrator.ifPresentOrElse(migrator -> {
                    if (args.length < 3) {
                        plugin.log(Level.INFO, migrator.getHelpMenu());
                        return;
                    }
                    switch (args[2]) {
                        case "start" -> migrator.start().thenAccept(succeeded -> {
                            if (succeeded) {
                                plugin.log(Level.INFO, "Migration completed successfully!");
                            } else {
                                plugin.log(Level.WARNING, "Migration failed!");
                            }
                        });
                        case "set" -> migrator.handleConfigurationCommand(Arrays.copyOfRange(args, 3, args.length));
                        default -> plugin.log(Level.INFO,
                                "Invalid syntax. Console usage: \"husksync migrate " + args[1] + " <start/set>");
                    }
                }, () -> {
                    plugin.log(Level.INFO,
                            "Please specify a valid migrator.\n" +
                            "If a migrator is not available, please verify that you meet the prerequisites to use it.");
                    logMigratorsList();
                });
            }
            default -> plugin.log(Level.INFO,
                    "Invalid syntax. Console usage: \"husksync <update/about/reload/migrate>\"");
        }
    }

    private void logMigratorsList() {
        plugin.log(Level.INFO,
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
        player.sendMessage(aboutMenu.toComponent());
    }
}
