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

import com.google.common.collect.Lists;
import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.william278.husksync.command.PluginCommand;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.data.Identifier;
import net.william278.husksync.database.Database;
import net.william278.husksync.listener.EventListener;
import net.william278.husksync.sync.DataSyncer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Plugin settings, read from config.yml
 */
@SuppressWarnings("FieldMayBeFinal")
@Getter
@Configuration
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Settings {

    protected static final String CONFIG_HEADER = """
            ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
            ┃        HuskSync Config       ┃
            ┃    Developed by William278   ┃
            ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
            ┣╸ Information: https://william278.net/project/husksync
            ┣╸ Config Help: https://william278.net/docs/husksync/config-file/
            ┗╸ Documentation: https://william278.net/docs/husksync""";

    // Top-level settings
    @Comment({"Locale of the default language file to use.", "Docs: https://william278.net/docs/husksync/translations"})
    private String language = Locales.DEFAULT_LOCALE;

    @Comment("Whether to automatically check for plugin updates on startup")
    private boolean checkForUpdates = true;

    @Comment("Specify a common ID for grouping servers running HuskSync. "
             + "Don't modify this unless you know what you're doing!")
    private String clusterId = "";

    @Comment("Enable development debug logging")
    private boolean debugLogging = false;

    @Comment({"Whether to enable the Player Analytics hook.", "Docs: https://william278.net/docs/husksync/plan-hook"})
    private boolean enablePlanHook = true;

    @Comment("Whether to cancel game event packets directly when handling locked players if ProtocolLib or PacketEvents is installed")
    private boolean cancelPackets = true;

    @Comment("Add HuskSync commands to this list to prevent them from being registered (e.g. ['userdata'])")
    @Getter(AccessLevel.NONE)
    private List<String> disabledCommands = Lists.newArrayList();

    // Database settings
    @Comment("Database settings")
    private DatabaseSettings database = new DatabaseSettings();

    @Getter
    @Configuration
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class DatabaseSettings {

        @Comment("Type of database to use (MYSQL, MARIADB, POSTGRES, MONGO)")
        private Database.Type type = Database.Type.MYSQL;

        @Comment("Specify credentials here for your MYSQL, MARIADB, POSTGRES OR MONGO database")
        private DatabaseCredentials credentials = new DatabaseCredentials();

        @Getter
        @Configuration
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class DatabaseCredentials {
            private String host = "localhost";
            private int port = 3306;
            private String database = "HuskSync";
            private String username = "root";
            private String password = "pa55w0rd";
            @Comment("Only change this if you're using MARIADB or POSTGRES")
            private String parameters = String.join("&",
                    "?autoReconnect=true", "useSSL=false",
                    "useUnicode=true", "characterEncoding=UTF-8");
        }

        @Comment("MYSQL, MARIADB, POSTGRES database Hikari connection pool properties. Don't modify this unless you know what you're doing!")
        private PoolSettings connectionPool = new PoolSettings();

        @Getter
        @Configuration
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class PoolSettings {
            private int maximumPoolSize = 10;
            private int minimumIdle = 10;
            private long maximumLifetime = 1800000;
            private long keepaliveTime = 0;
            private long connectionTimeout = 5000;
        }

        @Comment("Advanced MongoDB settings. Don't modify unless you know what you're doing!")
        private MongoSettings mongoSettings = new MongoSettings();

        @Getter
        @Configuration
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class MongoSettings {
            private boolean usingAtlas = false;
            private String parameters = String.join("&",
                    "?retryWrites=true", "w=majority",
                    "authSource=HuskSync");
        }

        @Comment("Names of tables to use on your database. Don't modify this unless you know what you're doing!")
        @Getter(AccessLevel.NONE)
        private Map<String, String> tableNames = Database.TableName.getDefaults();

        @Comment("Whether to run the creation SQL on the database when the server starts. Don't modify this unless you know what you're doing!")
        private boolean createTables = true;

        @NotNull
        public String getTableName(@NotNull Database.TableName tableName) {
            return tableNames.getOrDefault(tableName.name().toLowerCase(Locale.ENGLISH), tableName.getDefaultName());
        }
    }

    // 𝓡𝓮𝓭𝓲𝓼 settings
    @Comment("Redis settings")
    private RedisSettings redis = new RedisSettings();

    @Getter
    @Configuration
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class RedisSettings {

        @Comment("Specify the credentials of your Redis server here. Set \"password\" to '' if you don't have one")
        private RedisCredentials credentials = new RedisCredentials();

        @Getter
        @Configuration
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class RedisCredentials {
            private String host = "localhost";
            private int port = 6379;
            private String password = "";
            private boolean useSsl = false;
        }

        @Comment("Options for if you're using Redis sentinel. Don't modify this unless you know what you're doing!")
        private RedisSentinel sentinel = new RedisSentinel();

        @Getter
        @Configuration
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class RedisSentinel {
            @Comment("The master set name for the Redis sentinel.")
            private String master = "";
            @Comment("List of host:port pairs")
            private List<String> nodes = Lists.newArrayList();
            private String password = "";
        }

    }

    // Synchronization settings
    @Comment("Data syncing settings")
    private SynchronizationSettings synchronization = new SynchronizationSettings();

    @Getter
    @Configuration
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class SynchronizationSettings {

        @Comment({"The data synchronization mode to use (LOCKSTEP or DELAY). LOCKSTEP is recommended for most networks.",
                "Docs: https://william278.net/docs/husksync/sync-modes"})
        private DataSyncer.Mode mode = DataSyncer.Mode.LOCKSTEP;

        @Comment("The number of data snapshot backups that should be kept at once per user")
        private int maxUserDataSnapshots = 16;

        @Comment("Number of hours between new snapshots being saved as backups (Use \"0\" to backup all snapshots)")
        private int snapshotBackupFrequency = 4;

        @Comment({"List of save cause IDs for which a snapshot will be automatically pinned (so it won't be rotated).",
                "Docs: https://william278.net/docs/husksync/data-rotation#save-causes"})
        @Getter(AccessLevel.NONE)
        private List<String> autoPinnedSaveCauses = List.of(
                DataSnapshot.SaveCause.INVENTORY_COMMAND.name(),
                DataSnapshot.SaveCause.ENDERCHEST_COMMAND.name(),
                DataSnapshot.SaveCause.BACKUP_RESTORE.name(),
                DataSnapshot.SaveCause.LEGACY_MIGRATION.name(),
                DataSnapshot.SaveCause.MPDB_MIGRATION.name()
        );

        @Comment("Whether to create a snapshot for users on a world when the server saves that world")
        private boolean saveOnWorldSave = true;

        @Comment("Configuration for how and when to sync player data when they die")
        private SaveOnDeathSettings saveOnDeath = new SaveOnDeathSettings();

        @Getter
        @Configuration
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class SaveOnDeathSettings {
            @Comment("Whether to create a snapshot for users when they die (containing their death drops)")
            private boolean enabled = false;

            @Comment("What items to save in death snapshots? (DROPS or ITEMS_TO_KEEP). "
                     + "Note that ITEMS_TO_KEEP (suggested for keepInventory servers) requires a Paper 1.19.4+ server.")
            private DeathItemsMode itemsToSave = DeathItemsMode.DROPS;

            @Comment("Should a death snapshot still be created even if the items to save on the player's death are empty?")
            private boolean saveEmptyItems = true;

            @Comment("Whether dead players who log out and log in to a different server should have their items saved.")
            private boolean syncDeadPlayersChangingServer = true;

            /**
             * Represents the mode of saving items on death
             */
            public enum DeathItemsMode {
                DROPS,
                ITEMS_TO_KEEP
            }
        }

        @Comment("Whether to use the snappy data compression algorithm. Keep on unless you know what you're doing")
        private boolean compressData = true;

        @Comment("Where to display sync notifications (ACTION_BAR, CHAT or NONE)")
        private Locales.NotificationSlot notificationDisplaySlot = Locales.NotificationSlot.ACTION_BAR;

        @Comment("Persist maps locked in a Cartography Table to let them be viewed on any server")
        private boolean persistLockedMaps = true;

        @Comment("If using the DELAY sync method, how long should this server listen for Redis key data updates before "
                 + "pulling data from the database instead (i.e., if the user did not change servers).")
        private int networkLatencyMilliseconds = 500;

        @Comment({"Which data types to synchronize.", "Docs: https://william278.net/docs/husksync/sync-features"})
        @Getter(AccessLevel.NONE)
        private Map<String, Boolean> features = Identifier.getConfigMap();

        @Comment("Commands which should be blocked before a player has finished syncing (Use * to block all commands)")
        private List<String> blacklistedCommandsWhileLocked = new ArrayList<>(List.of("*"));

        @Comment("Configuration for how to sync attributes")
        private AttributeSettings attributes = new AttributeSettings();

        @Getter
        @Configuration
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class AttributeSettings {

            @Comment({"Which attribute types should be saved as part of attribute syncing. Supports wildcard matching.",
                    "(e.g. ['minecraft:generic.max_health', 'minecraft:generic.*'])"})
            @Getter(AccessLevel.NONE)
            private List<String> syncedAttributes = new ArrayList<>(List.of(
                    "minecraft:generic.max_health", "minecraft:max_health",
                    "minecraft:generic.max_absorption", "minecraft:max_absorption",
                    "minecraft:generic.luck", "minecraft:luck",
                    "minecraft:generic.scale", "minecraft:scale",
                    "minecraft:generic.step_height", "minecraft:step_height",
                    "minecraft:generic.gravity", "minecraft:gravity"
            ));

            @Comment({"Which attribute modifiers should be saved. Supports wildcard matching.",
                    "(e.g. ['minecraft:effect.speed', 'minecraft:effect.*'])"})
            @Getter(AccessLevel.NONE)
            private List<String> ignoredModifiers = new ArrayList<>(List.of(
                    "minecraft:effect.*", "minecraft:creative_mode_*"
            ));

            private boolean matchesWildcard(@NotNull String pat, @NotNull String value) {
                if (!pat.contains(":")) {
                    pat = "minecraft:%s".formatted(pat);
                }
                if (!value.contains(":")) {
                    value = "minecraft:%s".formatted(value);
                }
                return pat.contains("*") ? value.matches(pat.replace("*", ".*")) : pat.equals(value);
            }

            public boolean isIgnoredAttribute(@NotNull String attribute) {
                return syncedAttributes.stream().noneMatch(wildcard -> matchesWildcard(wildcard, attribute));
            }

            public boolean isIgnoredModifier(@NotNull String modifier) {
                return ignoredModifiers.stream().anyMatch(wildcard -> matchesWildcard(wildcard, modifier));
            }

        }

        @Comment("Event priorities for listeners (HIGHEST, NORMAL, LOWEST). Change if you encounter plugin conflicts")
        @Getter(AccessLevel.NONE)
        private Map<String, String> eventPriorities = EventListener.ListenerType.getDefaults();

        public boolean doAutoPin(@NotNull DataSnapshot.SaveCause cause) {
            return autoPinnedSaveCauses.contains(cause.name());
        }

        public boolean isFeatureEnabled(@NotNull Identifier id) {
            return id.isCustom() || features.getOrDefault(id.getKeyValue(), id.isEnabledByDefault());
        }

        @NotNull
        public EventListener.Priority getEventPriority(@NotNull EventListener.ListenerType type) {
            try {
                return EventListener.Priority.valueOf(eventPriorities.get(type.name().toLowerCase(Locale.ENGLISH)));
            } catch (IllegalArgumentException e) {
                return EventListener.Priority.NORMAL;
            }
        }
    }

    public boolean isCommandDisabled(@NotNull PluginCommand command) {
        return disabledCommands.stream().map(c -> c.startsWith("/") ? c.substring(1) : c)
                .anyMatch(c -> c.equalsIgnoreCase(command.getName()) || command.getAliases().contains(c));
    }


}
