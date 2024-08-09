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
import net.william278.husksync.config.Locales;
import net.william278.husksync.data.Data;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.user.CommandUser;
import net.william278.husksync.user.User;
import org.jetbrains.annotations.NotNull;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.StringJoiner;

public class DataSnapshotOverview {

    private final HuskSync plugin;
    private final User dataOwner;
    private final DataSnapshot.Unpacked snapshot;
    private final long snapshotSize;

    private DataSnapshotOverview(@NotNull DataSnapshot.Unpacked snapshot, long snapshotSize,
                                 @NotNull User dataOwner, @NotNull HuskSync plugin) {
        this.snapshot = snapshot;
        this.snapshotSize = snapshotSize;
        this.dataOwner = dataOwner;
        this.plugin = plugin;
    }

    @NotNull
    public static DataSnapshotOverview of(@NotNull DataSnapshot.Unpacked snapshot, long snapshotSize,
                                          @NotNull User dataOwner, @NotNull HuskSync plugin) {
        return new DataSnapshotOverview(snapshot, snapshotSize, dataOwner, plugin);
    }

    public void show(@NotNull CommandUser user) {
        // Title message, timestamp, owner and cause.
        final Locales locales = plugin.getLocales();
        locales.getLocale("data_manager_title", snapshot.getShortId(), snapshot.getId().toString(),
                        dataOwner.getUsername(), dataOwner.getUuid().toString())
                .ifPresent(user::sendMessage);
        locales.getLocale("data_manager_timestamp",
                        snapshot.getTimestamp().format(DateTimeFormatter
                                .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)),
                        snapshot.getTimestamp().toString())
                .ifPresent(user::sendMessage);
        if (snapshot.isPinned()) {
            locales.getLocale("data_manager_pinned")
                    .ifPresent(user::sendMessage);
        }
        locales.getLocale("data_manager_cause", snapshot.getSaveCause().getLocale(plugin))
                .ifPresent(user::sendMessage);
        locales.getLocale("data_manager_server", snapshot.getServerName())
                .ifPresent(user::sendMessage);

        // User status data, if present in the snapshot
        final Optional<Data.Health> health = snapshot.getHealth();
        final Optional<Data.Attributes> attributes = snapshot.getAttributes();
        final Optional<Data.Hunger> food = snapshot.getHunger();
        final Optional<Data.Experience> exp = snapshot.getExperience();
        final Optional<Data.GameMode> mode = snapshot.getGameMode();
        if (health.isPresent() && attributes.isPresent() && food.isPresent() && exp.isPresent() && mode.isPresent()) {
            locales.getLocale("data_manager_status",
                            Integer.toString((int) health.get().getHealth()),
                            Integer.toString((int) attributes.get().getMaxHealth()),
                            Integer.toString(food.get().getFoodLevel()),
                            Integer.toString(exp.get().getExpLevel()),
                            mode.get().getGameMode().toLowerCase(Locale.ENGLISH))
                    .ifPresent(user::sendMessage);
        }

        // Snapshot size
        locales.getLocale("data_manager_size", String.format("%.2fKiB", snapshotSize / 1024f))
                .ifPresent(user::sendMessage);

        // Advancement and statistic data, if both are present in the snapshot
        snapshot.getAdvancements()
                .flatMap(advancementData -> snapshot.getStatistics()
                        .flatMap(statisticsData -> locales.getLocale("data_manager_advancements_statistics",
                                Integer.toString(advancementData.getCompletedExcludingRecipes().size()),
                                generateAdvancementPreview(advancementData.getCompletedExcludingRecipes(), locales),
                                String.format("%.2f", (((statisticsData.getGenericStatistics().getOrDefault(
                                        "minecraft:play_one_minute", 0)) / 20d) / 60d) / 60d))))
                .ifPresent(user::sendMessage);

        if (user.hasPermission("husksync.command.inventory.edit")
            && user.hasPermission("husksync.command.enderchest.edit")) {
            locales.getLocale("data_manager_item_buttons", dataOwner.getUsername(), snapshot.getId().toString())
                    .ifPresent(user::sendMessage);
        }
        locales.getLocale("data_manager_management_buttons", dataOwner.getUsername(), snapshot.getId().toString())
                .ifPresent(user::sendMessage);
        if (user.hasPermission("husksync.command.userdata.dump")) {
            locales.getLocale("data_manager_system_buttons", dataOwner.getUsername(), snapshot.getId().toString())
                    .ifPresent(user::sendMessage);
        }
    }

    @NotNull
    private String generateAdvancementPreview(@NotNull List<Data.Advancements.Advancement> advancementData, @NotNull Locales locales) {
        final StringJoiner joiner = new StringJoiner("\n");
        final int PREVIEW_SIZE = 8;
        for (int i = 0; i < advancementData.size(); i++) {
            joiner.add(advancementData.get(i).getKey());
            if (i >= PREVIEW_SIZE) {
                break;
            }
        }
        final int remaining = advancementData.size() - PREVIEW_SIZE;
        if (remaining > 0) {
            joiner.add(locales.getRawLocale("data_manager_advancements_preview_remaining",
                            Integer.toString(remaining))
                    .orElse(String.format("+%s…", remaining)));
        }
        return joiner.toString();
    }

}
