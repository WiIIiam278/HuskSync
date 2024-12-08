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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.kyori.adventure.platform.AudienceProvider;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.minecraft.server.MinecraftServer;
import net.william278.desertwell.util.Version;
import net.william278.husksync.adapter.DataAdapter;
import net.william278.husksync.adapter.GsonAdapter;
import net.william278.husksync.adapter.SnappyGsonAdapter;
import net.william278.husksync.api.FabricHuskSyncAPI;
import net.william278.husksync.command.PluginCommand;
import net.william278.husksync.config.Locales;
import net.william278.husksync.config.Server;
import net.william278.husksync.config.Settings;
import net.william278.husksync.data.*;
import net.william278.husksync.database.Database;
import net.william278.husksync.database.MongoDbDatabase;
import net.william278.husksync.database.MySqlDatabase;
import net.william278.husksync.database.PostgresDatabase;
import net.william278.husksync.event.FabricEventDispatcher;
import net.william278.husksync.event.ModLoadedCallback;
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
import net.william278.uniform.Uniform;
import net.william278.uniform.fabric.FabricUniform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;

@Getter
@NoArgsConstructor
@SuppressWarnings("unchecked")
public class FabricHuskSync implements DedicatedServerModInitializer, HuskSync, FabricTask.Supplier,
        FabricEventDispatcher {

    private static final String PLATFORM_TYPE_ID = "fabric";

    private static final int VERSION1_16_5 = 2586;
    private static final int VERSION1_17_1 = 2730;
    private static final int VERSION1_18_2 = 2975;
    private static final int VERSION1_19_2 = 3120;
    private static final int VERSION1_19_4 = 3337;
    private static final int VERSION1_20_1 = 3465;
    private static final int VERSION1_20_2 = 3578;
    private static final int VERSION1_20_4 = 3700;
    private static final int VERSION1_20_5 = 3837;
    private static final int VERSION1_21_1 = 3955;
    private static final int VERSION1_21_3 = 4082;
    private static final int VERSION1_21_4 = 4189; // Current

    private final TreeMap<Identifier, Serializer<? extends Data>> serializers = Maps.newTreeMap(
            SerializerRegistry.DEPENDENCY_ORDER_COMPARATOR
    );
    private final Map<UUID, Map<Identifier, Data>> playerCustomDataStore = Maps.newConcurrentMap();
    private final Map<String, Boolean> permissions = Maps.newHashMap();
    private final List<Migrator> availableMigrators = Lists.newArrayList();
    private final Set<UUID> lockedPlayers = Sets.newConcurrentHashSet();
    private final Map<UUID, FabricUser> playerMap = Maps.newConcurrentMap();

    private Logger logger;
    private ModContainer mod;
    private MinecraftServer minecraftServer;
    private boolean disabling;
    private Gson gson;
    private AudienceProvider audiences;
    private Database database;
    private RedisManager redisManager;
    private EventListener eventListener;
    private DataAdapter dataAdapter;
    @Setter
    private DataSyncer dataSyncer;
    @Setter
    private Settings settings;
    @Setter
    private Locales locales;
    @Setter
    @Getter(AccessLevel.NONE)
    private Server serverName;

    @Override
    public void onInitializeServer() {
        // Get the logger and mod container
        this.logger = LoggerFactory.getLogger("HuskSync");
        this.mod = FabricLoader.getInstance().getModContainer("husksync").orElseThrow();
        this.disabling = false;
        this.gson = createGson();

        // Load settings and locales
        initialize("plugin config & locale files", (plugin) -> {
            loadSettings();
            loadLocales();
            loadServer();
            validateConfigFiles();
        });

        // Register commands
        initialize("commands", (plugin) -> getUniform().register(PluginCommand.Type.create(this)));

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
        this.audiences = MinecraftServerAudiences.of(minecraftServer);

        // Check compatibility
        checkCompatibility();

        // Prepare data adapter
        initialize("data adapter", (plugin) -> {
            if (getSettings().getSynchronization().isCompressData()) {
                this.dataAdapter = new SnappyGsonAdapter(this);
            } else {
                this.dataAdapter = new GsonAdapter(this);
            }
        });

        initialize("data serializers", (plugin) -> {
            // PERSISTENT_DATA is not registered / available on the Fabric platform
            registerSerializer(Identifier.INVENTORY, new FabricSerializer.Inventory(this));
            registerSerializer(Identifier.ENDER_CHEST, new FabricSerializer.EnderChest(this));
            registerSerializer(Identifier.ADVANCEMENTS, new FabricSerializer.Advancements(this));
            registerSerializer(Identifier.STATISTICS, new Serializer.Json<>(this, FabricData.Statistics.class));
            registerSerializer(Identifier.POTION_EFFECTS, new FabricSerializer.PotionEffects(this));
            registerSerializer(Identifier.GAME_MODE, new Serializer.Json<>(this, FabricData.GameMode.class));
            registerSerializer(Identifier.FLIGHT_STATUS, new Serializer.Json<>(this, FabricData.FlightStatus.class));
            registerSerializer(Identifier.ATTRIBUTES, new Serializer.Json<>(this, FabricData.Attributes.class));
            registerSerializer(Identifier.HEALTH, new Serializer.Json<>(this, FabricData.Health.class));
            registerSerializer(Identifier.HUNGER, new Serializer.Json<>(this, FabricData.Hunger.class));
            registerSerializer(Identifier.EXPERIENCE, new Serializer.Json<>(this, FabricData.Experience.class));
            registerSerializer(Identifier.LOCATION, new Serializer.Json<>(this, FabricData.Location.class));
            validateDependencies();
        });

        // Initialize the database
        initialize(getSettings().getDatabase().getType().getDisplayName() + " database connection", (plugin) -> {
            this.database = switch (settings.getDatabase().getType()) {
                case MYSQL, MARIADB -> new MySqlDatabase(this);
                case POSTGRES -> new PostgresDatabase(this);
                case MONGO -> new MongoDbDatabase(this);
            };
            this.database.initialize();
        });

        // Prepare redis connection
        initialize("Redis server connection", (plugin) -> {
            this.redisManager = new RedisManager(this);
            this.redisManager.initialize();
        });

        // Prepare data syncer
        initialize("data syncer", (plugin) -> {
            dataSyncer = getSettings().getSynchronization().getMode().create(this);
            dataSyncer.initialize();
        });

        // Register events
        initialize("events", (plugin) -> this.eventListener = new FabricEventListener(this));

        // Register plugin hooks
        initialize("hooks", (plugin) -> {
            if (isDependencyLoaded("Plan") && getSettings().isEnablePlanHook()) {
                new PlanHook(this).hookIntoPlan();
            }
        });

        // Register API
        initialize("api", (plugin) -> FabricHuskSyncAPI.register(this));

        // Check for updates
        this.checkForUpdates();

        ModLoadedCallback.EVENT.invoker().post(FabricHuskSyncAPI.getInstance());
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


    @NotNull
    @Override
    public String getServerName() {
        return serverName.getName();
    }

    @Override
    public boolean isDependencyLoaded(@NotNull String name) {
        return FabricLoader.getInstance().isModLoaded(name);
    }

    @Override
    @NotNull
    public Set<OnlineUser> getOnlineUsers() {
        return Sets.newHashSet(playerMap.values());
    }

    @Override
    @NotNull
    public Optional<OnlineUser> getOnlineUser(@NotNull UUID uuid) {
        return Optional.ofNullable(playerMap.get(uuid));
    }

    @Override
    @NotNull
    public Uniform getUniform() {
        return FabricUniform.getInstance(mod.getMetadata().getId());
    }

    @NotNull
    @Override
    public Map<Identifier, Data> getPlayerCustomDataStore(@NotNull OnlineUser user) {
        return playerCustomDataStore.compute(
                user.getUuid(),
                (uuid, data) -> data == null ? Maps.newHashMap() : data
        );
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
    @NotNull
    public Path getConfigDirectory() {
        final Path path = FabricLoader.getInstance().getConfigDir().resolve("husksync");
        if (!Files.isDirectory(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                log(Level.SEVERE, "Failed to create config directory", e);
            }
        }
        return path;
    }

    @Override
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
        return new ConsoleUser(audiences);
    }

    @Override
    @NotNull
    public Version getPluginVersion() {
        return Version.fromString(mod.getMetadata().getVersion().getFriendlyString(), "-");
    }

    @Override
    @NotNull
    public Version getMinecraftVersion() {
        return Version.fromString(minecraftServer.getVersion());
    }

    @NotNull
    public int getDataVersion(@NotNull Version mcVersion) {
        return switch (mcVersion.toStringWithoutMetadata()) {
            case "1.16", "1.16.1", "1.16.2", "1.16.3", "1.16.4", "1.16.5" -> VERSION1_16_5;
            case "1.17", "1.17.1" -> VERSION1_17_1;
            case "1.18", "1.18.1", "1.18.2" -> VERSION1_18_2;
            case "1.19", "1.19.1", "1.19.2" -> VERSION1_19_2;
            case "1.19.4" -> VERSION1_19_4;
            case "1.20", "1.20.1" -> VERSION1_20_1;
            case "1.20.2" -> VERSION1_20_2;
            case "1.20.4" -> VERSION1_20_4;
            case "1.20.5", "1.20.6" -> VERSION1_20_5;
            case "1.21", "1.21.1" -> VERSION1_21_1;
            case "1.21.2", "1.21.3" -> VERSION1_21_3;
            case "1.21.4" -> VERSION1_21_4;
            default -> VERSION1_21_4; // Current supported ver
        };
    }

    @NotNull
    @Override
    public String getPlatformType() {
        return PLATFORM_TYPE_ID;
    }

    @Override
    @NotNull
    public String getServerVersion() {
        return String.format("%s %s/%s", getPlatformType(), FabricLoader.getInstance()
                .getModContainer("fabricloader").map(l -> l.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown"), minecraftServer.getVersion());
    }

    @Override
    public Optional<LegacyConverter> getLegacyConverter() {
        return Optional.empty();
    }

    @Override
    @NotNull
    public FabricHuskSync getPlugin() {
        return this;
    }

}
