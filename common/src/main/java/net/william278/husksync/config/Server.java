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

package net.william278.husksync.config;

import net.william278.annotaml.Annotaml;
import net.william278.annotaml.YamlFile;
import net.william278.annotaml.YamlKey;
import net.william278.husksync.HuskSync;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * Represents a server on a proxied network.
 */
@YamlFile(header = """
        ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
        ┃   HuskSync Server ID config  ┃
        ┃    Developed by William278   ┃
        ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
        ┣╸ This file should contain the ID of this server as defined in your proxy config.
        ┗╸ If you join it using /server alpha, then set it to 'alpha' (case-sensitive)""")
public class Server {

    @YamlKey("name")
    private String serverName;

    private Server(@NotNull String serverName) {
        this.serverName = serverName;
    }

    @SuppressWarnings("unused")
    private Server() {
    }

    @NotNull
    public static Server getDefault(@NotNull HuskSync plugin) {
        return new Server(getDefaultServerName(plugin));
    }

    /**
     * Find a sensible default name for the server name property
     */
    @NotNull
    private static String getDefaultServerName(@NotNull HuskSync plugin) {
        try {
            // Fetch server default from supported plugins if present
            for (String s : List.of("HuskHomes", "HuskTowns")) {
                final File serverFile = Path.of(plugin.getDataFolder().getParent(), s, "server.yml").toFile();
                if (serverFile.exists()) {
                    return Annotaml.create(serverFile, Server.class).get().getName();
                }
            }

            // Fetch server default from user dir name
            final Path serverDirectory = Path.of(System.getProperty("user.dir"));
            return serverDirectory.getFileName().toString().trim();
        } catch (Throwable e) {
            return "server";
        }
    }

    @Override
    public boolean equals(@NotNull Object other) {
        // If the name of this server matches another, the servers are the same.
        if (other instanceof Server server) {
            return server.getName().equalsIgnoreCase(this.getName());
        }
        return super.equals(other);
    }

    /**
     * Proxy-defined name of this server.
     */
    @NotNull
    public String getName() {
        return serverName;
    }

}