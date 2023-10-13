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

package net.william278.husksync.config;

import net.william278.annotaml.YamlComment;
import net.william278.annotaml.YamlFile;
import net.william278.annotaml.YamlKey;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.data.Identifier;
import net.william278.husksync.database.Database;
import net.william278.husksync.listener.EventListener;
import net.william278.husksync.sync.DataSyncer;
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
        ┣╸ Config Help: https://william278.net/docs/husksync/config-file/
        ┗╸ Documentation: https://william278.net/docs/husksync""")
public class Settings {

    // Top-level settings
    @YamlComment("Locale of the default language file to use. Docs: https://william278.net/docs/husksync/translations")
    @YamlKey("language")
    private String language = "en-gb";

    @YamlComment("Whether to automatically check for plugin updates on startup")
    @YamlKey("check_for_updates")
    private boolean checkForUpdates = true;

    @YamlComment("Specify a common ID for grouping servers running HuskSync. "
            + "Don't modify this unless you know what you're doing!")
    @YamlKey("cluster_id")
    private String clusterId = "";

    @YamlComment("Enable development debug logging")
    @YamlKey("debug_logging")
    private boolean debugLogging = false;

    @YamlComment("Whether to provide modern, rich TAB suggestions for commands (if available)")
    @YamlKey("brigadier_tab_completion")
    private boolean brigadierTabCompletion = false;

    @YamlComment("Whether to enable the Player Analytics hook. Docs: https://william278.net/docs/husksync/plan-hook")
    @YamlKey("enable_plan_hook")
    private boolean enablePlanHook = true;


    // Database settings
    @YamlComment("Type of database to use (MYSQL, MARIADB)")
    @YamlKey("database.type")
    private Database.Type databaseType = Database.Type.MYSQL;

    @YamlComment("Specify credentials here for your MYSQL or MARIADB database")
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
    private String mySqlConnectionParameters = "?autoReconnect=true"
            + "&useSSL=false"
            + "&useUnicode=true"
            + "&characterEncoding=UTF-8";

    @YamlComment("MYSQL / MARIADB database Hikari connection pool properties. "
            + "Don't modify this unless you know what you're doing!")
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

    @YamlComment("Names of tables to use on your database. Don't modify this unless you know what you're doing!")
    @YamlKey("database.table_names")
    private Map<String, String> tableNames = TableName.getDefaults();


    // Redis settings
    @YamlComment("Specify the credentials of your Redis database here. Set \"password\" to '' if you don't have one")
    @YamlKey("redis.credentials.host")
    private String redisHost = "localhost";

    @YamlKey("redis.credentials.port")
    private int redisPort = 6379;

    @YamlKey("redis.credentials.password")
    private String redisPassword = "";

    @YamlKey("redis.use_ssl")
    private boolean redisUseSsl = false;


    // Synchronization settings
    @YamlComment("The mode of data synchronization to use (DELAY or LOCKSTEP). DELAY should be fine for most networks."
            + " Docs: https://william278.net/docs/husksync/sync-modes")
    @YamlKey("synchronization.mode")
    private DataSyncer.Mode syncMode = DataSyncer.Mode.DELAY;

    @YamlComment("The number of data snapshot backups that should be kept at once per user")
    @YamlKey("synchronization.max_user_data_snapshots")
    private int maxUserDataSnapshots = 16;

    @YamlComment("Number of hours between new snapshots being saved as backups (Use \"0\" to backup all snapshots)")
    @YamlKey("synchronization.snapshot_backup_frequency")
    private int snapshotBackupFrequency = 4;

    @YamlComment("List of save cause IDs for which a snapshot will be automatically pinned (so it won't be rotated)."
            + " Docs: https://william278.net/docs/husksync/data-rotation#save-causes")
    @YamlKey("synchronization.auto_pinned_save_causes")
    private List<String> autoPinnedSaveCauses = List.of(
            DataSnapshot.SaveCause.INVENTORY_COMMAND.name(),
            DataSnapshot.SaveCause.ENDERCHEST_COMMAND.name(),
            DataSnapshot.SaveCause.BACKUP_RESTORE.name(),
            DataSnapshot.SaveCause.LEGACY_MIGRATION.name(),
            DataSnapshot.SaveCause.MPDB_MIGRATION.name()
    );

    @YamlComment("Whether to create a snapshot for users on a world when the server saves that world")
    @YamlKey("synchronization.save_on_world_save")
    private boolean saveOnWorldSave = true;

    @YamlComment("Whether to create a snapshot for users when they die (containing their death drops)")
    @YamlKey("synchronization.save_on_death.enabled")
    private boolean saveOnDeath = false;

    @YamlComment("What items to save in death snapshots? (DROPS or ITEMS_TO_KEEP). "
            + " Note that ITEMS_TO_KEEP (suggested for keepInventory servers) requires a Paper 1.19.4+ server.")
    @YamlKey("synchronization.save_on_death.items_to_save")
    private DeathItemsMode deathItemsMode = DeathItemsMode.DROPS;

