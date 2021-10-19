package me.william278.crossserversync;

public class Settings {

    /*
     * General settings
     */

    // The type of THIS server (Bungee or Bukkit)
    public static ServerType serverType;

    // Redis settings
    public static String redisHost;
    public static int redisPort;
    public static String redisPassword;

    /*
     * Bungee / Proxy server-only settings
     */

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
     * Enum definitions
     */

    public enum ServerType {
        BUKKIT,
        BUNGEECORD
    }

    public enum DataStorageType {
        MYSQL,
        SQLITE
    }
}
