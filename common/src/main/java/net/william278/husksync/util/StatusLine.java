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

package net.william278.husksync.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.william278.husksync.HuskSync;
import net.william278.husksync.database.Database;
import org.apache.commons.text.WordUtils;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.stream.Collectors;

public enum StatusLine {
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
    REDIS_VERSION(plugin -> Component.text(plugin.getRedisManager().getVersion())),
    USING_REDIS_SENTINEL(plugin -> getBoolean(
            !plugin.getSettings().getRedis().getSentinel().getMaster().isBlank()
    )),
    REDIS_DATABASE(plugin -> Component.text(plugin.getSettings().getRedis().getCredentials().getDatabase())),
    USING_REDIS_USER(plugin -> getBoolean(
            !plugin.getSettings().getRedis().getCredentials().getUser().isBlank()
    )),
    USING_REDIS_PASSWORD(plugin -> getBoolean(
            !plugin.getSettings().getRedis().getCredentials().getPassword().isBlank()
    )),
    REDIS_USING_SSL(plugin -> getBoolean(
            plugin.getSettings().getRedis().getCredentials().isUseSsl()
    )),
    REDIS_LATENCY(plugin -> Component.text("%sms".formatted(plugin.getRedisManager().getLatency()))),
    IS_REDIS_LOCAL(plugin -> getLocalhostBoolean(
            plugin.getSettings().getRedis().getCredentials().getHost()
    )),
    LOCKED_USER_HANDLER(plugin -> Component.text(plugin.getLockedHandler().getClass().getSimpleName())),
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
    public Component get(@NotNull HuskSync plugin) {
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
    public String getValue(@NotNull HuskSync plugin) {
        return PlainTextComponentSerializer.plainText().serialize(supplier.apply(plugin));
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
