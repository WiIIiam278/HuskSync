package me.william278.crossserversync.bungeecord.data;

import me.william278.crossserversync.PlayerData;
import me.william278.crossserversync.CrossServerSyncBungeeCord;
import me.william278.crossserversync.bungeecord.data.sql.Database;

import java.sql.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Level;

public class DataManager {

    private static final CrossServerSyncBungeeCord plugin = CrossServerSyncBungeeCord.getInstance();
    public static PlayerDataCache playerDataCache;

    /**
     * Checks if the player is registered on the database; register them if not.
     *
     * @param playerUUID The UUID of the player to register
     */
    public static void ensurePlayerExists(UUID playerUUID) {
        if (!playerExists(playerUUID)) {
            createPlayerEntry(playerUUID);
        }
    }

    /**
     * Returns whether the player is registered in SQL (an entry in the PLAYER_TABLE)
     *
     * @param playerUUID The UUID of the player
     * @return {@code true} if the player is on the player table
     */
    private static boolean playerExists(UUID playerUUID) {
        try (Connection connection = CrossServerSyncBungeeCord.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM " + Database.PLAYER_TABLE_NAME + " WHERE `uuid`=?;")) {
                statement.setString(1, playerUUID.toString());
                ResultSet resultSet = statement.executeQuery();
                return resultSet.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "An SQL exception occurred", e);
            return false;
        }
    }

    private static void createPlayerEntry(UUID playerUUID) {
        try (Connection connection = CrossServerSyncBungeeCord.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO " + Database.PLAYER_TABLE_NAME + " (`uuid`) VALUES(?);")) {
                statement.setString(1, playerUUID.toString());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "An SQL exception occurred", e);
        }
    }

    public static PlayerData getPlayerData(UUID playerUUID) {
        try (Connection connection = CrossServerSyncBungeeCord.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM " + Database.DATA_TABLE_NAME + " WHERE `player_id`=(SELECT `id` FROM " + Database.PLAYER_TABLE_NAME + " WHERE `uuid`=?);")) {
                statement.setString(1, playerUUID.toString());
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    final UUID dataVersionUUID = UUID.fromString(resultSet.getString("version_uuid"));
                    final Timestamp dataSaveTimestamp = resultSet.getTimestamp("timestamp");
                    final String serializedInventory = resultSet.getString("inventory");
                    final String serializedEnderChest = resultSet.getString("ender_chest");
                    final double health = resultSet.getDouble("health");
                    final double maxHealth = resultSet.getDouble("max_health");
                    final int hunger = resultSet.getInt("hunger");
                    final float saturation = resultSet.getFloat("saturation");
                    final float saturationExhaustion = resultSet.getFloat("saturation_exhaustion");
                    final int selectedSlot = resultSet.getInt("selected_slot");
                    final String serializedStatusEffects = resultSet.getString("status_effects");
                    final int totalExperience = resultSet.getInt("total_experience");
                    final int expLevel = resultSet.getInt("exp_level");
                    final float expProgress = resultSet.getInt("exp_progress");
                    final String gameMode = resultSet.getString("game_mode");
                    final boolean isFlying = resultSet.getBoolean("is_flying");
                    final String serializedAdvancementData = resultSet.getString("advancements");
                    final String serializedLocationData = resultSet.getString(  "location");

                    final String serializedStatisticData = resultSet.getString("statistics");

                    return new PlayerData(playerUUID, dataVersionUUID, serializedInventory, serializedEnderChest,
                            health, maxHealth, hunger, saturation, saturationExhaustion, selectedSlot, serializedStatusEffects,
                            totalExperience, expLevel, expProgress, gameMode, serializedStatisticData, isFlying,
                            serializedAdvancementData, serializedLocationData);
                } else {
                    return PlayerData.DEFAULT_PLAYER_DATA(playerUUID);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "An SQL exception occurred", e);
            return null;
        }
    }

    public static void updatePlayerData(PlayerData playerData) {
        // Ignore if the Spigot server didn't properly sync the previous data

        // Add the new player data to the cache
        playerDataCache.updatePlayer(playerData);

        // SQL: If the player has cached data, update it, otherwise insert new data.
        if (playerHasCachedData(playerData.getPlayerUUID())) {
            updatePlayerSQLData(playerData);
        } else {
            insertPlayerData(playerData);
        }
    }

    private static void updatePlayerSQLData(PlayerData playerData) {
        try (Connection connection = CrossServerSyncBungeeCord.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE " + Database.DATA_TABLE_NAME + " SET `version_uuid`=?, `timestamp`=?, `inventory`=?, `ender_chest`=?, `health`=?, `max_health`=?, `hunger`=?, `saturation`=?, `saturation_exhaustion`=?, `selected_slot`=?, `status_effects`=?, `total_experience`=?, `exp_level`=?, `exp_progress`=?, `game_mode`=?, `statistics`=?, `is_flying`=?, `advancements`=?, `location`=? WHERE `player_id`=(SELECT `id` FROM " + Database.PLAYER_TABLE_NAME + " WHERE `uuid`=?);")) {
                statement.setString(1, playerData.getDataVersionUUID().toString());
                statement.setTimestamp(2, new Timestamp(Instant.now().getEpochSecond()));
                statement.setString(3, playerData.getSerializedInventory());
                statement.setString(4, playerData.getSerializedEnderChest());
                statement.setDouble(5, playerData.getHealth()); // Health
                statement.setDouble(6, playerData.getMaxHealth()); // Max health
                statement.setInt(7, playerData.getHunger()); // Hunger
                statement.setFloat(8, playerData.getSaturation()); // Saturation
                statement.setFloat(9, playerData.getSaturationExhaustion()); // Saturation exhaustion
                statement.setInt(10, playerData.getSelectedSlot()); // Current selected slot
                statement.setString(11, playerData.getSerializedEffectData()); // Status effects
                statement.setInt(12, playerData.getTotalExperience()); // Total Experience
                statement.setInt(13, playerData.getExpLevel()); // Exp level
                statement.setFloat(14, playerData.getExpProgress()); // Exp progress
                statement.setString(15, playerData.getGameMode()); // GameMode
                statement.setString(16, playerData.getSerializedStatistics()); // Statistics
                statement.setBoolean(17, playerData.isFlying()); // Is flying
                statement.setString(18, playerData.getSerializedAdvancements()); // Advancements
                statement.setString(19, playerData.getSerializedLocation()); // Location

                statement.setString(20, playerData.getPlayerUUID().toString());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "An SQL exception occurred", e);
        }
    }

    private static void insertPlayerData(PlayerData playerData) {
        try (Connection connection = CrossServerSyncBungeeCord.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO " + Database.DATA_TABLE_NAME + " (`player_id`,`version_uuid`,`timestamp`,`inventory`,`ender_chest`,`health`,`max_health`,`hunger`,`saturation`,`saturation_exhaustion`,`selected_slot`,`status_effects`,`total_experience`,`exp_level`,`exp_progress`,`game_mode`,`statistics`,`is_flying`,`advancements`,`location`) VALUES((SELECT `id` FROM " + Database.PLAYER_TABLE_NAME + " WHERE `uuid`=?),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);")) {
                statement.setString(1, playerData.getPlayerUUID().toString());
                statement.setString(2, playerData.getDataVersionUUID().toString());
                statement.setTimestamp(3, new Timestamp(Instant.now().getEpochSecond()));
                statement.setString(4, playerData.getSerializedInventory());
                statement.setString(5, playerData.getSerializedEnderChest());
                statement.setDouble(6, playerData.getHealth()); // Health
                statement.setDouble(7, playerData.getMaxHealth()); // Max health
                statement.setInt(8, playerData.getHunger()); // Hunger
                statement.setFloat(9, playerData.getSaturation()); // Saturation
                statement.setFloat(10, playerData.getSaturationExhaustion()); // Saturation exhaustion
                statement.setInt(11, playerData.getSelectedSlot()); // Current selected slot
                statement.setString(12, playerData.getSerializedEffectData()); // Status effects
                statement.setInt(13, playerData.getTotalExperience()); // Total Experience
                statement.setInt(14, playerData.getExpLevel()); // Exp level
                statement.setFloat(15, playerData.getExpProgress()); // Exp progress
                statement.setString(16, playerData.getGameMode()); // GameMode
                statement.setString(17, playerData.getSerializedStatistics()); // Statistics
                statement.setBoolean(18, playerData.isFlying()); // Is flying
                statement.setString(19, playerData.getSerializedAdvancements()); // Advancements
                statement.setString(20, playerData.getSerializedLocation()); // Location

                statement.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "An SQL exception occurred", e);
        }
    }

    /**
     * Returns whether the player has cached data saved in SQL (an entry in the DATA_TABLE)
     *
     * @param playerUUID The UUID of the player
     * @return {@code true} if the player has an entry in the data table
     */
    private static boolean playerHasCachedData(UUID playerUUID) {
        try (Connection connection = CrossServerSyncBungeeCord.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM " + Database.DATA_TABLE_NAME + " WHERE `player_id`=(SELECT `id` FROM " + Database.PLAYER_TABLE_NAME + " WHERE `uuid`=?);")) {
                statement.setString(1, playerUUID.toString());
                ResultSet resultSet = statement.executeQuery();
                return resultSet.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "An SQL exception occurred", e);
            return false;
        }
    }

    /**
     * A cache of PlayerData
     */
    public static class PlayerDataCache {
        // The cached player data
        public HashSet<PlayerData> playerData;

        public PlayerDataCache() {
            playerData = new HashSet<>();
        }

        /**
         * Update ar add data for a player to the cache
         *
         * @param newData The player's new/updated {@link PlayerData}
         */
        public void updatePlayer(PlayerData newData) {
            // Remove the old data if it exists
            PlayerData oldData = null;
            for (PlayerData data : playerData) {
                if (data.getPlayerUUID() == newData.getPlayerUUID()) {
                    oldData = data;
                }
            }
            if (oldData != null) {
                playerData.remove(oldData);
            }

            // Add the new data
            playerData.add(newData);
        }

        /**
         * Get a player's {@link PlayerData} by their {@link UUID}
         *
         * @param playerUUID The {@link UUID} of the player to check
         * @return The player's {@link PlayerData}
         */
        public PlayerData getPlayer(UUID playerUUID) {
            for (PlayerData data : playerData) {
                if (data.getPlayerUUID() == playerUUID) {
                    return data;
                }
            }
            return null;
        }

    }
}
