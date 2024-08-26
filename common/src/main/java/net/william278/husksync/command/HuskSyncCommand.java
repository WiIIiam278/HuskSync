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

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.william278.desertwell.about.AboutMenu;
import net.william278.desertwell.util.UpdateChecker;
import net.william278.husksync.HuskSync;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.database.Database;
import net.william278.husksync.migrator.Migrator;
import net.william278.husksync.user.CommandUser;
import net.william278.husksync.util.LegacyConverter;
import net.william278.uniform.BaseCommand;
import net.william278.uniform.CommandProvider;
import net.william278.uniform.Permission;
import net.william278.uniform.element.ArgumentElement;
import org.apache.commons.text.WordUtils;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class HuskSyncCommand extends PluginCommand {

    private final UpdateChecker updateChecker;
    private final AboutMenu aboutMenu;

    public HuskSyncCommand(@NotNull HuskSync plugin) {
        super("husksync", List.of(), Permission.Default.TRUE, ExecutionScope.ALL, plugin);
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
                        AboutMenu.Credit.of("Preva1l").description("Code"),
                        AboutMenu.Credit.of("hanbings").description("Code (Fabric porting)"),
                        AboutMenu.Credit.of("Stampede2011").description("Code (Fabric mixins)"))
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
                        AboutMenu.Credit.of("WinTone01").description("Turkish (tr-tr)"),
                        AboutMenu.Credit.of("IbanEtchep").description("French (fr-fr)"))
                .buttons(
                        AboutMenu.Link.of("https://william278.net/docs/husksync").text("Documentation").icon("⛏"),
                        AboutMenu.Link.of("https://github.com/WiIIiam278/HuskSync/issues").text("Issues").icon("❌").color(TextColor.color(0xff9f0f)),
                        AboutMenu.Link.of("https://discord.gg/tVYhJfyDWG").text("Discord").icon("⭐").color(TextColor.color(0x6773f5)))
                .build();
    }

    @Override
    public void provide(@NotNull BaseCommand<?> command) {
        command.setDefaultExecutor((ctx) -> about(command, ctx));
        command.addSubCommand("about", (sub) -> sub.setDefaultExecutor((ctx) -> about(command, ctx)));
        command.addSubCommand("status", needsOp("status"), status());
        command.addSubCommand("reload", needsOp("reload"), reload());
        command.addSubCommand("update", needsOp("update"), update());
        command.addSubCommand("forceupgrade", forceUpgrade());
        command.addSubCommand("migrate", migrate());
    }

    private void about(@NotNull BaseCommand<?> c, @NotNull CommandContext<?> ctx) {
        user(c, ctx).getAudience().sendMessage(aboutMenu.toComponent());
    }

    @NotNull
    private CommandProvider status() {
        return (sub) -> sub.setDefaultExecutor((ctx) -> {
            final CommandUser user = user(sub, ctx);
            plugin.getLocales().getLocale("system_status_header").ifPresent(user::sendMessage);
            user.sendMessage(Component.join(
                    JoinConfiguration.newlines(),
                    Arrays.stream(StatusLine.values()).map(s -> s.get(plugin)).toList()
            ));
        });
    }

    @NotNull
    private CommandProvider reload() {
        return (sub) -> sub.setDefaultExecutor((ctx) -> {
            final CommandUser user = user(sub, ctx);
            try {
                plugin.loadSettings();
                plugin.loadLocales();
                plugin.loadServer();
                plugin.getLocales().getLocale("reload_complete").ifPresent(user::sendMessage);
            } catch (Throwable e) {
                user.sendMessage(new MineDown(
                        "[Error:](#ff3300) [Failed to reload the plugin. Check console for errors.](#ff7e5e)"
                ));
                plugin.log(Level.SEVERE, "Failed to reload the plugin", e);
            }
        });
    }

    @NotNull
    private CommandProvider update() {
        return (sub) -> sub.setDefaultExecutor((ctx) -> updateChecker.check().thenAccept(checked -> {
            final CommandUser user = user(sub, ctx);
            if (checked.isUpToDate()) {
                plugin.getLocales().getLocale("up_to_date", plugin.getPluginVersion().toString())
                        .ifPresent(user::sendMessage);
                return;
            }
            plugin.getLocales().getLocale("update_available", checked.getLatestVersion().toString(),
                    plugin.getPluginVersion().toString()).ifPresent(user::sendMessage);
        }));
    }

    @NotNull
    private CommandProvider migrate() {
        return (sub) -> {
            sub.setCondition((ctx) -> sub.getUser(ctx).isConsole());
            sub.setDefaultExecutor((ctx) -> {
                plugin.log(Level.INFO, "Please choose a migrator, then run \"husksync migrate start <migrator>\"");
                plugin.log(Level.INFO, String.format(
                        "List of available migrators:\nMigrator ID / Migrator Name:\n%s",
                        plugin.getAvailableMigrators().stream()
                                .map(migrator -> String.format("%s - %s", migrator.getIdentifier(), migrator.getName()))
                                .collect(Collectors.joining("\n"))
                ));
            });
            sub.addSubCommand("help", (help) -> help.addSyntax((cmd) -> {
                final Migrator migrator = cmd.getArgument("migrator", Migrator.class);
                plugin.log(Level.INFO, migrator.getHelpMenu());
            }, migrator()));
            sub.addSubCommand("start", (start) -> start.addSyntax((cmd) -> {
                final Migrator migrator = cmd.getArgument("migrator", Migrator.class);
                migrator.start().thenAccept(succeeded -> {
                    if (succeeded) {
                        plugin.log(Level.INFO, "Migration completed successfully!");
                    } else {
                        plugin.log(Level.WARNING, "Migration failed!");
                    }
                });
            }, migrator()));
            sub.addSubCommand("set", (set) -> set.addSyntax((cmd) -> {
                final Migrator migrator = cmd.getArgument("migrator", Migrator.class);
                final String[] args = cmd.getArgument("args", String.class).split(" ");
                migrator.handleConfigurationCommand(args);
            }, migrator(), BaseCommand.greedyString("args")));
        };
    }

    @NotNull
    private CommandProvider forceUpgrade() {
        return (sub) -> {
            sub.setCondition((ctx) -> sub.getUser(ctx).isConsole());
            sub.setDefaultExecutor((ctx) -> {
                final LegacyConverter converter = plugin.getLegacyConverter().orElse(null);
                if (converter == null) {
                    return;
                }

                plugin.runAsync(() -> {
                    final Database database = plugin.getDatabase();
                    plugin.log(Level.INFO, "Beginning forced legacy data upgrade for all users...");
                    database.getAllUsers().forEach(user -> database.getLatestSnapshot(user).ifPresent(snapshot -> {
                        final DataSnapshot.Packed upgraded = converter.convert(
                                snapshot.asBytes(plugin),
                                UUID.randomUUID(),
                                OffsetDateTime.now()
                        );
                        upgraded.setSaveCause(DataSnapshot.SaveCause.CONVERTED_FROM_V2);
                        plugin.getDatabase().addSnapshot(user, upgraded);
                        plugin.getRedisManager().clearUserData(user);
                    }));
                    plugin.log(Level.INFO, "Legacy data upgrade complete!");
                });
            });
        };
    }

    @NotNull
    private <S> ArgumentElement<S, Migrator> migrator() {
        return new ArgumentElement<>("migrator", reader -> {
            final String id = reader.readString();
            final Migrator migrator = plugin.getAvailableMigrators().stream()
                    .filter(m -> m.getIdentifier().equalsIgnoreCase(id)).findFirst().orElse(null);
            if (migrator == null) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader);
            }
            return migrator;
        }, (context, builder) -> {
            for (Migrator material : plugin.getAvailableMigrators()) {
                builder.suggest(material.getIdentifier());
            }
            return builder.buildFuture();
        });
    }

    private enum StatusLine {
        PLUGIN_VERSION(plugin -> Component.text("v" + plugin.getPluginVersion().toStringWithoutMetadata())
                .appendSpace().append(plugin.getPluginVersion().getMetadata().isBlank() ? Component.empty()
                        : Component.text("(build " + plugin.getPluginVersion().getMetadata() + ")"))),
        SERVER_VERSION(plugin -> Component.text(plugin.getServerVersion())),
        LANGUAGE(plugin -> Component.text(plugin.getSettings().getLanguage())),
        MINECRAFT_VERSION(plugin -> Component.text(plugin.getMinecraftVersion().toString())),
        JAVA_VERSION(plugin -> Component.text(System.getProperty("java.version"))),
        JAVA_VENDOR(plugin -> Component.text(System.getProperty("java.vendor"))),
        SERVER_NAME(plugin -> Component.text(plugin.getServerName())),
        CLUSTER_ID(plugin -> Component.text(plugin.getSettings().getClusterId().isBlank() ? "None" : plugin.getSettings().getClusterId())),
        SYNC_MODE(plugin -> Component.text(WordUtils.capitalizeFully(
                plugin.getSettings().getSynchronization().getMode().toString()
        ))),
        DELAY_LATENCY(plugin -> Component.text(
                plugin.getSettings().getSynchronization().getNetworkLatencyMilliseconds() + "ms"
        )),
        DATABASE_TYPE(plugin ->
                Component.text(plugin.getSettings().getDatabase().getType().getDisplayName() +
                               (plugin.getSettings().getDatabase().getType() == Database.Type.MONGO ?
                                       (plugin.getSettings().getDatabase().getMongoSettings().isUsingAtlas() ? " Atlas" : "") : ""))
        ),
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
                plugin.getRegisteredDataTypes().stream().map(i -> Component.textOfChildren(Component.text(i.toString())
                                .appendSpace().append(Component.text(i.isEnabled() ? '✔' : '❌')))
                        .color(i.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED)
                        .hoverEvent(HoverEvent.showText(
                                Component.text(i.isEnabled() ? "Enabled" : "Disabled")
                                        .append(Component.newline())
                                        .append(Component.text("Dependencies: %s".formatted(i.getDependencies()
                                                .isEmpty() ? "(None)" : i.getDependencies().stream()
                                                .map(d -> "%s (%s)".formatted(
                                                        d.getKey().value(), d.isRequired() ? "Required" : "Optional"
                                                )).collect(Collectors.joining(", ")))
                                        ).color(NamedTextColor.GRAY))
                        ))).toList()
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
