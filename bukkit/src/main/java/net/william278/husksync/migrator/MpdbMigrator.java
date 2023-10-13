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
import net.william278.husksync.BukkitHuskSync;
import net.william278.husksync.HuskSync;
import net.william278.husksync.data.BukkitData;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.user.User;
import net.william278.mpdbconverter.MPDBConverter;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * A migrator for migrating MySQLPlayerDataBridge data to HuskSync {@link DataSnapshot}s
 */
public class MpdbMigrator extends Migrator {

    private final MPDBConverter mpdbConverter;
    private String sourceHost;
    private int sourcePort;
    private String sourceUsername;
    private String sourcePassword;
    private String sourceDatabase;
    private String sourceInventoryTable;
    private String sourceEnderChestTable;
    private String sourceExperienceTable;

    public MpdbMigrator(@NotNull BukkitHuskSync plugin) {
        super(plugin);
        this.mpdbConverter = MPDBConverter.getInstance(Objects.requireNonNull(
                Bukkit.getPluginManager().getPlugin("MySQLPlayerDataBridge"),
                "MySQLPlayerDataBridge dependency not found!"
        ));
        this.sourceHost = plugin.getSettings().getMySqlHost();
        this.sourcePort = plugin.getSettings().getMySqlPort();
        this.sourceUsername = plugin.getSettings().getMySqlUsername();
        this.sourcePassword = plugin.getSettings().getMySqlPassword();
        this.sourceDatabase = plugin.getSettings().getMySqlDatabase();
        this.sourceInventoryTable = "mpdb_inventory";
        this.sourceEnderChestTable = "mpdb_enderchest";
        this.sourceExperienceTable = "mpdb_experience";

    }

