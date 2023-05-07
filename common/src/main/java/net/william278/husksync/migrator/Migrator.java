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

package net.william278.husksync.migrator;

import net.william278.husksync.HuskSync;
import net.william278.husksync.data.UserData;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * A migrator that migrates data from other data formats to HuskSync's {@link UserData} format
 */
public abstract class Migrator {

    protected final HuskSync plugin;

    protected Migrator(@NotNull HuskSync plugin) {
        this.plugin = plugin;
    }

    /**
     * Start the migrator
     *
     * @return A future that will be completed when the migrator is done
     */
    public abstract CompletableFuture<Boolean> start();

    /**
     * Handle a command that sets migrator configuration parameters
     *
     * @param args The command arguments
     */
    public abstract void handleConfigurationCommand(@NotNull String[] args);

    /**
     * Obfuscates a data string to prevent important data from being logged to console
     *
     * @param dataString The data string to obfuscate
     * @return The data string obfuscated with stars (*)
     */
    protected final String obfuscateDataString(@NotNull String dataString) {
        return (dataString.length() > 1 ? dataString.charAt(0) + "*".repeat(dataString.length() - 1) : "");
    }

    @NotNull
    public abstract String getIdentifier();

    @NotNull
    public abstract String getName();

    @NotNull
    public abstract String getHelpMenu();

}
