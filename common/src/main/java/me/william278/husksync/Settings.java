package me.william278.husksync;

import java.util.ArrayList;

/**
 * Settings class, holds values loaded from the plugin config (either Bukkit or Bungee)
 */
public class Settings {

    /*
     * General settings
     */

    // Whether to do automatic update checks on startup
    public static boolean automaticUpdateChecks;

    // The type of THIS server (Bungee or Bukkit)
    public static ServerType serverType;

    // Redis settings
    public static String redisHost;
    public static int redisPort;
    public static String redisPassword;

    /*
     * Bungee / Proxy server-only settings
     */

    // Messages language
    public static String language;

    // Cluster IDs
    public static ArrayList<SynchronisationCluster> clusters = new ArrayList<>();

    // SQL settings
    public static DataStorageType dataStorageType;

    // MySQL specific settings
    public static String mySQLHost;
    public static String mySQLDatabase;
    public static String mySQLUsername;
    public static String mySQLPassword;
    public static int mySQLPort;
    public static String mySQLParams;

    // Hikari connection pooling settings
    public static int hikariMaximumPoolSize;
    public static int hikariMinimumIdle;
    public static long hikariMaximumLifetime;
    public static long hikariKeepAliveTime;
    public static long hikariConnectionTimeOut;

    /*
     * Bukkit server-only settings
     */

    // Synchronisation options
    public static boolean syncInventories;
    public static boolean syncEnderChests;
    public static boolean syncHealth;
    public static boolean syncHunger;
    public static boolean syncExperience;
    public static boolean syncPotionEffects;
    public static boolean syncStatistics;
    public static boolean syncGameMode;
    public static boolean syncAdvancements;
    public static boolean syncLocation;
    public static boolean syncFlight;

    // Future
    public static boolean useNativeImplementation;

    // This Cluster ID
    public static String cluster;

    /*
     * Enum definitions
     */

    public enum ServerType {
        BUKKIT,
        PROXY,
    }

    public enum DataStorageType {
        MYSQL,
        SQLITE
    }

    /**
     * Defines information for a synchronisation cluster as listed on the proxy
     */
    public record SynchronisationCluster(String clusterId, String databaseName, String playerTableName, String dataTableName) {
    }
}
