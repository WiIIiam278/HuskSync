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
import net.william278.husksync.data.DataContainer;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.player.CommandUser;
import net.william278.husksync.player.User;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.StringJoiner;

public class DataSnapshotOverview {

    private final HuskSync plugin;
    private final User dataOwner;
    private final DataSnapshot.Unpacked snapshot;

    private DataSnapshotOverview(@NotNull DataSnapshot.Unpacked snapshot, @NotNull User dataOwner,
                                 @NotNull HuskSync plugin) {
        this.snapshot = snapshot;
        this.dataOwner = dataOwner;
        this.plugin = plugin;
    }

    @NotNull
    public static DataSnapshotOverview of(@NotNull DataSnapshot.Unpacked snapshot, @NotNull User dataOwner,
                                          @NotNull HuskSync plugin) {
        return new DataSnapshotOverview(snapshot, dataOwner, plugin);
    }

    public void show(@NotNull CommandUser user) {
        // Title message, timestamp, owner and cause.
        final Locales locales = plugin.getLocales();
        locales.getLocale("data_manager_title", snapshot.getShortId(), snapshot.getId().toString(),
                        dataOwner.getUsername(), dataOwner.getUuid().toString())
                .ifPresent(user::sendMessage);
        locales.getLocale("data_manager_timestamp",
//                        new SimpleDateFormat("MMM dd yyyy, HH:mm:ss.sss").format(snapshot.getTimestamp())) todo fix
                    snapshot.getTimestamp().toString())
                .ifPresent(user::sendMessage);
        if (snapshot.isPinned()) {
            locales.getLocale("data_manager_pinned").ifPresent(user::sendMessage);
        }
        locales.getLocale("data_manager_cause", snapshot.getSaveCause().name().toLowerCase(Locale.ENGLISH).replaceAll("_", " "))
                .ifPresent(user::sendMessage);

        // User status data, if present in the snapshot
        final Optional<DataContainer.Health> health = snapshot.getHealth();
        final Optional<DataContainer.Food> food = snapshot.getFood();
        final Optional<DataContainer.Experience> experience = snapshot.getExperience();
        final Optional<DataContainer.GameMode> gameMode = snapshot.getGameMode();
        if (health.isPresent() && food.isPresent() && experience.isPresent() && gameMode.isPresent()) {
            locales.getLocale("data_manager_status",
                            Integer.toString((int) health.get().getHealth()),
                            Integer.toString((int) health.get().getMaxHealth()),
                            Integer.toString(food.get().getFoodLevel()),
                            Integer.toString(experience.get().getExpLevel()),
                            gameMode.get().getGameMode().toLowerCase(Locale.ENGLISH))
                    .ifPresent(user::sendMessage);
        }

        // Advancement and statistic data, if both are present in the snapshot
        snapshot.getAdvancements()
                .flatMap(advancementData -> snapshot.getStatistics()
                        .flatMap(statisticsData -> locales.getLocale("data_manager_advancements_statistics",
                                Integer.toString(advancementData.getCompleted().size()),
                                generateAdvancementPreview(advancementData.getCompleted(), locales),
                                String.format("%.2f", (((statisticsData.getGenericStatistics().getOrDefault(
                                        "PLAY_ONE_MINUTE", 0)) / 20d) / 60d) / 60d))))
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
    private String generateAdvancementPreview(@NotNull List<DataContainer.Advancements.Advancement> advancementData, @NotNull Locales locales) {
        final StringJoiner joiner = new StringJoiner("\n");
        final List<DataContainer.Advancements.Advancement> advancementsToPreview = advancementData.stream()
                .filter(id -> !id.getKey().startsWith("minecraft:recipe")).toList();
        final int PREVIEW_SIZE = 8;
        for (int i = 0; i < advancementsToPreview.size(); i++) {
            joiner.add(advancementsToPreview.get(i).getKey());
            if (i >= PREVIEW_SIZE) {
                break;
            }
        }
        final int remainingAdvancements = advancementsToPreview.size() - PREVIEW_SIZE;
        if (remainingAdvancements > 0) {
            joiner.add(locales.getRawLocale("data_manager_advancements_preview_remaining",
                    Integer.toString(remainingAdvancements)).orElse("+" + remainingAdvancements + "…"));
        }
        return joiner.toString();
    }

}
