package net.william278.husksync.config;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.william278.husksync.database.DatabaseType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Settings used for the plugin, as read from the config file
 */
public class Settings {

    /**
     * Map of {@link ConfigOption}s read from the config file
     */
    private final HashMap<ConfigOption, Object> configOptions;

    // Load the settings from the document
    private Settings(@NotNull YamlDocument config) {
        this.configOptions = new HashMap<>();
        Arrays.stream(ConfigOption.values()).forEach(configOption -> configOptions
                .put(configOption, switch (configOption.optionType) {
                    case BOOLEAN -> configOption.getBooleanValue(config);
                    case STRING -> configOption.getStringValue(config);
                    case DOUBLE -> configOption.getDoubleValue(config);
                    case FLOAT -> configOption.getFloatValue(config);
                    case INTEGER -> configOption.getIntValue(config);
                    case STRING_LIST -> configOption.getStringListValue(config);
                }));
    }

    /**
     * Get the value of the specified {@link ConfigOption}
     *
     * @param option the {@link ConfigOption} to check
     * @return the value of the {@link ConfigOption} as a boolean
     * @throws ClassCastException if the option is not a boolean
     */
    public boolean getBooleanValue(@NotNull ConfigOption option) throws ClassCastException {
        return (Boolean) configOptions.get(option);
    }

    /**
     * Get the value of the specified {@link ConfigOption}
     *
     * @param option the {@link ConfigOption} to check
     * @return the value of the {@link ConfigOption} as a string
     * @throws ClassCastException if the option is not a string
     */
    public String getStringValue(@NotNull ConfigOption option) throws ClassCastException {
        return (String) configOptions.get(option);
    }

    /**
     * Get the value of the specified {@link ConfigOption}
     *
     * @param option the {@link ConfigOption} to check
     * @return the value of the {@link ConfigOption} as a double
     * @throws ClassCastException if the option is not a double
     */
    public double getDoubleValue(@NotNull ConfigOption option) throws ClassCastException {
        return (Double) configOptions.get(option);
    }

    /**
     * Get the value of the specified {@link ConfigOption}
     *
     * @param option the {@link ConfigOption} to check
     * @return the value of the {@link ConfigOption} as a float
     * @throws ClassCastException if the option is not a float
     */
    public double getFloatValue(@NotNull ConfigOption option) throws ClassCastException {
        return (Float) configOptions.get(option);
    }

    /**
     * Get the value of the specified {@link ConfigOption}
     *
     * @param option the {@link ConfigOption} to check
     * @return the value of the {@link ConfigOption} as an integer
     * @throws ClassCastException if the option is not an integer
     */
    public int getIntegerValue(@NotNull ConfigOption option) throws ClassCastException {
        return (Integer) configOptions.get(option);
    }

    /**
     * Get the value of the specified {@link ConfigOption}
     *
     * @param option the {@link ConfigOption} to check
     * @return the value of the {@link ConfigOption} as a string {@link List}
     * @throws ClassCastException if the option is not a string list
     */
    @SuppressWarnings("unchecked")
    public List<String> getStringListValue(@NotNull ConfigOption option) throws ClassCastException {
        return (List<String>) configOptions.get(option);
    }


    /**
     * Load the settings from a BoostedYaml {@link YamlDocument} config file
     *
     * @param config The loaded {@link YamlDocument} config.yml file
     * @return the loaded {@link Settings}
     */
    public static Settings load(@NotNull YamlDocument config) {
        return new Settings(config);
    }

    /**
     * Represents an option stored by a path in config.yml
     */
    public enum ConfigOption {
        LANGUAGE("language", OptionType.STRING, "en-gb"),
        CHECK_FOR_UPDATES("check_for_updates", OptionType.BOOLEAN, true),

        CLUSTER_ID("cluster_id", OptionType.STRING, ""),
        DEBUG_LOGGING("debug_logging", OptionType.BOOLEAN, false),

