package me.william278.husksync.bungeecord.data.sql;

import me.william278.husksync.HuskSyncBungeeCord;
import me.william278.husksync.Settings;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class Database {
    protected HuskSyncBungeeCord plugin;

    public final static String DATA_POOL_NAME = "HuskSyncHikariPool";
    public final static String PLAYER_TABLE_NAME = "husksync_players";
    public final static String DATA_TABLE_NAME = "husksync_data";

    public Database(HuskSyncBungeeCord instance) {
        plugin = instance;
    }

    public abstract Connection getConnection() throws SQLException;

    public boolean isInactive() {
        try {
            return getConnection() == null;
        } catch (SQLException e) {
            return true;
        }
    }

    public abstract void load();

    public abstract void createTables();

    public abstract void close();

    public final int hikariMaximumPoolSize = me.william278.husksync.Settings.hikariMaximumPoolSize;
    public final int hikariMinimumIdle = me.william278.husksync.Settings.hikariMinimumIdle;
    public final long hikariMaximumLifetime = me.william278.husksync.Settings.hikariMaximumLifetime;
    public final long hikariKeepAliveTime = me.william278.husksync.Settings.hikariKeepAliveTime;
    public final long hikariConnectionTimeOut = Settings.hikariConnectionTimeOut;
}
