package net.william278.husksync.command;

import net.william278.husksync.BukkitHuskSync;
import org.jetbrains.annotations.NotNull;

/**
 * Commands available on the Bukkit HuskSync implementation
 */
public enum BukkitCommandType {
    HUSKSYNC_COMMAND(new HuskSyncCommand(BukkitHuskSync.getInstance())),
    HUSKSYNC_INVSEE(new InvseeCommand(BukkitHuskSync.getInstance())),
    HUSKSYNC_ECHEST(new EchestCommand(BukkitHuskSync.getInstance()));

    public final CommandBase commandBase;

    BukkitCommandType(@NotNull CommandBase commandBase) {
        this.commandBase = commandBase;
    }
}
