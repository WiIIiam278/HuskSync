package me.william278.crossserversync.bungeecord.data.sql;

import me.william278.crossserversync.Settings;
import me.william278.crossserversync.CrossServerSyncBungeeCord;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class Database {
    protected CrossServerSyncBungeeCord plugin;

    public final static String DATA_POOL_NAME = "CrossServerSyncHikariPool";
    public final static String PLAYER_TABLE_NAME = "crossserversync_players";
    public final static String DATA_TABLE_NAME = "crossserversync_data";

    public Database(CrossServerSyncBungeeCord instance) {
        plugin = instance;
    }

    public abstract Connection getConnection() throws SQLException;

    public abstract void load();

    public abstract void backup();

    public abstract void close();

    public final int hikariMaximumPoolSize = Settings.hikariMaximumPoolSize;
    public final int hikariMinimumIdle = Settings.hikariMinimumIdle;
    public final long hikariMaximumLifetime = Settings.hikariMaximumLifetime;
    public final long hikariKeepAliveTime = Settings.hikariKeepAliveTime;
    public final long hikariConnectionTimeOut = Settings.hikariConnectionTimeOut;
}
