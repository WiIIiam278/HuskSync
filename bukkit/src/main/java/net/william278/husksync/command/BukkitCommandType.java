package net.william278.husksync.command;

import net.william278.husksync.BukkitHuskSync;
import org.jetbrains.annotations.NotNull;

/**
 * Commands available on the Bukkit HuskSync implementation
 */
public enum BukkitCommandType {

    HUSKSYNC_COMMAND(new HuskSyncCommand(BukkitHuskSync.getInstance())),
    USERDATA_COMMAND(new UserDataCommand(BukkitHuskSync.getInstance())),
    INVENTORY_COMMAND(new InventoryCommand(BukkitHuskSync.getInstance())),
    ENDER_CHEST_COMMAND(new EnderChestCommand(BukkitHuskSync.getInstance()));

    public final CommandBase commandBase;

    BukkitCommandType(@NotNull CommandBase commandBase) {
        this.commandBase = commandBase;
    }
}