        DATABASE_TYPE("database.type", OptionType.STRING, DatabaseType.MYSQL),
        DATABASE_HOST("database.credentials.host", OptionType.STRING, "localhost"),
        DATABASE_PORT("database.credentials.port", OptionType.INTEGER, 3306),
        DATABASE_NAME("database.credentials.database", OptionType.STRING, "HuskSync"),
        DATABASE_USERNAME("database.credentials.username", OptionType.STRING, "root"),
        DATABASE_PASSWORD("database.credentials.password", OptionType.STRING, "pa55w0rd"),
        DATABASE_CONNECTION_PARAMS("database.credentials.params", OptionType.STRING, "?autoReconnect=true&useSSL=false"),
        DATABASE_CONNECTION_POOL_MAX_SIZE("database.connection_pool.maximum_pool_size", OptionType.INTEGER, 10),
        DATABASE_CONNECTION_POOL_MIN_IDLE("database.connection_pool.minimum_idle", OptionType.INTEGER, 10),
        DATABASE_CONNECTION_POOL_MAX_LIFETIME("database.connection_pool.maximum_lifetime", OptionType.INTEGER, 1800000),
        DATABASE_CONNECTION_POOL_KEEPALIVE("database.connection_pool.keepalive_time", OptionType.INTEGER, 0),
        DATABASE_CONNECTION_POOL_TIMEOUT("database.connection_pool.connection_timeout", OptionType.INTEGER, 5000),
        DATABASE_USERS_TABLE_NAME("database.table_names.users_table", OptionType.STRING, "husksync_users"),
        DATABASE_USER_DATA_TABLE_NAME("database.table_names.user_data_table", OptionType.STRING, "husksync_user_data"),

        DATABASE_MONGO_URI("database.mongo.uri", OptionType.STRING, ""),
        DATABASE_MONGO_DATABASE("database.mongo.database", OptionType.STRING, "HuskSync"),
        DATABASE_MONGO_USERNAME("database.mongo.username", OptionType.STRING, "root"),
        DATABASE_MONGO_PASSWORD("database.mongo.password", OptionType.STRING, "pa55w0rd"),

        REDIS_HOST("redis.credentials.host", OptionType.STRING, "localhost"),
        REDIS_PORT("redis.credentials.port", OptionType.INTEGER, 6379),
        REDIS_PASSWORD("redis.credentials.password", OptionType.STRING, ""),
        REDIS_USE_SSL("redis.use_ssl", OptionType.BOOLEAN, false),

        SYNCHRONIZATION_MAX_USER_DATA_SNAPSHOTS("synchronization.max_user_data_snapshots", OptionType.INTEGER, 5),
        SYNCHRONIZATION_SAVE_ON_WORLD_SAVE("synchronization.save_on_world_save", OptionType.BOOLEAN, true),
        SYNCHRONIZATION_COMPRESS_DATA("synchronization.compress_data", OptionType.BOOLEAN, true),
        SYNCHRONIZATION_NETWORK_LATENCY_MILLISECONDS("synchronization.network_latency_milliseconds", OptionType.INTEGER, 500),
        SYNCHRONIZATION_SYNC_INVENTORIES("synchronization.features.inventories", OptionType.BOOLEAN, true),
        SYNCHRONIZATION_SYNC_ENDER_CHESTS("synchronization.features.ender_chests", OptionType.BOOLEAN, true),
        SYNCHRONIZATION_SYNC_HEALTH("synchronization.features.health", OptionType.BOOLEAN, true),
        SYNCHRONIZATION_SYNC_MAX_HEALTH("synchronization.features.max_health", OptionType.BOOLEAN, true),
        SYNCHRONIZATION_SYNC_HUNGER("synchronization.features.hunger", OptionType.BOOLEAN, true),
        SYNCHRONIZATION_SYNC_EXPERIENCE("synchronization.features.experience", OptionType.BOOLEAN, true),
        SYNCHRONIZATION_SYNC_POTION_EFFECTS("synchronization.features.potion_effects", OptionType.BOOLEAN, true),
        SYNCHRONIZATION_SYNC_ADVANCEMENTS("synchronization.features.advancements", OptionType.BOOLEAN, true),
        SYNCHRONIZATION_SYNC_GAME_MODE("synchronization.features.game_mode", OptionType.BOOLEAN, true),
        SYNCHRONIZATION_SYNC_STATISTICS("synchronization.features.statistics", OptionType.BOOLEAN, true),
        SYNCHRONIZATION_SYNC_PERSISTENT_DATA_CONTAINER("synchronization.features.persistent_data_container", OptionType.BOOLEAN, true),
        SYNCHRONIZATION_SYNC_LOCATION("synchronization.features.location", OptionType.BOOLEAN, true);

