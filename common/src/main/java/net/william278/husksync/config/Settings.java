package net.william278.husksync.config;

import net.william278.annotaml.YamlComment;
import net.william278.annotaml.YamlFile;
import net.william278.annotaml.YamlKey;
import net.william278.husksync.data.SynchronizationFeature;
import net.william278.husksync.listener.EventListener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
    private Locales.NotificationSlot notificationSlot = Locales.NotificationSlot.ACTION_BAR;

    @YamlKey("synchronization.synchronise_dead_players_changing_server")
    private boolean synchroniseDeadPlayersChangingServer = true;

    @YamlKey("synchronization.network_latency_milliseconds")
    private int networkLatencyMilliseconds = 500;

    @YamlKey("synchronization.features")
    private Map<String, Boolean> synchronizationFeatures = SynchronizationFeature.getDefaults();

    @YamlKey("synchronization.blacklisted_commands_while_locked")
    private List<String> blacklistedCommandsWhileLocked = new ArrayList<>();

    @YamlKey("synchronization.event_priorities")
    private Map<String, String> synchronizationEventPriorities = EventListener.ListenerType.getDefaults();


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
        return tableNames.getOrDefault(tableName.name().toLowerCase(), tableName.defaultName);
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

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean doSaveOnWorldSave() {
        return saveOnWorldSave;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
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
    public Locales.NotificationSlot getNotificationDisplaySlot() {
        return notificationSlot;
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
        return getSynchronizationFeatures().getOrDefault(feature.name().toLowerCase(), feature.isEnabledByDefault());
    }

    @NotNull
    public List<String> getBlacklistedCommandsWhileLocked() {
        return blacklistedCommandsWhileLocked;
    }

    @NotNull
    public EventListener.Priority getEventPriority(@NotNull EventListener.ListenerType listenerType) {
        try {
            return EventListener.Priority.valueOf(synchronizationEventPriorities.get(listenerType.name().toLowerCase()));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return EventListener.Priority.NORMAL;
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
            return Map.entry(name().toLowerCase(), defaultName);
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