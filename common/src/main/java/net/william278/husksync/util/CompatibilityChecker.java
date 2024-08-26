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

package net.william278.husksync.util;

import de.exlll.configlib.Configuration;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurationStore;
import net.william278.desertwell.util.Version;
import net.william278.husksync.HuskSync;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.Objects;
import java.util.logging.Level;

import static net.william278.husksync.config.ConfigProvider.YAML_CONFIGURATION_PROPERTIES;

public interface CompatibilityChecker {

    String COMPATIBILITY_FILE = "compatibility.yml";

    default void checkCompatibility() throws HuskSync.FailedToLoadException {
        final YamlConfigurationProperties p = YAML_CONFIGURATION_PROPERTIES.build();
        final Version compatible;

        // Load compatibility file
        try (InputStream input = getResource(COMPATIBILITY_FILE)) {
            final CompatibilityConfig compat = new YamlConfigurationStore<>(CompatibilityConfig.class, p).read(input);
            compatible = Objects.requireNonNull(compat.getCompatibleWith());
        } catch (Throwable e) {
            getPlugin().log(Level.WARNING, "Failed to load compatibility config, skipping check.", e);
            return;
        }

        // Check compatibility
        if (compatible.compareTo(getPlugin().getMinecraftVersion()) != 0) {
            throw new HuskSync.FailedToLoadException("""
                    Incompatible Minecraft version. This version of HuskSync is designed for Minecraft %s.
                    Please download the correct version of HuskSync for your server's Minecraft version (%s)."""
                    .formatted(compatible.toString(), getPlugin().getMinecraftVersion().toString()));
        }
    }

    InputStream getResource(@NotNull String name);

    @NotNull
    HuskSync getPlugin();

    @Configuration
    record CompatibilityConfig(@NotNull String minecraftVersion) {

        @NotNull
        public Version getCompatibleWith() {
            return Version.fromString(minecraftVersion);
        }

    }

}