    @Override
    public CompletableFuture<Boolean> start() {
        plugin.log(Level.INFO, "Starting migration from MySQLPlayerDataBridge to HuskSync...");
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
                plugin.log(Level.INFO, "Establishing connection to MySQLPlayerDataBridge database...");
                connectionPool.setJdbcUrl(jdbcUrl);
                connectionPool.setUsername(sourceUsername);
                connectionPool.setPassword(sourcePassword);
                connectionPool.setPoolName((getIdentifier() + "_migrator_pool").toUpperCase(Locale.ENGLISH));

                plugin.log(Level.INFO, "Downloading raw data from the MySQLPlayerDataBridge database (this might take a while)...");
                final List<MpdbData> dataToMigrate = new ArrayList<>();
                try (final Connection connection = connectionPool.getConnection()) {
                    try (final PreparedStatement statement = connection.prepareStatement("""
                            SELECT `%source_inventory_table%`.`player_uuid`, `%source_inventory_table%`.`player_name`, `inventory`, `armor`, `enderchest`, `exp_lvl`, `exp`, `total_exp`
                            FROM `%source_inventory_table%`
                                INNER JOIN `%source_ender_chest_table%`
                                    ON `%source_inventory_table%`.`player_uuid` = `%source_ender_chest_table%`.`player_uuid`
                                INNER JOIN `%source_xp_table%`
                                    ON `%source_inventory_table%`.`player_uuid` = `%source_xp_table%`.`player_uuid`;
                            """.replaceAll(Pattern.quote("%source_inventory_table%"), sourceInventoryTable)
                            .replaceAll(Pattern.quote("%source_ender_chest_table%"), sourceEnderChestTable)
                            .replaceAll(Pattern.quote("%source_xp_table%"), sourceExperienceTable))) {
                        try (final ResultSet resultSet = statement.executeQuery()) {
                            int playersMigrated = 0;
                            while (resultSet.next()) {
                                dataToMigrate.add(new MpdbData(
                                        new User(UUID.fromString(resultSet.getString("player_uuid")),
                                                resultSet.getString("player_name")),
                                        resultSet.getString("inventory"),
                                        resultSet.getString("armor"),
                                        resultSet.getString("enderchest"),
                                        resultSet.getInt("exp_lvl"),
                                        resultSet.getInt("exp"),
                                        resultSet.getInt("total_exp")
                                ));
                                playersMigrated++;
                                if (playersMigrated % 25 == 0) {
                                    plugin.log(Level.INFO, "Downloaded MySQLPlayerDataBridge data for " + playersMigrated + " players...");
                                }
                            }
                        }
                    }
                }
                plugin.log(Level.INFO, "Completed download of " + dataToMigrate.size() + " entries from the MySQLPlayerDataBridge database!");
                plugin.log(Level.INFO, "Converting raw MySQLPlayerDataBridge data to HuskSync user data (this might take a while)...");

                final AtomicInteger playersConverted = new AtomicInteger();
                dataToMigrate.forEach(data -> {
                    final DataSnapshot.Packed convertedData = data.toUserData(mpdbConverter, plugin);
                    plugin.getDatabase().ensureUser(data.user());
                    plugin.getDatabase().addSnapshot(data.user(), convertedData);
                    playersConverted.getAndIncrement();
                    if (playersConverted.get() % 50 == 0) {
                        plugin.log(Level.INFO, "Converted MySQLPlayerDataBridge data for " + playersConverted + " players...");
                    }
                });
                plugin.log(Level.INFO, "Migration complete for " + dataToMigrate.size() + " users in " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds!");
                return true;
            } catch (Exception e) {
                plugin.log(Level.SEVERE, "Error while migrating data: " + e.getMessage() + " - are your source database credentials correct?");
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
                case "inventory_table" -> {
                    this.sourceInventoryTable = args[1];
                    yield true;
                }
                case "ender_chest_table" -> {
                    this.sourceEnderChestTable = args[1];
                    yield true;
                }
                case "experience_table" -> {
                    this.sourceExperienceTable = args[1];
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
        return "mpdb";
    }

    @NotNull
    @Override
    public String getName() {
        return "MySQLPlayerDataBridge Migrator";
    }

    @NotNull
    @Override
    public String getHelpMenu() {
        return """
                === MySQLPlayerDataBridge Migration Wizard ==========
                NOTE: This migrator currently WORKS WITH MPDB version
                v4.9.2 and below!
                
                This will migrate inventories, ender chests and XP
                from the MySQLPlayerDataBridge plugin to HuskSync.

                To prevent excessive migration times, other non-vital
                data will not be transferred.

                [!] Existing data in the database will be wiped. [!]

                STEP 1] Please ensure no players are on any servers.

                STEP 2] HuskSync will need to connect to the database
                used to hold the source MySQLPlayerDataBridge data.
                Please check these database parameters are OK:
                - host: %source_host%
                - port: %source_port%
                - username: %source_username%
                - password: %source_password%
                - database: %source_database%
                - inventory_table: %source_inventory_table%
                - ender_chest_table: %source_ender_chest_table%
                - experience_table: %source_xp_table%
                If any of these are not correct, please correct them
                using the command:
                "husksync migrate mpdb set <parameter> <value>"
                (e.g.: "husksync migrate mpdb set host 1.2.3.4")

                STEP 3] HuskSync will migrate data into the database
                tables configures in the config.yml file of this
                server. Please make sure you're happy with this
                before proceeding.

                STEP 4] To start the migration, please run:
                "husksync migrate mpdb start"
                
                NOTE: This migrator currently WORKS WITH MPDB version
                v4.9.2 and below!
                """.replaceAll(Pattern.quote("%source_host%"), obfuscateDataString(sourceHost))
                .replaceAll(Pattern.quote("%source_port%"), Integer.toString(sourcePort))
                .replaceAll(Pattern.quote("%source_username%"), obfuscateDataString(sourceUsername))
                .replaceAll(Pattern.quote("%source_password%"), obfuscateDataString(sourcePassword))
                .replaceAll(Pattern.quote("%source_database%"), sourceDatabase)
                .replaceAll(Pattern.quote("%source_inventory_table%"), sourceInventoryTable)
                .replaceAll(Pattern.quote("%source_ender_chest_table%"), sourceEnderChestTable)
                .replaceAll(Pattern.quote("%source_xp_table%"), sourceExperienceTable);
    }

    /**
     * Represents data exported from the MySQLPlayerDataBridge source database
     *
     * @param user                 The user whose data is being migrated
     * @param serializedInventory  The serialized inventory data
     * @param serializedArmor      The serialized armor data
     * @param serializedEnderChest The serialized ender chest data
     * @param expLevel             The player's current XP level
     * @param expProgress          The player's current XP progress
     * @param totalExp             The player's total XP score
     */
    private record MpdbData(
            @NotNull User user,
            @NotNull String serializedInventory,
            @NotNull String serializedArmor,
            @NotNull String serializedEnderChest,
            int expLevel,
            float expProgress,
            int totalExp
    ) {

        /**
         * Converts exported MySQLPlayerDataBridge data into HuskSync's {@link DataSnapshot} object format
         *
         * @param converter The {@link MPDBConverter} to use for converting to {@link ItemStack}s
         * @return A {@link CompletableFuture} that will resolve to the converted {@link DataSnapshot} object
         */
        @NotNull
        public DataSnapshot.Packed toUserData(@NotNull MPDBConverter converter, @NotNull HuskSync plugin) {
            // Combine inventory and armor
            final Inventory inventory = Bukkit.createInventory(null, InventoryType.PLAYER);
            inventory.setContents(converter.getItemStackFromSerializedData(serializedInventory));
            final ItemStack[] armor = converter.getItemStackFromSerializedData(serializedArmor).clone();
            for (int i = 36; i < 36 + armor.length; i++) {
                inventory.setItem(i, armor[i - 36]);
            }
            final ItemStack[] enderChest = converter.getItemStackFromSerializedData(serializedEnderChest);

            // Create user data record
            return DataSnapshot.builder(plugin)
                    .inventory(BukkitData.Items.Inventory.from(inventory.getContents(), 0))
                    .enderChest(BukkitData.Items.EnderChest.adapt(enderChest))
                    .experience(BukkitData.Experience.from(totalExp, expLevel, expProgress))
                    .gameMode(BukkitData.GameMode.from("SURVIVAL", false, false))
                    .saveCause(DataSnapshot.SaveCause.MPDB_MIGRATION)
                    .buildAndPack();
        }

    }

}
