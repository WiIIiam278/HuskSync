/*
 * This file is part of HuskSync, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.husksync.migrator;

import net.william278.husksync.HuskSync;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * A migrator that migrates data from other data formats to HuskSync's format
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