        /**
         * The path in the config.yml file to the value
         */
        @NotNull
        public final String configPath;

        /**
         * The {@link OptionType} of this option
         */
        @NotNull
        public final OptionType optionType;

        /**
         * The default value of this option if not set in config
         */
        @Nullable
        private final Object defaultValue;

        ConfigOption(@NotNull String configPath, @NotNull OptionType optionType, @Nullable Object defaultValue) {
            this.configPath = configPath;
            this.optionType = optionType;
            this.defaultValue = defaultValue;
        }

        ConfigOption(@NotNull String configPath, @NotNull OptionType optionType) {
            this.configPath = configPath;
            this.optionType = optionType;
            this.defaultValue = null;
        }

        /**
         * Get the value at the path specified (or return default if set), as a string
         *
         * @param config The {@link YamlDocument} config file
         * @return the value defined in the config, as a string
         */
        public String getStringValue(@NotNull YamlDocument config) {
            return defaultValue != null
                    ? config.getString(configPath, (String) defaultValue)
                    : config.getString(configPath);
        }

        /**
         * Get the value at the path specified (or return default if set), as a boolean
         *
         * @param config The {@link YamlDocument} config file
         * @return the value defined in the config, as a boolean
         */
        public boolean getBooleanValue(@NotNull YamlDocument config) {
            return defaultValue != null
                    ? config.getBoolean(configPath, (Boolean) defaultValue)
                    : config.getBoolean(configPath);
        }

        /**
         * Get the value at the path specified (or return default if set), as a double
         *
         * @param config The {@link YamlDocument} config file
         * @return the value defined in the config, as a double
         */
        public double getDoubleValue(@NotNull YamlDocument config) {
            return defaultValue != null
                    ? config.getDouble(configPath, (Double) defaultValue)
                    : config.getDouble(configPath);
        }

        /**
         * Get the value at the path specified (or return default if set), as a float
         *
         * @param config The {@link YamlDocument} config file
         * @return the value defined in the config, as a float
         */
        public float getFloatValue(@NotNull YamlDocument config) {
            return defaultValue != null
                    ? config.getFloat(configPath, (Float) defaultValue)
                    : config.getFloat(configPath);
        }

        /**
         * Get the value at the path specified (or return default if set), as an int
         *
         * @param config The {@link YamlDocument} config file
         * @return the value defined in the config, as an int
         */
        public int getIntValue(@NotNull YamlDocument config) {
            return defaultValue != null
                    ? config.getInt(configPath, (Integer) defaultValue)
                    : config.getInt(configPath);
        }

        /**
         * Get the value at the path specified (or return default if set), as a string {@link List}
         *
         * @param config The {@link YamlDocument} config file
         * @return the value defined in the config, as a string {@link List}
         */
        public List<String> getStringListValue(@NotNull YamlDocument config) {
            return config.getStringList(configPath, new ArrayList<>());
        }

        /**
         * Represents the type of the object
         */
        public enum OptionType {
            BOOLEAN,
            STRING,
            DOUBLE,
            FLOAT,
            INTEGER,
            STRING_LIST
        }
    }

}
