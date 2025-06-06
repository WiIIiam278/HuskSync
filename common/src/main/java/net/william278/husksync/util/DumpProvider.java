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

import net.william278.husksync.HuskSync;
import net.william278.husksync.user.CommandUser;
import net.william278.husksync.user.OnlineUser;
import net.william278.toilet.DumpOptions;
import net.william278.toilet.Toilet;
import net.william278.toilet.dump.*;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import static net.william278.toilet.DumpOptions.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface DumpProvider {

    @NotNull String BYTEBIN_URL = "https://bytebin.lucko.me";
    @NotNull String VIEWER_URL = "https://william278.net/dump";

    @NotNull
    Toilet getToilet();

    @NotNull
    @Blocking
    default String createDump(@NotNull CommandUser u) {
        return getToilet().dump(getPluginStatus(), u instanceof OnlineUser o
                        ? new DumpUser(o.getName(), o.getUuid()) : null,
                getRedisInfo()).toString();
    }

    @NotNull
    default DumpOptions getDumpOptions() {
        return builder()
                .bytebinUrl(BYTEBIN_URL)
                .viewerUrl(VIEWER_URL)
                .projectMeta(ProjectMeta.builder()
                        .id("husksync")
                        .name("HuskSync")
                        .version(getPlugin().getPluginVersion().toString())
                        .md5("unknown")
                        .author("William278")
                        .sourceCode("https://github.com/WiIIiam278/HuskSync")
                        .website("https://william278.net/project/husksync")
                        .support("https://discord.gg/tVYhJfyDWG")
                        .build())
                .fileInclusionRules(List.of(
                        FileInclusionRule.configFile("config.yml", "Config File"),
                        FileInclusionRule.configFile(getMessagesFile(), "Locales File")
                ))
                .compatibilityRules(List.of(
                        getCompatibilityWarning("CombatLogX", "Combat loggers require additional" +
                                "configuration for use with HuskSync. Check https://william278.net/docs/husksync/event-priorities"),
                        getIncompatibleNotice("UltimateAutoRestart", "Restart plugins are not" +
                                "compatible with HuskSync as they affect the way the server shuts down, preventing data" +
                                "from saving correctly during a restart. Check https://william278.net/docs/husksync/troubleshooting")
                ))
                .build();
    }

    @NotNull
    @Blocking
    private PluginStatus getPluginStatus() {
        return PluginStatus.builder()
                .blocks(List.of(getSystemStatus(), getRegisteredDataTypes()))
                .build();
    }

    @NotNull
    @Blocking
    private PluginStatus.MapStatusBlock getSystemStatus() {
        return new PluginStatus.MapStatusBlock(
                Map.ofEntries(
                        Map.entry("Language", StatusLine.LANGUAGE.getValue(getPlugin())),
                        Map.entry("Database Type", StatusLine.DATABASE_TYPE.getValue(getPlugin())),
                        Map.entry("Database Local", StatusLine.IS_DATABASE_LOCAL.getValue(getPlugin())),
                        Map.entry("Locked User Handler", StatusLine.LOCKED_USER_HANDLER.getValue(getPlugin())),
                        Map.entry("Server Name", StatusLine.SERVER_NAME.getValue(getPlugin())),
                        Map.entry("Redis Version", StatusLine.REDIS_VERSION.getValue(getPlugin())),
                        Map.entry("Redis Latency", StatusLine.REDIS_LATENCY.getValue(getPlugin())),
                        Map.entry("Redis Sentinel", StatusLine.USING_REDIS_SENTINEL.getValue(getPlugin())),
                        Map.entry("Redis Database", StatusLine.REDIS_DATABASE.getValue(getPlugin())),
                        Map.entry("Redis User", StatusLine.USING_REDIS_USER.getValue(getPlugin())),
                        Map.entry("Redis Password", StatusLine.USING_REDIS_PASSWORD.getValue(getPlugin())),
                        Map.entry("Redis SSL", StatusLine.REDIS_USING_SSL.getValue(getPlugin())),
                        Map.entry("Redis Local", StatusLine.IS_REDIS_LOCAL.getValue(getPlugin()))
                ),
                "Plugin Status", "fa6-solid:wrench"
        );
    }

    @NotNull
    @Blocking
    private PluginStatus.MapStatusBlock getRegisteredDataTypes() {
        return new PluginStatus.MapStatusBlock(
                getPlugin().getRegisteredDataTypes().stream().collect(Collectors.toMap(
                        i -> i.getKey().asMinimalString(),
                        i -> i.isEnabled() ? "✅ Enabled" : "❌ Disabled",
                        (a, b) -> a)
                ),
                "Registered Data Types", "carbon:data-blob"
        );
    }

    @NotNull
    @Blocking
    private ExtraFile getRedisInfo() {
        return new ExtraFile(
                "redis-status", "Redis Status", "devicon-plain:redis",
                getPlugin().getRedisManager().getStatusDump(),
                "markdown"
        );
    }

    @NotNull
    @SuppressWarnings("SameParameterValue")
    private CompatibilityRule getCompatibilityWarning(@NotNull String plugin, @NotNull String description) {
        return CompatibilityRule.builder()
                .labelToApply(new PluginInfo.Label("Warning", "#fcba03", description))
                .resourceName(plugin).build();
    }

    @NotNull
    @SuppressWarnings("SameParameterValue")
    private CompatibilityRule getIncompatibleNotice(@NotNull String plugin, @NotNull String description) {
        return CompatibilityRule.builder()
                .labelToApply(new PluginInfo.Label("Incompatible", "#ff3300", description))
                .resourceName(plugin).build();
    }

    @NotNull
    private String getMessagesFile() {
        return "messages-%s.yml".formatted(getPlugin().getSettings().getLanguage());
    }

    @NotNull
    HuskSync getPlugin();

}