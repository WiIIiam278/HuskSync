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
import net.william278.husksync.adapter.DataAdapter;
import net.william278.husksync.config.Locales;
import net.william278.husksync.config.Settings;
import net.william278.husksync.data.DataContainer;
import net.william278.husksync.data.Serializer;
import net.william278.husksync.database.Database;
import net.william278.husksync.event.EventDispatcher;
import net.william278.husksync.migrator.Migrator;
import net.william278.husksync.player.ConsoleUser;
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

    @NotNull
    DataAdapter getDataAdapter();

    /**
     * Returns the data serializer for the given {@link DataContainer.Type}
     */
    @NotNull
    <T extends DataContainer> Map<DataContainer.Type, Serializer<T>> getSerializers();

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
        } catch (Throwable e) {
            throw new FailedToLoadException("Failed to initialize " + name, e);
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
     * Get the console user
     *
     * @return the {@link ConsoleUser}
     */
    @NotNull
    ConsoleUser getConsole();

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
     * Returns the platform type
     *
     * @return the platform type
     */
    @NotNull
    String getPlatformType();

    /**
     * Reloads the {@link Settings} and {@link Locales} from their respective config files.
     */
    default void loadConfigs() {
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
        } catch (IOException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new FailedToLoadException("Failed to load config or message files", e);
        }
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
                            "A new version of HuskSync is available: v%s (running v%s)",
                            checked.getLatestVersion(), getPluginVersion())
                    );
                }
            });
        }
    }

    @NotNull
    Set<UUID> getLockedPlayers();

    /**
     * An exception indicating the plugin has been accessed before it has been registered.
     */
    static final class FailedToLoadException extends IllegalStateException {

        private static final String FORMAT = """
                HuskSync has failed to load! The plugin will not be enabled and no data will be synchronised.
                Please make sure the plugin has been setup correctly (https://william278.net/docs/husksync/setup):
                                
                1) Make sure you've entered your MySQL or MariaDB database details correctly in config.yml
                2) Make sure your Redis server details are also correct in config.yml
                3) Make sure your config is up-to-date (https://william278.net/docs/husksync/config-files)
                4) Check the error below for more details
                                
                Caused by: %s""";

        FailedToLoadException(@NotNull String message, @NotNull Throwable cause) {
            super(String.format(FORMAT, message), cause);
        }

    }

}
