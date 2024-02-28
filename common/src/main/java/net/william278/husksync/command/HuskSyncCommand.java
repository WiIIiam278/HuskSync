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
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.william278.desertwell.about.AboutMenu;
import net.william278.desertwell.util.UpdateChecker;
import net.william278.husksync.HuskSync;
import net.william278.husksync.migrator.Migrator;
import net.william278.husksync.user.CommandUser;
import net.william278.husksync.user.OnlineUser;
import org.apache.commons.text.WordUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class HuskSyncCommand extends Command implements TabProvider {

    private static final Map<String, Boolean> SUB_COMMANDS = Map.of(
            "about", false,
            "status", true,
            "reload", true,
            "migrate", true,
            "update", true
    );

    private final UpdateChecker updateChecker;
    private final AboutMenu aboutMenu;

    public HuskSyncCommand(@NotNull HuskSync plugin) {
        super("husksync", List.of(), "[" + String.join("|", SUB_COMMANDS.keySet()) + "]", plugin);
        addAdditionalPermissions(SUB_COMMANDS);

        this.updateChecker = plugin.getUpdateChecker();
        this.aboutMenu = AboutMenu.builder()
                .title(Component.text("HuskSync"))
                .description(Component.text("A modern, cross-server player data synchronization system"))
                .version(plugin.getPluginVersion())
                .credits("Author",
                        AboutMenu.Credit.of("William278").description("Click to visit website").url("https://william278.net"))
                .credits("Contributors",
                        AboutMenu.Credit.of("HarvelsX").description("Code"),
                        AboutMenu.Credit.of("HookWoods").description("Code"),
                        AboutMenu.Credit.of("Preva1l").description("Code"))
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
                        AboutMenu.Credit.of("xF3d3").description("Italian (it-it)"),
                        AboutMenu.Credit.of("cada3141").description("Korean (ko-kr)"),
                        AboutMenu.Credit.of("Wirayuda5620").description("Indonesian (id-id)"),
                        AboutMenu.Credit.of("WinTone01").description("Turkish (tr-tr)"))
                .buttons(
                        AboutMenu.Link.of("https://william278.net/docs/husksync").text("Documentation").icon("⛏"),
                        AboutMenu.Link.of("https://github.com/WiIIiam278/HuskSync/issues").text("Issues").icon("❌").color(TextColor.color(0xff9f0f)),
                        AboutMenu.Link.of("https://discord.gg/tVYhJfyDWG").text("Discord").icon("⭐").color(TextColor.color(0x6773f5)))
                .build();
    }

    @Override
    public void execute(@NotNull CommandUser executor, @NotNull String[] args) {
        final String subCommand = parseStringArg(args, 0).orElse("about").toLowerCase(Locale.ENGLISH);
        if (SUB_COMMANDS.containsKey(subCommand) && !executor.hasPermission(getPermission(subCommand))) {
            plugin.getLocales().getLocale("error_no_permission")
                    .ifPresent(executor::sendMessage);
            return;
        }

        switch (subCommand) {
            case "about" -> executor.sendMessage(aboutMenu.toComponent());
            case "status" -> {
                getPlugin().getLocales().getLocale("system_status_header").ifPresent(executor::sendMessage);
                executor.sendMessage(Component.join(
                        JoinConfiguration.newlines(),
                        Arrays.stream(StatusLine.values()).map(s -> s.get(plugin)).toList()
                ));
            }
            case "reload" -> {
                try {
                    plugin.loadSettings();
                    plugin.loadLocales();
                    plugin.loadServer();
                    plugin.getLocales().getLocale("reload_complete").ifPresent(executor::sendMessage);
                } catch (Throwable e) {
                    executor.sendMessage(new MineDown(
                            "[Error:](#ff3300) [Failed to reload the plugin. Check console for errors.](#ff7e5e)"
                    ));
                    plugin.log(Level.SEVERE, "Failed to reload the plugin", e);
                }
            }
            case "migrate" -> {
                if (executor instanceof OnlineUser) {
                    plugin.getLocales().getLocale("error_console_command_only")
                            .ifPresent(executor::sendMessage);
                    return;
                }
                this.handleMigrationCommand(args);
            }
            case "update" -> updateChecker.check().thenAccept(checked -> {
                if (checked.isUpToDate()) {
                    plugin.getLocales().getLocale("up_to_date", plugin.getPluginVersion().toString())
                            .ifPresent(executor::sendMessage);
                    return;
                }
                plugin.getLocales().getLocale("update_available", checked.getLatestVersion().toString(),
                        plugin.getPluginVersion().toString()).ifPresent(executor::sendMessage);
            });
            default -> plugin.getLocales().getLocale("error_invalid_syntax", getUsage())
                    .ifPresent(executor::sendMessage);
        }
    }

    // Handle a migration console command input
    private void handleMigrationCommand(@NotNull String[] args) {
        if (args.length < 2) {
            plugin.log(Level.INFO,
                    "Please choose a migrator, then run \"husksync migrate <migrator>\"");
            this.logMigratorList();
            return;
        }

        final Optional<Migrator> selectedMigrator = plugin.getAvailableMigrators().stream()
                .filter(available -> available.getIdentifier().equalsIgnoreCase(args[1]))
                .findFirst();
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
                default -> plugin.log(Level.INFO, String.format(
                        "Invalid syntax. Console usage: \"husksync migrate %s <start/set>", args[1]
                ));
            }
        }, () -> {
            plugin.log(Level.INFO,
                    "Please specify a valid migrator.\n" +
                            "If a migrator is not available, please verify that you meet the prerequisites to use it.");
            this.logMigratorList();
        });
    }

    // Log the list of available migrators
    private void logMigratorList() {
        plugin.log(Level.INFO, String.format(
                "List of available migrators:\nMigrator ID / Migrator Name:\n%s",
                plugin.getAvailableMigrators().stream()
                        .map(migrator -> String.format("%s - %s", migrator.getIdentifier(), migrator.getName()))
                        .collect(Collectors.joining("\n"))
        ));
    }

    @Nullable
    @Override
    public List<String> suggest(@NotNull CommandUser user, @NotNull String[] args) {
        return switch (args.length) {
            case 0, 1 -> SUB_COMMANDS.keySet().stream().sorted().toList();
            default -> null;
        };
    }

    private enum StatusLine {
        PLUGIN_VERSION(plugin -> Component.text("v" + plugin.getPluginVersion().toStringWithoutMetadata())
                .appendSpace().append(plugin.getPluginVersion().getMetadata().isBlank() ? Component.empty()
                        : Component.text("(build " + plugin.getPluginVersion().getMetadata() + ")"))),
        PLATFORM_TYPE(plugin -> Component.text(WordUtils.capitalizeFully(plugin.getPlatformType()))),
        LANGUAGE(plugin -> Component.text(plugin.getSettings().getLanguage())),
        MINECRAFT_VERSION(plugin -> Component.text(plugin.getMinecraftVersion().toString())),
        JAVA_VERSION(plugin -> Component.text(System.getProperty("java.version"))),
        JAVA_VENDOR(plugin -> Component.text(System.getProperty("java.vendor"))),
        SYNC_MODE(plugin -> Component.text(WordUtils.capitalizeFully(
                plugin.getSettings().getSynchronization().getMode().toString()
        ))),
        DELAY_LATENCY(plugin -> Component.text(
                plugin.getSettings().getSynchronization().getNetworkLatencyMilliseconds() + "ms"
        )),
        SERVER_NAME(plugin -> Component.text(plugin.getServerName())),
        DATABASE_TYPE(plugin -> Component.text(plugin.getSettings().getDatabase().getType().getDisplayName())),
        IS_DATABASE_LOCAL(plugin -> getLocalhostBoolean(plugin.getSettings().getDatabase().getCredentials().getHost())),
        USING_REDIS_SENTINEL(plugin -> getBoolean(
                !plugin.getSettings().getRedis().getSentinel().getMaster().isBlank()
        )),
        USING_REDIS_PASSWORD(plugin -> getBoolean(
                !plugin.getSettings().getRedis().getCredentials().getPassword().isBlank()
        )),
        REDIS_USING_SSL(plugin -> getBoolean(
                plugin.getSettings().getRedis().getCredentials().isUseSsl()
        )),
        IS_REDIS_LOCAL(plugin -> getLocalhostBoolean(
                plugin.getSettings().getRedis().getCredentials().getHost()
        )),
        DATA_TYPES(plugin -> Component.join(
                JoinConfiguration.commas(true),
                plugin.getRegisteredDataTypes().stream().map(i -> {
                    boolean enabled = plugin.getSettings().getSynchronization().isFeatureEnabled(i);
                    return Component.textOfChildren(Component
                                    .text(i.toString()).appendSpace().append(Component.text(enabled ? '✔' : '❌')))
                            .color(enabled ? NamedTextColor.GREEN : NamedTextColor.RED)
                            .hoverEvent(HoverEvent.showText(Component.text(enabled ? "Enabled" : "Disabled")));
                }).toList()
        ));

        private final Function<HuskSync, Component> supplier;

        StatusLine(@NotNull Function<HuskSync, Component> supplier) {
            this.supplier = supplier;
        }

        @NotNull
        private Component get(@NotNull HuskSync plugin) {
            return Component
                    .text("•").appendSpace()
                    .append(Component.text(
                            WordUtils.capitalizeFully(name().replaceAll("_", " ")),
                            TextColor.color(0x848484)
                    ))
                    .append(Component.text(':')).append(Component.space().color(NamedTextColor.WHITE))
                    .append(supplier.apply(plugin));
        }

        @NotNull
        private static Component getBoolean(boolean value) {
            return Component.text(value ? "Yes" : "No", value ? NamedTextColor.GREEN : NamedTextColor.RED);
        }

        @NotNull
        private static Component getLocalhostBoolean(@NotNull String value) {
            return getBoolean(value.equals("127.0.0.1") || value.equals("0.0.0.0")
                    || value.equals("localhost") || value.equals("::1"));
        }
    }

}
