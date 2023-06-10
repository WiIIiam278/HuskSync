/*
 * This file is part of HuskSync by William278. Do not redistribute!
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  All rights reserved.
 *
 *  This source code is provided as reference to licensed individuals that have purchased the HuskSync
 *  plugin once from any of the official sources it is provided. The availability of this code does
 *  not grant you the rights to modify, re-distribute, compile or redistribute this source code or
 *  "plugin" outside this intended purpose. This license does not cover libraries developed by third
 *  parties that are utilised in the plugin.
 */

package net.william278.husksync.config;

import net.william278.annotaml.YamlComment;
import net.william278.annotaml.YamlFile;
import net.william278.annotaml.YamlKey;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Plugin settings, read from config.yml
 */
@YamlFile(header = """
        ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
        ┃        HuskSync Config       ┃
        ┃    Developed by William278   ┃
        ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
        ┣╸ Information: https://william278.net/project/husksync
        ┗╸ Documentation: https://william278.net/docs/husksync""",
        versionField = "config_version", versionNumber = 4)
public class Settings {

    // Top-level settings
    @YamlKey("language")
    private String language = "en-gb";

    @YamlKey("check_for_updates")
    private boolean checkForUpdates = true;

    @YamlKey("cluster_id")
    private String clusterId = "";

    @YamlKey("debug_logging")
    private boolean debugLogging = false;


    // Database settings
    @YamlComment("Database connection settings")
    @YamlKey("database.credentials.host")
    private String mySqlHost = "localhost";

    @YamlKey("database.credentials.port")
    private int mySqlPort = 3306;

    @YamlKey("database.credentials.database")
    private String mySqlDatabase = "HuskSync";

    @YamlKey("database.credentials.username")
    private String mySqlUsername = "root";

    @YamlKey("database.credentials.password")
    private String mySqlPassword = "pa55w0rd";

    @YamlKey("database.credentials.parameters")
    private String mySqlConnectionParameters = "?autoReconnect=true&useSSL=false";

    @YamlComment("MySQL connection pool properties")
    @YamlKey("database.connection_pool.maximum_pool_size")
    private int mySqlConnectionPoolSize = 10;

    @YamlKey("database.connection_pool.minimum_idle")
    private int mySqlConnectionPoolIdle = 10;

    @YamlKey("database.connection_pool.maximum_lifetime")
    private long mySqlConnectionPoolLifetime = 1800000;

    @YamlKey("database.connection_pool.keepalive_time")
    private long mySqlConnectionPoolKeepAlive = 0;

    @YamlKey("database.connection_pool.connection_timeout")
    private long mySqlConnectionPoolTimeout = 5000;

    @YamlKey("database.table_names")
    private Map<String, String> tableNames = TableName.getDefaults();


    // Redis settings
    @YamlComment("Redis connection settings")
    @YamlKey("redis.credentials.host")
    private String redisHost = "localhost";

    @YamlKey("redis.credentials.port")
    private int redisPort = 6379;

    @YamlKey("redis.credentials.password")
    private String redisPassword = "";

    @YamlKey("redis.use_ssl")
    private boolean redisUseSsl = false;


    // Synchronization settings
    @YamlComment("Synchronization settings")
    @YamlKey("synchronization.max_user_data_snapshots")
    private int maxUserDataSnapshots = 5;

    @YamlKey("synchronization.save_on_world_save")
    private boolean saveOnWorldSave = true;

    @YamlKey("synchronization.save_on_death")
    private boolean saveOnDeath = false;

    @YamlKey("synchronization.save_empty_drops_on_death")
    private boolean saveEmptyDropsOnDeath = true;

    @YamlKey("synchronization.compress_data")
    private boolean compressData = true;

    @YamlKey("synchronization.notification_display_slot")
    private NotificationDisplaySlot notificationDisplaySlot = NotificationDisplaySlot.ACTION_BAR;

    @YamlKey("synchronization.synchronise_dead_players_changing_server")
    private boolean synchroniseDeadPlayersChangingServer = true;

    @YamlKey("synchronization.network_latency_milliseconds")
    private int networkLatencyMilliseconds = 500;

    @YamlKey("synchronization.features")
    private Map<String, Boolean> synchronizationFeatures = SynchronizationFeature.getDefaults();

    @YamlKey("synchronization.blacklisted_commands_while_locked")
    private List<String> blacklistedCommandsWhileLocked = new ArrayList<>(List.of("*"));

    @YamlKey("synchronization.event_priorities")
    private Map<String, String> synchronizationEventPriorities = EventType.getDefaults();


    // Zero-args constructor for instantiation via Annotaml
    public Settings() {
    }


    @NotNull
    public String getLanguage() {
        return language;
    }

    public boolean doCheckForUpdates() {
        return checkForUpdates;
    }

    @NotNull
    public String getClusterId() {
        return clusterId;
    }

    public boolean doDebugLogging() {
        return debugLogging;
    }

    @NotNull
    public String getMySqlHost() {
        return mySqlHost;
    }

    public int getMySqlPort() {
        return mySqlPort;
    }

    @NotNull
    public String getMySqlDatabase() {
        return mySqlDatabase;
    }

    @NotNull
    public String getMySqlUsername() {
        return mySqlUsername;
    }

    @NotNull
    public String getMySqlPassword() {
        return mySqlPassword;
    }

    @NotNull
    public String getMySqlConnectionParameters() {
        return mySqlConnectionParameters;
    }

    @NotNull
    public String getTableName(@NotNull TableName tableName) {
        return tableNames.getOrDefault(tableName.name().toLowerCase(Locale.ENGLISH), tableName.defaultName);
    }

    public int getMySqlConnectionPoolSize() {
        return mySqlConnectionPoolSize;
    }

