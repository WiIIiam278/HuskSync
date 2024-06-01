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

package net.william278.husksync.hook;

import com.djrapitops.plan.capability.CapabilityService;
import com.djrapitops.plan.extension.*;
import com.djrapitops.plan.extension.annotation.*;
import com.djrapitops.plan.extension.icon.Color;
import com.djrapitops.plan.extension.icon.Family;
import com.djrapitops.plan.extension.icon.Icon;
import com.djrapitops.plan.extension.table.Table;
import com.djrapitops.plan.extension.table.TableColumnFormat;
import net.william278.husksync.HuskSync;
import net.william278.husksync.data.Data;
import net.william278.husksync.data.DataHolder;
import net.william278.husksync.data.DataSnapshot;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class PlanHook {

    private final HuskSync plugin;

    public PlanHook(@NotNull HuskSync plugin) {
        this.plugin = plugin;
    }

    public void hookIntoPlan() {
        if (!areAllCapabilitiesAvailable()) {
            return;
        }
        registerDataExtension();
        handlePlanReload();
    }

    private boolean areAllCapabilitiesAvailable() {
        CapabilityService capabilities = CapabilityService.getInstance();
        return capabilities.hasCapability("DATA_EXTENSION_VALUES");
    }

    private void registerDataExtension() {
        try {
            ExtensionService.getInstance().register(new PlanDataExtension(plugin));
            plugin.log(Level.INFO, "Registered HuskSync Plan data extension");
        } catch (IllegalStateException | IllegalArgumentException e) {
            plugin.log(Level.WARNING, "Failed to register Plan data extension: " + e.getMessage(), e);
        }
    }

    // Re-register the extension when plan enables
    private void handlePlanReload() {
        CapabilityService.getInstance().registerEnableListener(isPlanEnabled -> {
            if (isPlanEnabled) {
                registerDataExtension();
            }
        });
    }

    @TabInfo(
            tab = "Current Status",
            iconName = "id-card",
            iconFamily = Family.SOLID,
            elementOrder = {ElementOrder.VALUES, ElementOrder.TABLE, ElementOrder.GRAPH}
    )
    @TabInfo(
            tab = "Data Snapshots",
            iconName = "clipboard-list",
            iconFamily = Family.SOLID,
            elementOrder = {ElementOrder.VALUES, ElementOrder.TABLE, ElementOrder.GRAPH}
    )
    @TabOrder({"Current Status", "Data Snapshots"})
    @PluginInfo(
            name = "HuskSync",
            iconName = "exchange-alt",
            iconFamily = Family.SOLID,
            color = Color.LIGHT_BLUE
    )
    @SuppressWarnings("unused")
    public static class PlanDataExtension implements DataExtension {

        private HuskSync plugin;

        private static final String UNKNOWN_STRING = "N/A";

        private static final String PINNED_HTML_STRING = "&#128205;&nbsp;";

        protected PlanDataExtension(@NotNull HuskSync plugin) {
            this.plugin = plugin;
        }

        protected PlanDataExtension() {
        }

        @Override
        public CallEvents[] callExtensionMethodsOn() {
            return new CallEvents[]{
                    CallEvents.PLAYER_JOIN,
                    CallEvents.PLAYER_LEAVE
            };
        }

        // Get the user's latest data snapshot
        private Optional<DataSnapshot.Unpacked> getLatestSnapshot(@NotNull UUID uuid) {
            return plugin.getDatabase().getUser(uuid)
                    .flatMap(user -> plugin.getDatabase().getLatestSnapshot(user))
                    .map(snapshot -> snapshot.unpack(plugin));
        }

        @BooleanProvider(
                text = "Has Synced",
                description = "Whether this user has saved, synchronized data.",
                iconName = "exchange-alt",
                iconFamily = Family.SOLID,
                conditionName = "hasSynced",
                hidden = true
        )
        @Tab("Current Status")
        public boolean getUserHasSynced(@NotNull UUID uuid) {
            return getLatestSnapshot(uuid).isPresent();
        }

        @Conditional("hasSynced")
        @NumberProvider(
                text = "Sync Time",
                description = "The last time the user had their data synced with the server.",
                iconName = "clock",
                iconFamily = Family.SOLID,
                format = FormatType.DATE_SECOND,
                priority = 6
        )
        @Tab("Current Status")
        public long getCurrentDataTimestamp(@NotNull UUID uuid) {
            return getLatestSnapshot(uuid)
                    .map(DataSnapshot::getTimestamp)
                    .orElse(OffsetDateTime.now())
                    .toEpochSecond();
        }

        @Conditional("hasSynced")
        @StringProvider(
                text = "Version ID",
                description = "ID of the data version that the user is currently using.",
                iconName = "bolt",
                iconFamily = Family.SOLID,
                priority = 5
        )
        @Tab("Current Status")
        public String getCurrentDataId(@NotNull UUID uuid) {
            return getLatestSnapshot(uuid)
                    .map(DataSnapshot::getShortId)
                    .orElse(UNKNOWN_STRING);
        }

        @Conditional("hasSynced")
        @StringProvider(
                text = "Health",
                description = "The number of health points out of the max health points this player currently has.",
                iconName = "heart",
                iconFamily = Family.SOLID,
                priority = 4
        )
        @Tab("Current Status")
        public String getHealth(@NotNull UUID uuid) {
            return getLatestSnapshot(uuid)
                    .flatMap(DataHolder::getHealth)
                    .map(health -> String.format("%s", health.getHealth()))
                    .orElse(UNKNOWN_STRING);
        }

        @Conditional("hasSynced")
        @NumberProvider(
                text = "Hunger",
                description = "The number of hunger points this player currently has.",
                iconName = "drumstick-bite",
                iconFamily = Family.SOLID,
                priority = 3
        )
        @Tab("Current Status")
        public long getHunger(@NotNull UUID uuid) {
            return getLatestSnapshot(uuid)
                    .flatMap(DataHolder::getHunger)
                    .map(Data.Hunger::getFoodLevel)
                    .orElse(20);
        }

        @Conditional("hasSynced")
        @NumberProvider(
                text = "Experience Level",
                description = "The number of experience levels this player currently has.",
                iconName = "hat-wizard",
                iconFamily = Family.SOLID,
                priority = 2
        )
        @Tab("Current Status")
        public long getExperienceLevel(@NotNull UUID uuid) {
            return getLatestSnapshot(uuid)
                    .flatMap(DataHolder::getExperience)
                    .map(Data.Experience::getExpLevel)
                    .orElse(0);
        }

        @Conditional("hasSynced")
        @StringProvider(
                text = "Game Mode",
                description = "The game mode this player is currently in.",
                iconName = "gamepad",
                iconFamily = Family.SOLID,
                priority = 1
        )
        @Tab("Current Status")
        public String getGameMode(@NotNull UUID uuid) {
            return getLatestSnapshot(uuid)
                    .flatMap(DataHolder::getGameMode)
                    .map(Data.GameMode::getGameMode)
                    .orElse(UNKNOWN_STRING);
        }

        @Conditional("hasSynced")
        @NumberProvider(
                text = "Advancements",
                description = "The number of advancements & recipes the player has progressed in.",
                iconName = "award",
                iconFamily = Family.SOLID
        )
        @Tab("Current Status")
        public long getAdvancementsCompleted(@NotNull UUID playerUUID) {
            return getLatestSnapshot(playerUUID)
                    .flatMap(DataHolder::getAdvancements)
                    .map(Data.Advancements::getCompleted)
                    .stream().count();
        }

        @Conditional("hasSynced")
        @TableProvider(tableColor = Color.LIGHT_BLUE)
        @Tab("Data Snapshots")
        public Table getDataSnapshots(@NotNull UUID playerUUID) {
            final Table.Factory dataSnapshotsTable = Table.builder()
                    .columnOne("Time", new Icon(Family.SOLID, "clock", Color.NONE))
                    .columnOneFormat(TableColumnFormat.DATE_SECOND)
                    .columnTwo("ID", new Icon(Family.SOLID, "bolt", Color.NONE))
                    .columnThree("Cause", new Icon(Family.SOLID, "flag", Color.NONE))
                    .columnFour("Pinned", new Icon(Family.SOLID, "thumbtack", Color.NONE));
            plugin.getDatabase().getUser(playerUUID).ifPresent(user ->
                    plugin.getDatabase().getAllSnapshots(user).forEach(snapshot -> dataSnapshotsTable.addRow(
                            snapshot.getTimestamp().toEpochSecond(),
                            snapshot.getShortId(),
                            snapshot.getSaveCause().getDisplayName(),
                            snapshot.isPinned() ? PINNED_HTML_STRING + "Pinned" : "Unpinned"
                    ))
            );
            return dataSnapshotsTable.build();
        }
    }
}
