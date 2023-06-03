/*
 * This file is part of HuskSync by William278. Do not redistribute!
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  All rights reserved.
 *
 *  This source code is provided as reference to licensed individuals that have purchased the HuskSync
 *  plugin once from any of the official sources it is provided. The availability of this code does
 *  not grant you the rights to modify, re-distribute, compile or redistribute this source code or
 *  "plugin" outside this intended purpose. This license does not cover libraries developed by third
 *  parties that are utilised in the plugin.
 */

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
