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

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.AudienceProvider;
import net.william278.desertwell.util.ThrowingConsumer;
import net.william278.desertwell.util.UpdateChecker;
import net.william278.desertwell.util.Version;
import net.william278.husksync.adapter.DataAdapter;
import net.william278.husksync.config.ConfigProvider;
import net.william278.husksync.data.Data;
import net.william278.husksync.data.Identifier;
import net.william278.husksync.data.SerializerRegistry;
import net.william278.husksync.database.Database;
import net.william278.husksync.event.EventDispatcher;
import net.william278.husksync.migrator.Migrator;
import net.william278.husksync.redis.RedisManager;
import net.william278.husksync.sync.DataSyncer;
import net.william278.husksync.user.ConsoleUser;
import net.william278.husksync.user.OnlineUser;
import net.william278.husksync.util.CompatibilityChecker;
import net.william278.husksync.util.LegacyConverter;
import net.william278.husksync.util.Task;
import net.william278.uniform.Uniform;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;

/**
 * Abstract implementation of the HuskSync plugin.
 */
public interface HuskSync extends Task.Supplier, EventDispatcher, ConfigProvider, SerializerRegistry,
        CompatibilityChecker {

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
     * Returns the implementing adapter for serializing data
     *
     * @return the {@link DataAdapter}
     */
    @NotNull
    DataAdapter getDataAdapter();

    /**
     * Returns the data syncer implementation
     *
     * @return the {@link DataSyncer} implementation
     */
    @NotNull
    DataSyncer getDataSyncer();

    /**
     * Set the data syncer implementation
     *
     * @param dataSyncer the {@link DataSyncer} implementation
     */
    void setDataSyncer(@NotNull DataSyncer dataSyncer);

    /**
     * Get the uniform command provider
     *
     * @return the command provider
     */
    @NotNull
    Uniform getUniform();

    /**
     * Returns a list of available data {@link Migrator}s
     *
     * @return a list of {@link Migrator}s
     */
    @NotNull
    List<Migrator> getAvailableMigrators();

    @NotNull
    Map<UUID, Map<Identifier, Data>> getPlayerCustomDataStore();

    @NotNull
    default Map<Identifier, Data> getPlayerCustomDataStore(@NotNull OnlineUser user) {
        if (getPlayerCustomDataStore().containsKey(user.getUuid())) {
            return getPlayerCustomDataStore().get(user.getUuid());
        }
        final Map<Identifier, Data> data = new HashMap<>();
        getPlayerCustomDataStore().put(user.getUuid(), data);
        return data;
    }

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
        if (getSettings().isDebugLogging()) {
            log(Level.INFO, getDebugString(message), throwable);
        }
    }

    // Get the debug log message format
    @NotNull
    private String getDebugString(@NotNull String message) {
        return String.format("[DEBUG] [%s] %s", new SimpleDateFormat("mm:ss.SSS").format(new Date()), message);
    }

    /**
     * Get the {@link AudienceProvider} instance
     *
     * @return the {@link AudienceProvider} instance
     * @since 1.0
     */
    @NotNull
    AudienceProvider getAudiences();

    /**
     * Get the {@link Audience} instance for the given {@link OnlineUser}
     *
     * @param user the {@link OnlineUser} to get the {@link Audience} for
     * @return the {@link Audience} instance
     */
    @NotNull
    default Audience getAudience(@NotNull UUID user) {
        return getAudiences().player(user);
    }

    /**
     * Get the {@link ConsoleUser} instance
     *
     * @return the {@link ConsoleUser} instance
     * @since 1.0
     */
    @NotNull
    default ConsoleUser getConsole() {
        return new ConsoleUser(getAudiences());
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
     * Returns the data version for a Minecraft version
     *
     * @param minecraftVersion the Minecraft version
     * @return the data version int
     */
    int getDataVersion(@NotNull Version minecraftVersion);

    /**
     * Returns the platform type
     *
     * @return the platform type
     */
    @NotNull
    String getPlatformType();

    /**
     * Returns the server software version
     *
     * @return the server software version string
     */
    @NotNull
    String getServerVersion();

    /**
     * Returns the legacy data converter if it exists
     *
     * @return the {@link LegacyConverter}
     */
    Optional<LegacyConverter> getLegacyConverter();

    @NotNull
    default UpdateChecker getUpdateChecker() {
        return UpdateChecker.builder()
                .currentVersion(getPluginVersion())
                .endpoint(UpdateChecker.Endpoint.SPIGOT)
                .resource(Integer.toString(SPIGOT_RESOURCE_ID))
                .build();
    }

    default void checkForUpdates() {
        if (getSettings().isCheckForUpdates()) {
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

    /**
     * Get the set of UUIDs of "locked players", for which events will be canceled.
     * </p>
     * Players are locked while their items are being set (on join) or saved (on quit)
     */
    @NotNull
    Set<UUID> getLockedPlayers();

    default boolean isLocked(@NotNull UUID uuid) {
        return getLockedPlayers().contains(uuid);
    }

    default void lockPlayer(@NotNull UUID uuid) {
        getLockedPlayers().add(uuid);
    }

    default void unlockPlayer(@NotNull UUID uuid) {
        getLockedPlayers().remove(uuid);
    }

    @NotNull
    Gson getGson();

    boolean isDisabling();

    @NotNull
    default Gson createGson() {
        return Converters.registerOffsetDateTime(new GsonBuilder()).create();
    }

    /**
     * An exception indicating the plugin has been accessed before it has been registered.
     */
    final class FailedToLoadException extends IllegalStateException {

        private static final String FORMAT = """
                HuskSync has failed to load! The plugin will not be enabled and no data will be synchronized.
                Please make sure the plugin has been setup correctly (https://william278.net/docs/husksync/setup):
                                
                1) Make sure you've entered your MySQL, MariaDB or MongoDB database details correctly in config.yml
                2) Make sure your Redis server details are also correct in config.yml
                3) Make sure your config is up-to-date (https://william278.net/docs/husksync/config-file)
                4) Check the error below for more details
                                
                Caused by: %s""";

        public FailedToLoadException(@NotNull String message) {
            super(String.format(FORMAT, message));
        }

        public FailedToLoadException(@NotNull String message, @NotNull Throwable cause) {
            super(String.format(FORMAT, message), cause);
        }

    }

}