    public int getMySqlConnectionPoolIdle() {
        return mySqlConnectionPoolIdle;
    }

    public long getMySqlConnectionPoolLifetime() {
        return mySqlConnectionPoolLifetime;
    }

    public long getMySqlConnectionPoolKeepAlive() {
        return mySqlConnectionPoolKeepAlive;
    }

    public long getMySqlConnectionPoolTimeout() {
        return mySqlConnectionPoolTimeout;
    }

    @NotNull
    public String getRedisHost() {
        return redisHost;
    }

    public int getRedisPort() {
        return redisPort;
    }

    @NotNull
    public String getRedisPassword() {
        return redisPassword;
    }

    public boolean isRedisUseSsl() {
        return redisUseSsl;
    }

    public int getMaxUserDataSnapshots() {
        return maxUserDataSnapshots;
    }

    public boolean doSaveOnWorldSave() {
        return saveOnWorldSave;
    }

    public boolean doSaveOnDeath() {
        return saveOnDeath;
    }

    public boolean doSaveEmptyDropsOnDeath() {
        return saveEmptyDropsOnDeath;
    }

    public boolean doCompressData() {
        return compressData;
    }

    @NotNull
    public NotificationDisplaySlot getNotificationDisplaySlot() {
        return notificationDisplaySlot;
    }

    public boolean isSynchroniseDeadPlayersChangingServer() {
        return synchroniseDeadPlayersChangingServer;
    }

    public int getNetworkLatencyMilliseconds() {
        return networkLatencyMilliseconds;
    }

    @NotNull
    public Map<String, Boolean> getSynchronizationFeatures() {
        return synchronizationFeatures;
    }

    public boolean getSynchronizationFeature(@NotNull SynchronizationFeature feature) {
        return getSynchronizationFeatures().getOrDefault(feature.name().toLowerCase(Locale.ENGLISH), feature.enabledByDefault);
    }

    @NotNull
    public List<String> getBlacklistedCommandsWhileLocked() {
        return blacklistedCommandsWhileLocked;
    }

    @NotNull
    public EventPriority getEventPriority(@NotNull Settings.EventType eventType) {
        try {
            return EventPriority.valueOf(synchronizationEventPriorities.get(eventType.name().toLowerCase(Locale.ENGLISH)));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return EventPriority.NORMAL;
        }
    }

    /**
     * Represents the names of tables in the database
     */
    public enum TableName {
        USERS("husksync_users"),
        USER_DATA("husksync_user_data");

        private final String defaultName;

        TableName(@NotNull String defaultName) {
            this.defaultName = defaultName;
        }

        @NotNull
        private Map.Entry<String, String> toEntry() {
            return Map.entry(name().toLowerCase(Locale.ENGLISH), defaultName);
        }

        @SuppressWarnings("unchecked")
        @NotNull
        private static Map<String, String> getDefaults() {
            return Map.ofEntries(Arrays.stream(values())
                    .map(TableName::toEntry)
                    .toArray(Map.Entry[]::new));
        }
    }

    /**
     * Determines the slot a system notification should be displayed in
     */
    public enum NotificationDisplaySlot {
        /**
         * Displays the notification in the action bar
         */
        ACTION_BAR,
        /**
         * Displays the notification in the chat
         */
        CHAT,
        /**
         * Displays the notification in an advancement toast
         */
        TOAST,
        /**
         * Does not display the notification
         */
        NONE
    }

    /**
     * Represents enabled synchronisation features
     */
    public enum SynchronizationFeature {
        INVENTORIES(true),
        ENDER_CHESTS(true),
        HEALTH(true),
        MAX_HEALTH(true),
        HUNGER(true),
        EXPERIENCE(true),
        POTION_EFFECTS(true),
        ADVANCEMENTS(true),
        GAME_MODE(true),
        STATISTICS(true),
        PERSISTENT_DATA_CONTAINER(false),
        LOCKED_MAPS(false),
        LOCATION(false);

        private final boolean enabledByDefault;

        SynchronizationFeature(boolean enabledByDefault) {
            this.enabledByDefault = enabledByDefault;
        }

        @NotNull
        private Map.Entry<String, Boolean> toEntry() {
            return Map.entry(name().toLowerCase(Locale.ENGLISH), enabledByDefault);
        }

        @SuppressWarnings("unchecked")
        @NotNull
        private static Map<String, Boolean> getDefaults() {
            return Map.ofEntries(Arrays.stream(values())
                    .map(SynchronizationFeature::toEntry)
                    .toArray(Map.Entry[]::new));
        }
    }

    /**
     * Represents events that HuskSync listens to, with a configurable priority listener
     */
    public enum EventType {
        JOIN_LISTENER(EventPriority.LOWEST),
        QUIT_LISTENER(EventPriority.LOWEST),
        DEATH_LISTENER(EventPriority.NORMAL);

        private final EventPriority defaultPriority;

        EventType(@NotNull EventPriority defaultPriority) {
            this.defaultPriority = defaultPriority;
        }

        @NotNull
        private Map.Entry<String, String> toEntry() {
            return Map.entry(name().toLowerCase(Locale.ENGLISH), defaultPriority.name());
        }


        @SuppressWarnings("unchecked")
        @NotNull
        private static Map<String, String> getDefaults() {
            return Map.ofEntries(Arrays.stream(values())
                    .map(EventType::toEntry)
                    .toArray(Map.Entry[]::new));
        }
    }

    /**
     * Represents priorities for events that HuskSync listens to
     */
    public enum EventPriority {
        /**
         * Listens and processes the event execution last
         */
        HIGHEST,
        /**
         * Listens in between {@link #HIGHEST} and {@link #LOWEST} priority marked
         */
        NORMAL,
        /**
         * Listens and processes the event execution first
         */
        LOWEST
    }

}