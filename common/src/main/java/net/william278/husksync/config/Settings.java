package net.william278.husksync.config;

import net.william278.annotaml.YamlComment;
import net.william278.annotaml.YamlFile;
import net.william278.annotaml.YamlKey;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

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

        versionField = "config_version", versionNumber = 2)
public class Settings {

    // Top-level settings
    public String language = "en-gb";

    @YamlKey("check_for_updates")
    public boolean checkForUpdates = true;

    @YamlKey("cluster_id")
    public String clusterId = "";

    @YamlKey("debug_logging")
    public boolean debugLogging = false;


    // Database settings
    @YamlComment("Database connection settings")
    @YamlKey("database.credentials.host")
    public String mySqlHost = "localhost";

    @YamlKey("database.credentials.port")
    public int mySqlPort = 3306;

    @YamlKey("database.credentials.database")
    public String mySqlDatabase = "HuskSync";

    @YamlKey("database.mysql.credentials.username")
    public String mySqlUsername = "root";

    @YamlKey("database.credentials.password")
    public String mySqlPassword = "pa55w0rd";

    @YamlKey("database.credentials.parameters")
    public String mySqlConnectionParameters = "?autoReconnect=true&useSSL=false";

    @YamlComment("MySQL connection pool properties")
    @YamlKey("database.connection_pool.maximum_pool_size")
    public int mySqlConnectionPoolSize = 10;

    @YamlKey("database.connection_pool.minimum_idle")
    public int mySqlConnectionPoolIdle = 10;

    @YamlKey("database.connection_pool.maximum_lifetime")
    public long mySqlConnectionPoolLifetime = 1800000;

    @YamlKey("database.connection_pool.keepalive_time")
    public long mySqlConnectionPoolKeepAlive = 0;

    @YamlKey("database.connection_pool.connection_timeout")
    public long mySqlConnectionPoolTimeout = 5000;

    @YamlKey("database.table_names")
    public Map<String, String> tableNames = TableName.getDefaults();

    @NotNull
    public String getTableName(@NotNull TableName tableName) {
        return Optional.ofNullable(tableNames.get(tableName.name().toLowerCase()))
                .orElse(tableName.defaultName);
    }


    // Redis settings
    @YamlComment("Redis connection settings")
    @YamlKey("redis.credentials.host")
    public String redisHost = "localhost";

    @YamlKey("redis.credentials.port")
    public int redisPort = 6379;

    @YamlKey("redis.credentials.password")
    public String redisPassword = "";

    @YamlKey("redis.use_ssl")
    public boolean redisUseSsl = false;


    // Synchronization settings
    @YamlComment("Synchronization settings")
    @YamlKey("synchronization.max_user_data_snapshots")
    public int maxUserDataSnapshots = 5;

    @YamlKey("synchronization.save_on_world_save")
    public boolean saveOnWorldSave = true;

    @YamlKey("synchronization.save_on_death")
    public boolean saveOnDeath = false;

    @YamlKey("synchronization.compress_data")
    public boolean compressData = true;

    @YamlKey("synchronization.save_dead_player_inventories")
    public boolean saveDeadPlayerInventories = true;

    @YamlKey("synchronization.network_latency_milliseconds")
    public int networkLatencyMilliseconds = 500;

    @YamlKey("synchronization.features")
    public Map<String, Boolean> synchronizationFeatures = SynchronizationFeature.getDefaults();

    public boolean getSynchronizationFeature(@NotNull SynchronizationFeature feature) {
        return Optional.ofNullable(synchronizationFeatures.get(feature.name().toLowerCase()))
                .orElse(feature.enabledByDefault);
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

        private Map.Entry<String, String> toEntry() {
            return Map.entry(name().toLowerCase(), defaultName);
        }

        @SuppressWarnings("unchecked")
        private static Map<String, String> getDefaults() {
            return Map.ofEntries(Arrays.stream(values())
                    .map(TableName::toEntry)
                    .toArray(Map.Entry[]::new));
        }
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
        LOCATION(false);

        private final boolean enabledByDefault;

        SynchronizationFeature(boolean enabledByDefault) {
            this.enabledByDefault = enabledByDefault;
        }

        private Map.Entry<String, Boolean> toEntry() {
            return Map.entry(name().toLowerCase(), enabledByDefault);
        }

        @SuppressWarnings("unchecked")
        private static Map<String, Boolean> getDefaults() {
            return Map.ofEntries(Arrays.stream(values())
                    .map(SynchronizationFeature::toEntry)
                    .toArray(Map.Entry[]::new));
        }
    }

}