    @YamlComment("Should a death snapshot still be created even if the items to save on the player's death are empty?")
    @YamlKey("synchronization.save_on_death.save_empty_items")
    private boolean saveEmptyDeathItems = true;

    @YamlComment("Whether dead players who log out and log in to a different server should have their items saved.")
    @YamlKey("synchronization.save_on_death.sync_dead_players_changing_server")
    private boolean synchronizeDeadPlayersChangingServer = true;

    @YamlComment("Whether to use the snappy data compression algorithm. Keep on unless you know what you're doing")
    @YamlKey("synchronization.compress_data")
    private boolean compressData = true;

    @YamlComment("Where to display sync notifications (ACTION_BAR, CHAT, TOAST or NONE)")
    @YamlKey("synchronization.notification_display_slot")
    private Locales.NotificationSlot notificationSlot = Locales.NotificationSlot.ACTION_BAR;

    @YamlComment("(Experimental) Persist Cartography Table locked maps to let them be viewed on any server")
    @YamlKey("synchronization.persist_locked_maps")
    private boolean persistLockedMaps = true;

    @YamlComment("Whether to synchronize player max health (requires health syncing to be enabled)")
    @YamlKey("synchronization.synchronize_max_health")
    private boolean synchronizeMaxHealth = true;

    @YamlComment("If using the DELAY sync method, how long should this server listen for Redis key data updates before "
            + "pulling data from the database instead (i.e., if the user did not change servers).")
    @YamlKey("synchronization.network_latency_milliseconds")
    private int networkLatencyMilliseconds = 500;

    @YamlComment("Which data types to synchronize (Docs: https://william278.net/docs/husksync/sync-features)")
    @YamlKey("synchronization.features")
    private Map<String, Boolean> synchronizationFeatures = Identifier.getConfigMap();

    @YamlComment("Commands which should be blocked before a player has finished syncing (Use * to block all commands)")
    @YamlKey("synchronization.blacklisted_commands_while_locked")
    private List<String> blacklistedCommandsWhileLocked = new ArrayList<>(List.of("*"));

    @YamlComment("Event priorities for listeners (HIGHEST, NORMAL, LOWEST). Change if you encounter plugin conflicts")
    @YamlKey("synchronization.event_priorities")
    private Map<String, String> syncEventPriorities = EventListener.ListenerType.getDefaults();


    // Zero-args constructor for instantiation via Annotaml
    @SuppressWarnings("unused")
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

    public boolean doBrigadierTabCompletion() {
        return brigadierTabCompletion;
    }

    public boolean usePlanHook() {
        return enablePlanHook;
    }

    @NotNull
    public Database.Type getDatabaseType() {
        return databaseType;
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

    public boolean redisUseSsl() {
        return redisUseSsl;
    }

    @NotNull
    public DataSyncer.Mode getSyncMode() {
        return syncMode;
    }

    public int getMaxUserDataSnapshots() {
        return maxUserDataSnapshots;
    }

    public int getBackupFrequency() {
        return snapshotBackupFrequency;
    }

    public boolean doSaveOnWorldSave() {
        return saveOnWorldSave;
    }

    public boolean doSaveOnDeath() {
        return saveOnDeath;
    }

    @NotNull
    public DeathItemsMode getDeathItemsMode() {
        return deathItemsMode;
    }

    public boolean doSaveEmptyDeathItems() {
        return saveEmptyDeathItems;
    }

    public boolean doCompressData() {
        return compressData;
    }

    public boolean doAutoPin(@NotNull DataSnapshot.SaveCause cause) {
        return autoPinnedSaveCauses.contains(cause.name());
    }

    @NotNull
    public Locales.NotificationSlot getNotificationDisplaySlot() {
        return notificationSlot;
    }

    public boolean doPersistLockedMaps() {
        return persistLockedMaps;
    }

    public boolean doSynchronizeDeadPlayersChangingServer() {
        return synchronizeDeadPlayersChangingServer;
    }

    public boolean doSynchronizeMaxHealth() {
        return synchronizeMaxHealth;
    }

    public int getNetworkLatencyMilliseconds() {
        return networkLatencyMilliseconds;
    }

    @NotNull
    public Map<String, Boolean> getSynchronizationFeatures() {
        return synchronizationFeatures;
    }

    public boolean isSyncFeatureEnabled(@NotNull Identifier id) {
        return id.isCustom() || getSynchronizationFeatures().getOrDefault(id.getKeyValue(), id.isEnabledByDefault());
    }

    @NotNull
    public List<String> getBlacklistedCommandsWhileLocked() {
        return blacklistedCommandsWhileLocked;
    }

    @NotNull
    public EventListener.Priority getEventPriority(@NotNull EventListener.ListenerType type) {
        try {
            return EventListener.Priority.valueOf(syncEventPriorities.get(type.name().toLowerCase(Locale.ENGLISH)));
        } catch (IllegalArgumentException e) {
            return EventListener.Priority.NORMAL;
        }
    }

    /**
     * Represents the mode of saving items on death
     */
    public enum DeathItemsMode {
        DROPS,
        ITEMS_TO_KEEP
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

}
