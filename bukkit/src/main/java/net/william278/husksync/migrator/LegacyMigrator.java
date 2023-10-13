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

package net.william278.husksync.migrator;

import com.zaxxer.hikari.HikariDataSource;
import me.william278.husksync.bukkit.data.DataSerializer;
import net.william278.hslmigrator.HSLConverter;
import net.william278.husksync.HuskSync;
import net.william278.husksync.data.BukkitData;
import net.william278.husksync.data.Data;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.user.User;
import net.william278.husksync.util.BukkitLegacyConverter;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class LegacyMigrator extends Migrator {

    private final HSLConverter hslConverter;
    private String sourceHost;
    private int sourcePort;
    private String sourceUsername;
    private String sourcePassword;
    private String sourceDatabase;
    private String sourcePlayersTable;
    private String sourceDataTable;

    public LegacyMigrator(@NotNull HuskSync plugin) {
        super(plugin);
        this.hslConverter = HSLConverter.getInstance();
        this.sourceHost = plugin.getSettings().getMySqlHost();
        this.sourcePort = plugin.getSettings().getMySqlPort();
        this.sourceUsername = plugin.getSettings().getMySqlUsername();
        this.sourcePassword = plugin.getSettings().getMySqlPassword();
        this.sourceDatabase = plugin.getSettings().getMySqlDatabase();
        this.sourcePlayersTable = "husksync_players";
        this.sourceDataTable = "husksync_data";
    }

    @Override
    public CompletableFuture<Boolean> start() {
        plugin.log(Level.INFO, "Starting migration of legacy HuskSync v1.x data...");
        final long startTime = System.currentTimeMillis();
        return plugin.supplyAsync(() -> {
            // Wipe the existing database, preparing it for data import
            plugin.log(Level.INFO, "Preparing existing database (wiping)...");
            plugin.getDatabase().wipeDatabase();
            plugin.log(Level.INFO, "Successfully wiped user data database (took " + (System.currentTimeMillis() - startTime) + "ms)");

            // Create jdbc driver connection url
            final String jdbcUrl = "jdbc:mysql://" + sourceHost + ":" + sourcePort + "/" + sourceDatabase;

            // Create a new data source for the mpdb converter
            try (final HikariDataSource connectionPool = new HikariDataSource()) {
                plugin.log(Level.INFO, "Establishing connection to legacy database...");
                connectionPool.setJdbcUrl(jdbcUrl);
                connectionPool.setUsername(sourceUsername);
                connectionPool.setPassword(sourcePassword);
                connectionPool.setPoolName((getIdentifier() + "_migrator_pool").toUpperCase(Locale.ENGLISH));

                plugin.log(Level.INFO, "Downloading raw data from the legacy database (this might take a while)...");
                final List<LegacyData> dataToMigrate = new ArrayList<>();
                try (final Connection connection = connectionPool.getConnection()) {
                    try (final PreparedStatement statement = connection.prepareStatement("""
                            SELECT `uuid`, `username`, `inventory`, `ender_chest`, `health`, `max_health`, `health_scale`, `hunger`, `saturation`, `saturation_exhaustion`, `selected_slot`, `status_effects`, `total_experience`, `exp_level`, `exp_progress`, `game_mode`, `statistics`, `is_flying`, `advancements`, `location`
                            FROM `%source_players_table%`
                            INNER JOIN `%source_data_table%`
                            ON `%source_players_table%`.`id` = `%source_data_table%`.`player_id`
                            WHERE `username` IS NOT NULL;
                            """.replaceAll(Pattern.quote("%source_players_table%"), sourcePlayersTable)
                            .replaceAll(Pattern.quote("%source_data_table%"), sourceDataTable))) {
                        try (final ResultSet resultSet = statement.executeQuery()) {
                            int playersMigrated = 0;
                            while (resultSet.next()) {
                                dataToMigrate.add(new LegacyData(
                                        new User(UUID.fromString(resultSet.getString("uuid")),
                                                resultSet.getString("username")),
                                        resultSet.getString("inventory"),
                                        resultSet.getString("ender_chest"),
                                        resultSet.getDouble("health"),
                                        resultSet.getDouble("max_health"),
                                        resultSet.getDouble("health_scale"),
                                        resultSet.getInt("hunger"),
                                        resultSet.getFloat("saturation"),
                                        resultSet.getFloat("saturation_exhaustion"),
                                        resultSet.getInt("selected_slot"),
                                        resultSet.getString("status_effects"),
                                        resultSet.getInt("total_experience"),
                                        resultSet.getInt("exp_level"),
                                        resultSet.getFloat("exp_progress"),
                                        resultSet.getString("game_mode"),
                                        resultSet.getString("statistics"),
                                        resultSet.getBoolean("is_flying"),
                                        resultSet.getString("advancements"),
                                        resultSet.getString("location")
                                ));
                                playersMigrated++;
                                if (playersMigrated % 50 == 0) {
                                    plugin.log(Level.INFO, "Downloaded legacy data for " + playersMigrated + " players...");
                                }
                            }
                        }
                    }
                }
                plugin.log(Level.INFO, "Completed download of " + dataToMigrate.size() + " entries from the legacy database!");
                plugin.log(Level.INFO, "Converting HuskSync 1.x data to the new user data format (this might take a while)...");

                final AtomicInteger playersConverted = new AtomicInteger();
                dataToMigrate.forEach(data -> {
                    final DataSnapshot.Packed convertedData = data.toUserData(hslConverter, plugin);
                    plugin.getDatabase().ensureUser(data.user());
                    try {
                        plugin.getDatabase().addSnapshot(data.user(), convertedData);
                    } catch (Throwable e) {
                        plugin.log(Level.SEVERE, "Failed to migrate legacy data for " + data.user().getUsername() + ": " + e.getMessage());
                        return;
                    }

                    playersConverted.getAndIncrement();
                    if (playersConverted.get() % 50 == 0) {
                        plugin.log(Level.INFO, "Converted legacy data for " + playersConverted + " players...");
                    }
                });
                plugin.log(Level.INFO, "Migration complete for " + dataToMigrate.size() + " users in " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds!");
                return true;
            } catch (Throwable e) {
                plugin.log(Level.SEVERE, "Error while migrating legacy data: " + e.getMessage() + " - are your source database credentials correct?", e);
                return false;
            }
        });
    }

    @Override
    public void handleConfigurationCommand(@NotNull String[] args) {
        if (args.length == 2) {
            if (switch (args[0].toLowerCase(Locale.ENGLISH)) {
                case "host" -> {
                    this.sourceHost = args[1];
                    yield true;
                }
                case "port" -> {
                    try {
                        this.sourcePort = Integer.parseInt(args[1]);
                        yield true;
                    } catch (NumberFormatException e) {
                        yield false;
                    }
                }
                case "username" -> {
                    this.sourceUsername = args[1];
                    yield true;
                }
                case "password" -> {
                    this.sourcePassword = args[1];
                    yield true;
                }
                case "database" -> {
                    this.sourceDatabase = args[1];
                    yield true;
                }
                case "players_table" -> {
                    this.sourcePlayersTable = args[1];
                    yield true;
                }
                case "data_table" -> {
                    this.sourceDataTable = args[1];
                    yield true;
                }
                default -> false;
            }) {
                plugin.log(Level.INFO, getHelpMenu());
                plugin.log(Level.INFO, "Successfully set " + args[0] + " to " +
                        obfuscateDataString(args[1]));
            } else {
                plugin.log(Level.INFO, "Invalid operation, could not set " + args[0] + " to " +
                        obfuscateDataString(args[1]) + " (is it a valid option?)");
            }
        } else {
            plugin.log(Level.INFO, getHelpMenu());
        }
    }

    @NotNull
    @Override
    public String getIdentifier() {
        return "legacy";
    }

    @NotNull
    @Override
    public String getName() {
        return "HuskSync v1.x --> v3.x Migrator";
    }

    @NotNull
    @Override
    public String getHelpMenu() {
        return """
                === HuskSync v1.x --> v3.x Migration Wizard =========
                This will migrate all user data from HuskSync v1.x to
                HuskSync v3.x's new format. To perform the migration,
                please follow the steps below carefully.

                [!] Existing data in the database will be wiped. [!]

                STEP 1] Please ensure no players are on any servers.

                STEP 2] HuskSync will need to connect to the database
                used to hold the existing, legacy HuskSync data.
                If this is the same database as the one you are
                currently using, you probably don't need to change
                anything.
                Please check that the credentials below are the
                correct credentials of the source legacy HuskSync
                database.
                - host: %source_host%
                - port: %source_port%
                - username: %source_username%
                - password: %source_password%
                - database: %source_database%
                - players_table: %source_players_table%
                - data_table: %source_data_table%
                If any of these are not correct, please correct them
                using the command:
                "husksync migrate legacy set <parameter> <value>"
                (e.g.: "husksync migrate legacy set host 1.2.3.4")

                STEP 3] HuskSync will migrate data into the database
                tables configures in the config.yml file of this
                server. Please make sure you're happy with this
                before proceeding.

                STEP 4] To start the migration, please run:
                "husksync migrate legacy start"
                """.replaceAll(Pattern.quote("%source_host%"), obfuscateDataString(sourceHost))
                .replaceAll(Pattern.quote("%source_port%"), Integer.toString(sourcePort))
                .replaceAll(Pattern.quote("%source_username%"), obfuscateDataString(sourceUsername))
                .replaceAll(Pattern.quote("%source_password%"), obfuscateDataString(sourcePassword))
                .replaceAll(Pattern.quote("%source_database%"), sourceDatabase)
                .replaceAll(Pattern.quote("%source_players_table%"), sourcePlayersTable)
                .replaceAll(Pattern.quote("%source_data_table%"), sourceDataTable);
    }

    private record LegacyData(@NotNull User user,
                              @NotNull String serializedInventory, @NotNull String serializedEnderChest,
                              double health, double maxHealth, double healthScale, int hunger, float saturation,
                              float saturationExhaustion, int selectedSlot, @NotNull String serializedPotionEffects,
                              int totalExp, int expLevel, float expProgress,
                              @NotNull String gameMode, @NotNull String serializedStatistics, boolean isFlying,
                              @NotNull String serializedAdvancements, @NotNull String serializedLocation) {

        @NotNull
        public DataSnapshot.Packed toUserData(@NotNull HSLConverter converter, @NotNull HuskSync plugin) {
            try {
                final DataSerializer.StatisticData stats = converter.deserializeStatisticData(serializedStatistics);
                final DataSerializer.PlayerLocation loc = converter.deserializePlayerLocationData(serializedLocation);
                final BukkitLegacyConverter adapter = (BukkitLegacyConverter) plugin.getLegacyConverter()
                        .orElseThrow(() -> new IllegalStateException("Legacy converter not present"));

                return DataSnapshot.builder(plugin)
                        // Inventory
                        .inventory(BukkitData.Items.Inventory.from(
                                adapter.deserializeLegacyItemStacks(serializedInventory),
                                selectedSlot
                        ))

                        // Ender chest
                        .enderChest(BukkitData.Items.EnderChest.adapt(
                                adapter.deserializeLegacyItemStacks(serializedEnderChest)
                        ))

                        // Location
                        .location(BukkitData.Location.from(
                                loc == null ? 0d : loc.x(),
                                loc == null ? 64d : loc.y(),
                                loc == null ? 0d : loc.z(),
                                loc == null ? 90f : loc.yaw(),
                                loc == null ? 180f : loc.pitch(),
                                new Data.Location.World(
                                        loc == null ? "world" : loc.worldName(),
                                        UUID.randomUUID(), "NORMAL"
                                )))

                        // Advancements
                        .advancements(BukkitData.Advancements.from(converter
                                .deserializeAdvancementData(serializedAdvancements).stream()
                                .map(data -> Data.Advancements.Advancement.adapt(data.key(), data.criteriaMap()))
                                .toList()))

                        // Stats
                        .statistics(BukkitData.Statistics.from(
                                BukkitData.Statistics.createStatisticsMap(
                                        convertStatisticMap(stats.untypedStatisticValues()),
                                        convertMaterialStatisticMap(stats.blockStatisticValues()),
                                        convertMaterialStatisticMap(stats.itemStatisticValues()),
                                        convertEntityStatisticMap(stats.entityStatisticValues())
                                )))

                        // Health, hunger, experience & game mode
                        .health(BukkitData.Health.from(health, maxHealth, healthScale))
                        .hunger(BukkitData.Hunger.from(hunger, saturation, saturationExhaustion))
                        .experience(BukkitData.Experience.from(totalExp, expLevel, expProgress))
                        .gameMode(BukkitData.GameMode.from(gameMode, isFlying, isFlying))

                        // Build & pack into new format
                        .saveCause(DataSnapshot.SaveCause.LEGACY_MIGRATION).buildAndPack();
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }

        private Map<String, Integer> convertStatisticMap(@NotNull HashMap<Statistic, Integer> rawMap) {
            final HashMap<String, Integer> convertedMap = new HashMap<>();
            for (Map.Entry<Statistic, Integer> entry : rawMap.entrySet()) {
                convertedMap.put(entry.getKey().getKey().toString(), entry.getValue());
            }
            return convertedMap;
        }

        private Map<String, Map<String, Integer>> convertMaterialStatisticMap(@NotNull HashMap<Statistic, HashMap<Material, Integer>> rawMap) {
            final Map<String, Map<String, Integer>> convertedMap = new HashMap<>();
            for (Map.Entry<Statistic, HashMap<Material, Integer>> entry : rawMap.entrySet()) {
                for (Map.Entry<Material, Integer> materialEntry : entry.getValue().entrySet()) {
                    convertedMap.computeIfAbsent(entry.getKey().getKey().toString(), k -> new HashMap<>())
                            .put(materialEntry.getKey().getKey().toString(), materialEntry.getValue());
                }
            }
            return convertedMap;
        }

        private Map<String, Map<String, Integer>> convertEntityStatisticMap(@NotNull HashMap<Statistic, HashMap<EntityType, Integer>> rawMap) {
            final Map<String, Map<String, Integer>> convertedMap = new HashMap<>();
            for (Map.Entry<Statistic, HashMap<EntityType, Integer>> entry : rawMap.entrySet()) {
                for (Map.Entry<EntityType, Integer> materialEntry : entry.getValue().entrySet()) {
                    convertedMap.computeIfAbsent(entry.getKey().getKey().toString(), k -> new HashMap<>())
                            .put(materialEntry.getKey().getKey().toString(), materialEntry.getValue());
                }
            }
            return convertedMap;
        }

    }

}
