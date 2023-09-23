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

package net.william278.husksync.command;

import net.william278.husksync.HuskSync;
import net.william278.husksync.user.CommandUser;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Command extends Node {

    private final String usage;
    private final Map<String, Boolean> additionalPermissions;

    protected Command(@NotNull String name, @NotNull List<String> aliases, @NotNull String usage,
                      @NotNull HuskSync plugin) {
        super(name, aliases, plugin);
        this.usage = usage;
        this.additionalPermissions = new HashMap<>();
    }

    @Override
    public final void onExecuted(@NotNull CommandUser executor, @NotNull String[] args) {
        if (!executor.hasPermission(getPermission())) {
            plugin.getLocales().getLocale("error_no_permission")
                    .ifPresent(executor::sendMessage);
            return;
        }
        plugin.runAsync(() -> this.execute(executor, args));
    }

    public abstract void execute(@NotNull CommandUser executor, @NotNull String[] args);

    @NotNull
    public final String getRawUsage() {
        return usage;
    }

    @NotNull
    public final String getUsage() {
        return "/" + getName() + " " + getRawUsage();
    }

    public final void addAdditionalPermissions(@NotNull Map<String, Boolean> permissions) {
        permissions.forEach((permission, value) -> this.additionalPermissions.put(getPermission(permission), value));
    }

    @NotNull
    public final Map<String, Boolean> getAdditionalPermissions() {
        return additionalPermissions;
    }

    @NotNull
    public String getDescription() {
        return plugin.getLocales().getRawLocale(getName() + "_command_description")
                .orElse(getUsage());
    }

    @NotNull
    public final HuskSync getPlugin() {
        return plugin;
    }

}