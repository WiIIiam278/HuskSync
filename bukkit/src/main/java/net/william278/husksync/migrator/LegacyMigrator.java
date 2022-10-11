package net.william278.husksync.migrator;

import com.zaxxer.hikari.HikariDataSource;
import me.william278.husksync.bukkit.data.DataSerializer;
import net.william278.hslmigrator.HSLConverter;
import net.william278.husksync.HuskSync;
import net.william278.husksync.data.*;
import net.william278.husksync.player.User;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
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

    private final String minecraftVersion;

    public LegacyMigrator(@NotNull HuskSync plugin) {
        super(plugin);
        this.hslConverter = HSLConverter.getInstance();
        this.sourceHost = plugin.getSettings().mySqlHost;
        this.sourcePort = plugin.getSettings().mySqlPort;
        this.sourceUsername = plugin.getSettings().mySqlUsername;
        this.sourcePassword = plugin.getSettings().mySqlPassword;
        this.sourceDatabase = plugin.getSettings().mySqlDatabase;
        this.sourcePlayersTable = "husksync_players";
        this.sourceDataTable = "husksync_data";
        this.minecraftVersion = plugin.getMinecraftVersion().toString();
    }

    @Override
    public CompletableFuture<Boolean> start() {
        plugin.getLoggingAdapter().log(Level.INFO, "Starting migration of legacy HuskSync v1.x data...");
        final long startTime = System.currentTimeMillis();
        return CompletableFuture.supplyAsync(() -> {
            // Wipe the existing database, preparing it for data import
            plugin.getLoggingAdapter().log(Level.INFO, "Preparing existing database (wiping)...");
            plugin.getDatabase().wipeDatabase().join();
            plugin.getLoggingAdapter().log(Level.INFO, "Successfully wiped user data database (took " + (System.currentTimeMillis() - startTime) + "ms)");

            // Create jdbc driver connection url
            final String jdbcUrl = "jdbc:mysql://" + sourceHost + ":" + sourcePort + "/" + sourceDatabase;

            // Create a new data source for the mpdb converter
            try (final HikariDataSource connectionPool = new HikariDataSource()) {
                plugin.getLoggingAdapter().log(Level.INFO, "Establishing connection to legacy database...");
                connectionPool.setJdbcUrl(jdbcUrl);
                connectionPool.setUsername(sourceUsername);
                connectionPool.setPassword(sourcePassword);
                connectionPool.setPoolName((getIdentifier() + "_migrator_pool").toUpperCase());

                plugin.getLoggingAdapter().log(Level.INFO, "Downloading raw data from the legacy database (this might take a while)...");
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
                                    plugin.getLoggingAdapter().log(Level.INFO, "Downloaded legacy data for " + playersMigrated + " players...");
                                }
                            }
                        }
                    }
                }
                plugin.getLoggingAdapter().log(Level.INFO, "Completed download of " + dataToMigrate.size() + " entries from the legacy database!");
                plugin.getLoggingAdapter().log(Level.INFO, "Converting HuskSync 1.x data to the new user data format (this might take a while)...");

                final AtomicInteger playersConverted = new AtomicInteger();
                dataToMigrate.forEach(data -> data.toUserData(hslConverter, minecraftVersion).thenAccept(convertedData -> {
                    plugin.getDatabase().ensureUser(data.user()).thenRun(() ->
                            plugin.getDatabase().setUserData(data.user(), convertedData, DataSaveCause.LEGACY_MIGRATION)
                                    .exceptionally(exception -> {
                                        plugin.getLoggingAdapter().log(Level.SEVERE, "Failed to migrate legacy data for " + data.user().username + ": " + exception.getMessage());
                                        return null;
                                    })).join();

                    playersConverted.getAndIncrement();
                    if (playersConverted.get() % 50 == 0) {
                        plugin.getLoggingAdapter().log(Level.INFO, "Converted legacy data for " + playersConverted + " players...");
                    }
                }).join());
                plugin.getLoggingAdapter().log(Level.INFO, "Migration complete for " + dataToMigrate.size() + " users in " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds!");
                return true;
            } catch (Exception e) {
                plugin.getLoggingAdapter().log(Level.SEVERE, "Error while migrating legacy data: " + e.getMessage() + " - are your source database credentials correct?");
                return false;
            }
        });
    }

    @Override
    public void handleConfigurationCommand(@NotNull String[] args) {
        if (args.length == 2) {
            if (switch (args[0].toLowerCase()) {
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
                plugin.getLoggingAdapter().log(Level.INFO, getHelpMenu());
                plugin.getLoggingAdapter().log(Level.INFO, "Successfully set " + args[0] + " to " +
                                                           obfuscateDataString(args[1]));
            } else {
                plugin.getLoggingAdapter().log(Level.INFO, "Invalid operation, could not set " + args[0] + " to " +
                                                           obfuscateDataString(args[1]) + " (is it a valid option?)");
            }
        } else {
            plugin.getLoggingAdapter().log(Level.INFO, getHelpMenu());
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
        return "HuskSync v1.x --> v2.x Migrator";
    }

    @NotNull
    @Override
    public String getHelpMenu() {
        return """
                === HuskSync v1.x --> v2.x Migration Wizard =========
                This will migrate all user data from HuskSync v1.x to
                HuskSync v2.x's new format. To perform the migration,
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
        public CompletableFuture<UserData> toUserData(@NotNull HSLConverter converter,
                                                      @NotNull String minecraftVersion) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    final DataSerializer.StatisticData legacyStatisticData = converter
                            .deserializeStatisticData(serializedStatistics);
                    final StatisticsData convertedStatisticData = new StatisticsData(
                            convertStatisticMap(legacyStatisticData.untypedStatisticValues()),
                            convertMaterialStatisticMap(legacyStatisticData.blockStatisticValues()),
                            convertMaterialStatisticMap(legacyStatisticData.itemStatisticValues()),
                            convertEntityStatisticMap(legacyStatisticData.entityStatisticValues()));

                    final List<AdvancementData> convertedAdvancements = converter
                            .deserializeAdvancementData(serializedAdvancements)
                            .stream().map(data -> new AdvancementData(data.key(), data.criteriaMap())).toList();

                    final DataSerializer.PlayerLocation legacyLocationData = converter
                            .deserializePlayerLocationData(serializedLocation);
                    final LocationData convertedLocationData = new LocationData(
                            legacyLocationData == null ? "world" : legacyLocationData.worldName(),
                            UUID.randomUUID(),
                            "NORMAL",
                            legacyLocationData == null ? 0d : legacyLocationData.x(),
                            legacyLocationData == null ? 64d : legacyLocationData.y(),
                            legacyLocationData == null ? 0d : legacyLocationData.z(),
                            legacyLocationData == null ? 90f : legacyLocationData.yaw(),
                            legacyLocationData == null ? 180f : legacyLocationData.pitch());

                    return UserData.builder(minecraftVersion)
                            .setStatus(new StatusData(health, maxHealth, healthScale, hunger, saturation,
                                    saturationExhaustion, selectedSlot, totalExp, expLevel, expProgress, gameMode, isFlying))
                            .setInventory(new ItemData(serializedInventory))
                            .setEnderChest(new ItemData(serializedEnderChest))
                            .setPotionEffects(new PotionEffectData(serializedPotionEffects))
                            .setAdvancements(convertedAdvancements)
                            .setStatistics(convertedStatisticData)
                            .setLocation(convertedLocationData)
                            .build();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        private Map<String, Integer> convertStatisticMap(@NotNull HashMap<Statistic, Integer> rawMap) {
            final HashMap<String, Integer> convertedMap = new HashMap<>();
            for (Map.Entry<Statistic, Integer> entry : rawMap.entrySet()) {
                convertedMap.put(entry.getKey().toString(), entry.getValue());
            }
            return convertedMap;
        }

        private Map<String, Map<String, Integer>> convertMaterialStatisticMap(@NotNull HashMap<Statistic, HashMap<Material, Integer>> rawMap) {
            final Map<String, Map<String, Integer>> convertedMap = new HashMap<>();
            for (Map.Entry<Statistic, HashMap<Material, Integer>> entry : rawMap.entrySet()) {
                for (Map.Entry<Material, Integer> materialEntry : entry.getValue().entrySet()) {
                    convertedMap.computeIfAbsent(entry.getKey().toString(), k -> new HashMap<>())
                            .put(materialEntry.getKey().toString(), materialEntry.getValue());
                }
            }
            return convertedMap;
        }

        private Map<String, Map<String, Integer>> convertEntityStatisticMap(@NotNull HashMap<Statistic, HashMap<EntityType, Integer>> rawMap) {
            final Map<String, Map<String, Integer>> convertedMap = new HashMap<>();
            for (Map.Entry<Statistic, HashMap<EntityType, Integer>> entry : rawMap.entrySet()) {
                for (Map.Entry<EntityType, Integer> materialEntry : entry.getValue().entrySet()) {
                    convertedMap.computeIfAbsent(entry.getKey().toString(), k -> new HashMap<>())
                            .put(materialEntry.getKey().toString(), materialEntry.getValue());
                }
            }
            return convertedMap;
        }

    }

}
