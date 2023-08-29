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

package net.william278.husksync;

import net.william278.annotaml.Annotaml;
import net.william278.desertwell.util.ThrowingConsumer;
import net.william278.desertwell.util.UpdateChecker;
import net.william278.desertwell.util.Version;
import net.william278.husksync.config.Locales;
import net.william278.husksync.config.Settings;
import net.william278.husksync.data.DataAdapter;
import net.william278.husksync.database.Database;
import net.william278.husksync.event.EventDispatcher;
import net.william278.husksync.migrator.Migrator;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.redis.RedisManager;
import net.william278.husksync.util.Task;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Level;

/**
 * Abstract implementation of the HuskSync plugin.
 */
public interface HuskSync extends Task.Supplier, EventDispatcher {

    int SPIGOT_RESOURCE_ID = 97144;

    /**
     * Returns a set of online players.
     *
     * @return a set of online players as {@link OnlineUser}
     */
    @NotNull
    Set<OnlineUser> getOnlineUsers();

    /**
     * Returns an online user by UUID if they exist
     *
     * @param uuid the UUID of the user to get
     * @return an online user as {@link OnlineUser}
     */
    @NotNull
    Optional<OnlineUser> getOnlineUser(@NotNull UUID uuid);

    /**
     * Returns the database implementation
     *
     * @return the {@link Database} implementation
     */
    @NotNull
    Database getDatabase();

    /**
     * Returns the redis manager implementation
     *
     * @return the {@link RedisManager} implementation
     */

    @NotNull
    RedisManager getRedisManager();

    /**
     * Returns the data adapter implementation
     *
     * @return the {@link DataAdapter} implementation
     */
    @NotNull
    DataAdapter getDataAdapter();

    /**
     * Returns a list of available data {@link Migrator}s
     *
     * @return a list of {@link Migrator}s
     */
    @NotNull
    List<Migrator> getAvailableMigrators();


    /**
     * Initialize a faucet of the plugin.
     *
     * @param name   the name of the faucet
     * @param runner a runnable for initializing the faucet
     */
    default void initialize(@NotNull String name, @NotNull ThrowingConsumer<HuskSync> runner) {
        log(Level.INFO, "Initializing " + name + "...");
        try {
            runner.accept(this);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize " + name, e);
        }
        log(Level.INFO, "Successfully initialized " + name);
    }

    /**
     * Returns the plugin {@link Settings}
     *
     * @return the {@link Settings}
     */
    @NotNull
    Settings getSettings();

    void setSettings(@NotNull Settings settings);

    /**
     * Returns the plugin {@link Locales}
     *
     * @return the {@link Locales}
     */
    @NotNull
    Locales getLocales();

    void setLocales(@NotNull Locales locales);

    /**
     * Returns if a dependency is loaded
     *
     * @param name the name of the dependency
     * @return {@code true} if the dependency is loaded, {@code false} otherwise
     */
    boolean isDependencyLoaded(@NotNull String name);

    /**
     * Get a resource as an {@link InputStream} from the plugin jar
     *
     * @param name the path to the resource
     * @return the {@link InputStream} of the resource
     */
    InputStream getResource(@NotNull String name);

    /**
     * Returns the plugin data folder
     *
     * @return the plugin data folder as a {@link File}
     */
    @NotNull
    File getDataFolder();

    /**
     * Log a message to the console
     *
     * @param level     the level of the message
     * @param message   the message to log
     * @param throwable a throwable to log
     */
    void log(@NotNull Level level, @NotNull String message, @NotNull Throwable... throwable);

    /**
     * Send a debug message to the console, if debug logging is enabled
     *
     * @param message   the message to log
     * @param throwable a throwable to log
     */
    default void debug(@NotNull String message, @NotNull Throwable... throwable) {
        if (getSettings().doDebugLogging()) {
            log(Level.INFO, String.format("[DEBUG] %s", message), throwable);
        }
    }

    /**
     * Returns the plugin version
     *
     * @return the plugin {@link Version}
     */
    @NotNull
    Version getPluginVersion();

    /**
     * Returns the Minecraft version implementation
     *
     * @return the Minecraft {@link Version}
     */
    @NotNull
    Version getMinecraftVersion();

    /**
     * Reloads the {@link Settings} and {@link Locales} from their respective config files.
     *
     * @return {@code true} if the reload was successful, {@code false} otherwise
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    default boolean loadConfigs() {
        try {
            // Load settings
            setSettings(Annotaml.create(new File(getDataFolder(), "config.yml"), Settings.class).get());

            // Load locales from language preset default
            final Locales languagePresets = Annotaml.create(
                    Locales.class,
                    Objects.requireNonNull(getResource(String.format("locales/%s.yml", getSettings().getLanguage())))
            ).get();
            setLocales(Annotaml.create(new File(
                    getDataFolder(),
                    String.format("messages_%s.yml", getSettings().getLanguage())
            ), languagePresets).get());
            return true;
        } catch (IOException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            log(Level.SEVERE, "Failed to load HuskSync config or messages file", e);
        }
        return false;
    }

    @NotNull
    default UpdateChecker getUpdateChecker() {
        return UpdateChecker.builder()
                .currentVersion(getPluginVersion())
                .endpoint(UpdateChecker.Endpoint.SPIGOT)
                .resource(Integer.toString(SPIGOT_RESOURCE_ID))
                .build();
    }

    default void checkForUpdates() {
        if (getSettings().doCheckForUpdates()) {
            getUpdateChecker().check().thenAccept(checked -> {
                if (!checked.isUpToDate()) {
                    log(Level.WARNING, String.format(
                            "A new version of HuskHomes is available: v%s (running v%s)",
                            checked.getLatestVersion(), getPluginVersion())
                    );
                }
            });
        }
    }

    Set<UUID> getLockedPlayers();

}
