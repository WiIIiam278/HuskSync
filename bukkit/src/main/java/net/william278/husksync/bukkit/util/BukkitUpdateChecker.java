package net.william278.husksync.bukkit.util;

import net.william278.husksync.HuskSyncBukkit;
import net.william278.husksync.util.UpdateChecker;

import java.util.logging.Level;

public class BukkitUpdateChecker extends UpdateChecker {

    private static final HuskSyncBukkit plugin = HuskSyncBukkit.getInstance();

    public BukkitUpdateChecker() {
        super(plugin.getDescription().getVersion());
    }

    @Override
    public void log(Level level, String message) {
        plugin.getLogger().log(level, message);
    }
}
