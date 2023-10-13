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

import com.google.gson.Gson;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.minecraft.server.MinecraftServer;
import net.william278.desertwell.util.Version;
import net.william278.husksync.adapter.DataAdapter;
import net.william278.husksync.adapter.GsonAdapter;
import net.william278.husksync.adapter.SnappyGsonAdapter;
import net.william278.husksync.command.Command;
import net.william278.husksync.command.FabricCommand;
import net.william278.husksync.config.Locales;
import net.william278.husksync.config.Server;
import net.william278.husksync.config.Settings;
import net.william278.husksync.data.Data;
import net.william278.husksync.data.FabricSerializer;
import net.william278.husksync.data.Identifier;
import net.william278.husksync.data.Serializer;
import net.william278.husksync.database.Database;
import net.william278.husksync.database.MySqlDatabase;
import net.william278.husksync.event.FabricEventDispatcher;
import net.william278.husksync.hook.PlanHook;
import net.william278.husksync.listener.EventListener;
import net.william278.husksync.listener.FabricEventListener;
import net.william278.husksync.migrator.Migrator;
import net.william278.husksync.redis.RedisManager;
import net.william278.husksync.sync.DataSyncer;
import net.william278.husksync.user.ConsoleUser;
import net.william278.husksync.user.FabricUser;
import net.william278.husksync.user.OnlineUser;
import net.william278.husksync.util.FabricTask;
import net.william278.husksync.util.LegacyConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class FabricHuskSync implements DedicatedServerModInitializer, HuskSync, FabricTask.Supplier,
        FabricEventDispatcher {
    private Logger logger;
    private ModContainer mod;
    private MinecraftServer minecraftServer;
    private Database database;
    private RedisManager redisManager;
    private EventListener eventListener;
    private DataAdapter dataAdapter;
    private Map<Identifier, Serializer<? extends Data>> serializers;
    private Map<UUID, Map<Identifier, Data>> playerCustomDataStore;
    private Set<UUID> lockedPlayers;
    private DataSyncer dataSyncer;
    private Settings settings;
    private Locales locales;
    private Server server;
    private Map<String, Boolean> permissions;
    private FabricServerAudiences audiences;
    private Gson gson;
    private boolean disabling;

    public FabricHuskSync() {
    }

    @Override
    public void onInitializeServer() {
        // Get the logger and mod container
        this.logger = LoggerFactory.getLogger("HuskSync");
        this.mod = FabricLoader.getInstance().getModContainer("husksync").orElseThrow();

        // Prepare utils
        this.disabling = false;
        this.gson = createGson();
        this.serializers = new LinkedHashMap<>();
        this.lockedPlayers = new ConcurrentSkipListSet<>();
        this.playerCustomDataStore = new ConcurrentHashMap<>();
        this.permissions = new HashMap<>();

        // Load settings and locales
        initialize("plugin config & locale files", (plugin) -> this.loadConfigs());

        // Register commands
        initialize("commands", (plugin) -> this.registerCommands());

        // Load HuskSync after server startup
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            this.minecraftServer = server;
            this.onEnable();
        });

        // Unload HuskSync before server shutdown
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> this.onDisable());
    }

    private void onEnable() {
        // Initial plugin setup
        this.audiences = FabricServerAudiences.of(minecraftServer);

        // Prepare data adapter
        initialize("data adapter", (plugin) -> {
            if (getSettings().doCompressData()) {
                this.dataAdapter = new SnappyGsonAdapter(this);
            } else {
                this.dataAdapter = new GsonAdapter(this);
            }
        });

        // TODO: Prepare serializers
        initialize("data serializers", (plugin) -> {
            registerSerializer(Identifier.INVENTORY, new FabricSerializer.Inventory(this));
            registerSerializer(Identifier.ENDER_CHEST, new FabricSerializer.EnderChest(this));
        });

        // Initialize the database
        initialize(getSettings().getDatabaseType().getDisplayName() + " database connection", (plugin) -> {
            this.database = new MySqlDatabase(this);
            this.database.initialize();
        });

        // Prepare redis connection
        initialize("Redis server connection", (plugin) -> {
            this.redisManager = new RedisManager(this);
            this.redisManager.initialize();
        });

        // Prepare data syncer
        initialize("data syncer", (plugin) -> {
            this.dataSyncer = getSettings().getSyncMode().create(this);
            this.dataSyncer.initialize();
        });

        // Register events
        initialize("events", (plugin) -> this.eventListener = new FabricEventListener(this));

        // Register plugin hooks
        initialize("hooks", (plugin) -> {
            if (isDependencyLoaded("Plan") && getSettings().usePlanHook()) {
                new PlanHook(this).hookIntoPlan();
            }
        });

        // TODO: Register API
//        initialize("api", (plugin) -> {
//        });

        // Check for updates
        this.checkForUpdates();
    }

    private void onDisable() {
        // Handle shutdown
        this.disabling = true;

        // Close the event listener / data syncer
        if (this.dataSyncer != null) {
            this.dataSyncer.terminate();
        }
        if (this.eventListener != null) {
            this.eventListener.handlePluginDisable();
        }

        // Cancel tasks, close audiences
        if (audiences != null) {
            this.audiences.close();
        }
        this.cancelTasks();

        // Complete shutdown
        log(Level.INFO, "Successfully disabled HuskSync v" + getPluginVersion());
    }

    private void registerCommands() {
        final List<Command> commands = FabricCommand.Type.getCommands(this);
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
                commands.forEach(command -> new FabricCommand(command, this).register(dispatcher))
        );
    }

    @NotNull
    public FabricServerAudiences getAudiences() {
        return audiences;
    }

    @NotNull
    public Map<String, Boolean> getPermissions() {
        return permissions;
    }

    @Override
    @NotNull
    public Database getDatabase() {
        return database;
    }

    @Override
    @NotNull
    public RedisManager getRedisManager() {
        return redisManager;
    }

    @Override
    @NotNull
    public DataAdapter getDataAdapter() {
        return dataAdapter;
    }

    @NotNull
    @Override
    public DataSyncer getDataSyncer() {
        return dataSyncer;
    }

    @Override
    public void setDataSyncer(@NotNull DataSyncer dataSyncer) {

    }

    @NotNull
    @Override
    public Map<Identifier, Serializer<? extends Data>> getSerializers() {
        return serializers;
    }

    @Override
    @NotNull
    public Settings getSettings() {
        return settings;
    }

    @Override
    public void setSettings(@NotNull Settings settings) {
        this.settings = settings;
    }

    @NotNull
    @Override
    public String getServerName() {
        return server.getName();
    }

    @Override
    public void setServer(@NotNull Server server) {
        this.server = server;
    }

    @Override
    @NotNull
    public Locales getLocales() {
        return locales;
    }

    @Override
    public void setLocales(@NotNull Locales locales) {
        this.locales = locales;
    }

    @Override
    public boolean isDependencyLoaded(@NotNull String name) {
        return FabricLoader.getInstance().isModLoaded(name);
    }

    @Override
    @NotNull
    public List<Migrator> getAvailableMigrators() {
        return List.of(); // No migrators available on Fabric
    }

    @NotNull
    @Override
    public Map<UUID, Map<Identifier, Data>> getPlayerCustomDataStore() {
        return playerCustomDataStore;
    }

    @Override
    @NotNull
    public Set<OnlineUser> getOnlineUsers() {
        return minecraftServer.getPlayerManager().getPlayerList()
                .stream().map(user -> (OnlineUser) FabricUser.adapt(user, this))
                .collect(Collectors.toSet());
    }

    @Override
    @NotNull
    public Optional<OnlineUser> getOnlineUser(@NotNull UUID uuid) {
        return Optional.ofNullable(minecraftServer.getPlayerManager().getPlayer(uuid))
                .map(user -> FabricUser.adapt(user, this));
    }

    @Override
    @Nullable
    public InputStream getResource(@NotNull String name) {
        return this.mod.findPath(name)
                .map(path -> {
                    try {
                        return Files.newInputStream(path);
                    } catch (IOException e) {
                        log(Level.WARNING, "Failed to load resource: " + name, e);
                    }
                    return null;
                })
                .orElse(this.getClass().getClassLoader().getResourceAsStream(name));
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public void log(@NotNull Level level, @NotNull String message, @NotNull Throwable... throwable) {
        LoggingEventBuilder logEvent = logger.makeLoggingEventBuilder(
                switch (level.getName()) {
                    case "WARNING" -> org.slf4j.event.Level.WARN;
                    case "SEVERE" -> org.slf4j.event.Level.ERROR;
                    default -> org.slf4j.event.Level.INFO;
                }
        );
        if (throwable.length >= 1) {
            logEvent = logEvent.setCause(throwable[0]);
        }
        logEvent.log(message);
    }

    @NotNull
    @Override
    public ConsoleUser getConsole() {
        return new ConsoleUser(audiences.console());
    }

    @Override
    @NotNull
    public Version getPluginVersion() {
        return Version.fromString(mod.getMetadata().getVersion().getFriendlyString(), "-");
    }

    @Override
    @NotNull
    public File getDataFolder() throws IOException {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("husksync");

        if (!Files.isDirectory(path)) {
            Files.createDirectory(path);
        }

        return path.toFile();
    }

    @Override
    @NotNull
    public Version getMinecraftVersion() {
        return Version.fromString(minecraftServer.getVersion());
    }

    @NotNull
    @Override
    public String getPlatformType() {
        return "fabric";
    }

    @Override
    public Optional<LegacyConverter> getLegacyConverter() {
        return Optional.empty();
    }

    @NotNull
    @Override
    public Set<UUID> getLockedPlayers() {
        return lockedPlayers;
    }

    @NotNull
    @Override
    public Gson getGson() {
        return gson;
    }

    @Override
    public boolean isDisabling() {
        return disabling;
    }

    @NotNull
    public MinecraftServer getMinecraftServer() {
        return minecraftServer;
    }

    @Override
    @NotNull
    public FabricHuskSync getPlugin() {
        return this;
    }

}
