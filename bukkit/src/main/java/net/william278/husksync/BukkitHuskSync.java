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

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.william278.annotaml.Annotaml;
import net.william278.desertwell.util.Version;
import net.william278.husksync.command.BukkitCommand;
import net.william278.husksync.command.BukkitCommandType;
import net.william278.husksync.command.Permission;
import net.william278.husksync.config.Locales;
import net.william278.husksync.config.Settings;
import net.william278.husksync.data.CompressedDataAdapter;
import net.william278.husksync.data.DataAdapter;
import net.william278.husksync.data.JsonDataAdapter;
import net.william278.husksync.database.Database;
import net.william278.husksync.database.MySqlDatabase;
import net.william278.husksync.event.BukkitEventCannon;
import net.william278.husksync.event.EventCannon;
import net.william278.husksync.hook.PlanHook;
import net.william278.husksync.listener.BukkitEventListener;
import net.william278.husksync.listener.EventListener;
import net.william278.husksync.migrator.LegacyMigrator;
import net.william278.husksync.migrator.Migrator;
import net.william278.husksync.migrator.MpdbMigrator;
import net.william278.husksync.player.BukkitPlayer;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.redis.RedisManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class BukkitHuskSync extends JavaPlugin implements HuskSync {

    /**
     * Metrics ID for <a href="https://bstats.org/plugin/bukkit/HuskSync%20-%20Bukkit/13140">HuskSync on Bukkit</a>.
     */
    private static final int METRICS_ID = 13140;
    private Database database;
    private RedisManager redisManager;
    private EventListener eventListener;
    private DataAdapter dataAdapter;
    private EventCannon eventCannon;
    private Settings settings;
    private Locales locales;
    private List<Migrator> availableMigrators;

    private BukkitAudiences audiences;
    private static BukkitHuskSync instance;

    /**
     * (<b>Internal use only)</b> Returns the instance of the implementing Bukkit plugin
     *
     * @return the instance of the Bukkit plugin
     */
    public static BukkitHuskSync getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        // Initialize HuskSync
        final AtomicBoolean initialized = new AtomicBoolean(true);
        try {
            // Create adventure audience
            this.audiences = BukkitAudiences.create(this);

            // Load settings and locales
            log(Level.INFO, "Loading plugin configuration settings & locales...");
            initialized.set(reload().join());
            if (initialized.get()) {
                log(Level.INFO, "Successfully loaded plugin configuration settings & locales");
            } else {
                throw new HuskSyncInitializationException("Failed to load plugin configuration settings and/or locales");
            }

            // Prepare data adapter
            if (settings.doCompressData()) {
                dataAdapter = new CompressedDataAdapter();
            } else {
                dataAdapter = new JsonDataAdapter();
            }

            // Prepare event cannon
            eventCannon = new BukkitEventCannon();

            // Prepare migrators
            availableMigrators = new ArrayList<>();
            availableMigrators.add(new LegacyMigrator(this));
            final Plugin mySqlPlayerDataBridge = Bukkit.getPluginManager().getPlugin("MySqlPlayerDataBridge");
            if (mySqlPlayerDataBridge != null) {
                availableMigrators.add(new MpdbMigrator(this, mySqlPlayerDataBridge));
            }

            // Prepare database connection
            this.database = new MySqlDatabase(this);
            log(Level.INFO, String.format("Attempting to establish connection to the %s database...",
                    settings.getDatabaseType().getDisplayName()));
            this.database.initialize();
            if (initialized.get()) {
                log(Level.INFO, String.format("Successfully established a connection to the %s database",
                        settings.getDatabaseType().getDisplayName()));
            } else {
                throw new HuskSyncInitializationException("Failed to establish a connection to the database. " +
                        "Please check the supplied database credentials in the config file");
            }

            // Prepare redis connection
            this.redisManager = new RedisManager(this);
            log(Level.INFO, "Attempting to establish connection to the Redis server...");
            initialized.set(this.redisManager.initialize());
            if (initialized.get()) {
                log(Level.INFO, "Successfully established a connection to the Redis server");
            } else {
                throw new HuskSyncInitializationException("Failed to establish a connection to the Redis server. " +
                        "Please check the supplied Redis credentials in the config file");
            }

            // Register events
            log(Level.INFO, "Registering events...");
            this.eventListener = new BukkitEventListener(this);
            log(Level.INFO, "Successfully registered events listener");

            // Register permissions
            log(Level.INFO, "Registering permissions & commands...");
            Arrays.stream(Permission.values()).forEach(permission -> getServer().getPluginManager()
                    .addPermission(new org.bukkit.permissions.Permission(permission.node, switch (permission.defaultAccess) {
                        case EVERYONE -> PermissionDefault.TRUE;
                        case NOBODY -> PermissionDefault.FALSE;
                        case OPERATORS -> PermissionDefault.OP;
                    })));

            // Register commands
            for (final BukkitCommandType bukkitCommandType : BukkitCommandType.values()) {
                final PluginCommand pluginCommand = getCommand(bukkitCommandType.commandBase.command);
                if (pluginCommand != null) {
                    new BukkitCommand(bukkitCommandType.commandBase, this).register(pluginCommand);
                }
            }
            log(Level.INFO, "Successfully registered permissions & commands");

            // Hook into plan
            if (Bukkit.getPluginManager().getPlugin("Plan") != null) {
                log(Level.INFO, "Enabling Plan integration...");
                new PlanHook(this).hookIntoPlan();
                log(Level.INFO, "Plan integration enabled!");
            }

            // Hook into bStats metrics
            try {
                new Metrics(this, METRICS_ID);
            } catch (final Exception e) {
                log(Level.WARNING, "Skipped bStats metrics initialization due to an exception.");
            }

            // Check for updates
            if (settings.doCheckForUpdates()) {
                log(Level.INFO, "Checking for updates...");
                getLatestVersionIfOutdated().thenAccept(newestVersion ->
                        newestVersion.ifPresent(newVersion -> log(Level.WARNING,
                                "An update is available for HuskSync, v" + newVersion
                                        + " (Currently running v" + getPluginVersion() + ")")));
            }
        } catch (IllegalStateException exception) {
            log(Level.SEVERE, """
                    ***************************************************
                               
                              Failed to initialize HuskSync!
                               
                    ***************************************************
                    The plugin was disabled due to an error. Please check
                    the logs below for details.
                    No user data will be synchronised.
                    ***************************************************
                    Caused by: %error_message%
                    """
                    .replaceAll("%error_message%", exception.getMessage()));
            initialized.set(false);
        } catch (Exception exception) {
            log(Level.SEVERE, "An unhandled exception occurred initializing HuskSync!", exception);
            initialized.set(false);
        } finally {
            // Validate initialization
            if (initialized.get()) {
                log(Level.INFO, "Successfully enabled HuskSync v" + getPluginVersion());
            } else {
                log(Level.SEVERE, "Failed to initialize HuskSync. The plugin will now be disabled");
                getServer().getPluginManager().disablePlugin(this);
            }
        }
    }

    @Override
    public void onDisable() {
        if (this.eventListener != null) {
            this.eventListener.handlePluginDisable();
        }
        log(Level.INFO, "Successfully disabled HuskSync v" + getPluginVersion());
    }

    @Override
    public @NotNull Set<OnlineUser> getOnlineUsers() {
        return Bukkit.getOnlinePlayers().stream().map(BukkitPlayer::adapt).collect(Collectors.toSet());
    }

    @Override
    public @NotNull Optional<OnlineUser> getOnlineUser(@NotNull UUID uuid) {
        final Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return Optional.empty();
        }
        return Optional.of(BukkitPlayer.adapt(player));
    }

    @Override
    public @NotNull Database getDatabase() {
        return database;
    }

    @Override
    public @NotNull RedisManager getRedisManager() {
        return redisManager;
    }

    @Override
    public @NotNull DataAdapter getDataAdapter() {
        return dataAdapter;
    }

    @Override
    public @NotNull EventCannon getEventCannon() {
        return eventCannon;
    }

    @NotNull
    @Override
    public List<Migrator> getAvailableMigrators() {
        return availableMigrators;
    }

    @Override
    public @NotNull Settings getSettings() {
        return settings;
    }

    @Override
    public @NotNull Locales getLocales() {
        return locales;
    }

    @Override
    public void log(@NotNull Level level, @NotNull String message, @NotNull Throwable... throwable) {
        if (throwable.length > 0) {
            getLogger().log(level, message, throwable[0]);
        } else {
            getLogger().log(level, message);
        }
    }

    @NotNull
    @Override
    public Version getPluginVersion() {
        return Version.fromString(getDescription().getVersion(), "-");
    }

    @NotNull
    @Override
    public Version getMinecraftVersion() {
        return Version.fromString(Bukkit.getBukkitVersion());
    }

    /**
     * Returns the adventure Bukkit audiences
     *
     * @return The adventure Bukkit audiences
     */
    @NotNull
    public BukkitAudiences getAudiences() {
        return audiences;
    }

    @Override
    public Set<UUID> getLockedPlayers() {
        return this.eventListener.getLockedPlayers();
    }

    @Override
    public CompletableFuture<Boolean> reload() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Load plugin settings
                this.settings = Annotaml.create(new File(getDataFolder(), "config.yml"), new Settings()).get();

                // Load locales from language preset default
                final Locales languagePresets = Annotaml.create(Locales.class,
                        Objects.requireNonNull(getResource("locales/" + settings.getLanguage() + ".yml"))).get();
                this.locales = Annotaml.create(new File(getDataFolder(), "messages_" + settings.getLanguage() + ".yml"),
                        languagePresets).get();
                return true;
            } catch (IOException | NullPointerException | InvocationTargetException | IllegalAccessException |
                     InstantiationException e) {
                log(Level.SEVERE, "Failed to load data from the config", e);
                return false;
            }
        });
    }
}
