package me.william278.husksync.bungeecord.util;

import me.william278.husksync.HuskSyncBungeeCord;
import me.william278.husksync.util.UpdateChecker;

import java.util.logging.Level;

public class BungeeUpdateChecker extends UpdateChecker {

    private static final HuskSyncBungeeCord plugin = HuskSyncBungeeCord.getInstance();

    public BungeeUpdateChecker(String versionToCheck) {
        super(versionToCheck);
    }

    @Override
    public void log(Level level, String message) {
        plugin.getLogger().log(level, message);
    }
}
