package net.william278.husksync.proxy.data.sql;

import net.william278.husksync.Settings;
import net.william278.husksync.util.Logger;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class Database {

    public String dataPoolName;
    public Settings.SynchronisationCluster cluster;
    public final Logger logger;

    public Database(Settings.SynchronisationCluster cluster, Logger logger) {
        this.cluster = cluster;
        this.dataPoolName = cluster != null ? "HuskSyncHikariPool-" + cluster.clusterId() : "HuskSyncMigratorPool";
        this.logger = logger;
    }

    public abstract Connection getConnection() throws SQLException;

    public boolean isInactive() {
        try (Connection connection = getConnection()) {
            return connection == null || !connection.isValid(0);
        } catch (SQLException e) {
            return true;
        }
    }

    public abstract void load();

    public abstract void createTables();

    public abstract void close();

    public final int hikariMaximumPoolSize = Settings.hikariMaximumPoolSize;
    public final int hikariMinimumIdle = Settings.hikariMinimumIdle;
    public final long hikariMaximumLifetime = Settings.hikariMaximumLifetime;
    public final long hikariKeepAliveTime = Settings.hikariKeepAliveTime;
    public final long hikariConnectionTimeOut = Settings.hikariConnectionTimeOut;
}
