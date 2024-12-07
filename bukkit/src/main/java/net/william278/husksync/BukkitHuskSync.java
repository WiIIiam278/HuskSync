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
import de.tr7zw.changeme.nbtapi.utils.DataFixerUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.platform.AudienceProvider;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.william278.desertwell.util.Version;
import net.william278.husksync.adapter.DataAdapter;
import net.william278.husksync.adapter.GsonAdapter;
import net.william278.husksync.adapter.SnappyGsonAdapter;
import net.william278.husksync.api.BukkitHuskSyncAPI;
import net.william278.husksync.command.PluginCommand;
import net.william278.husksync.config.Locales;
import net.william278.husksync.config.Server;
import net.william278.husksync.config.Settings;
import net.william278.husksync.data.*;
import net.william278.husksync.database.Database;
import net.william278.husksync.database.MongoDbDatabase;
import net.william278.husksync.database.MySqlDatabase;
import net.william278.husksync.database.PostgresDatabase;
import net.william278.husksync.event.BukkitEventDispatcher;
import net.william278.husksync.hook.PlanHook;
import net.william278.husksync.listener.BukkitEventListener;
import net.william278.husksync.migrator.LegacyMigrator;
import net.william278.husksync.migrator.Migrator;
import net.william278.husksync.migrator.MpdbMigrator;
import net.william278.husksync.redis.RedisManager;
import net.william278.husksync.sync.DataSyncer;
import net.william278.husksync.user.BukkitUser;
import net.william278.husksync.user.OnlineUser;
import net.william278.husksync.util.BukkitLegacyConverter;
import net.william278.husksync.util.BukkitMapPersister;
import net.william278.husksync.util.BukkitTask;
import net.william278.husksync.util.LegacyConverter;
import net.william278.uniform.Uniform;
import net.william278.uniform.bukkit.BukkitUniform;
import org.bstats.bukkit.Metrics;
import org.bukkit.entity.Player;
import org.bukkit.map.MapView;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import space.arim.morepaperlib.MorePaperLib;
import space.arim.morepaperlib.scheduling.AsynchronousScheduler;
import space.arim.morepaperlib.scheduling.AttachedScheduler;
import space.arim.morepaperlib.scheduling.GracefulScheduling;
import space.arim.morepaperlib.scheduling.RegionalScheduler;

import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@SuppressWarnings("unchecked")
public class BukkitHuskSync extends JavaPlugin implements HuskSync, BukkitTask.Supplier,
        BukkitEventDispatcher, BukkitMapPersister {

    /**
     * Metrics ID for <a href="https://bstats.org/plugin/bukkit/HuskSync%20-%20Bukkit/13140">HuskSync on Bukkit</a>.
     */
    private static final int METRICS_ID = 13140;
    private static final String PLATFORM_TYPE_ID = "bukkit";

    private final TreeMap<Identifier, Serializer<? extends Data>> serializers = Maps.newTreeMap(
            SerializerRegistry.DEPENDENCY_ORDER_COMPARATOR
    );
    private final Map<UUID, Map<Identifier, Data>> playerCustomDataStore = Maps.newConcurrentMap();
    private final Map<Integer, MapView> mapViews = Maps.newConcurrentMap();
    private final List<Migrator> availableMigrators = Lists.newArrayList();
    private final Set<UUID> lockedPlayers = Sets.newConcurrentHashSet();

    private boolean disabling;
    private Gson gson;
    private AudienceProvider audiences;
    private MorePaperLib paperLib;
    private Database database;
    private RedisManager redisManager;
    private BukkitEventListener eventListener;
    private DataAdapter dataAdapter;
    private DataSyncer dataSyncer;
    private LegacyConverter legacyConverter;
    private AsynchronousScheduler asyncScheduler;
    private RegionalScheduler regionalScheduler;
    @Setter
    private Settings settings;
    @Setter
    private Locales locales;
    @Setter
    @Getter(AccessLevel.NONE)
    private Server serverName;

    @Override
    public void onLoad() {
        // Initial plugin setup
        this.disabling = false;
        this.gson = createGson();
        this.paperLib = new MorePaperLib(this);

        // Load settings and locales
        initialize("plugin config & locale files", (plugin) -> {
            loadSettings();
            loadLocales();
            loadServer();
            validateConfigFiles();
        });

        this.eventListener = createEventListener();
        eventListener.onLoad();
    }

    @Override
    public void onEnable() {
        this.audiences = BukkitAudiences.create(this);

        // Check compatibility
        checkCompatibility();

        // Register commands
        initialize("commands", (plugin) -> getUniform().register(PluginCommand.Type.create(this)));

        // Prepare data adapter
        initialize("data adapter", (plugin) -> {
            if (settings.getSynchronization().isCompressData()) {
                dataAdapter = new SnappyGsonAdapter(this);
            } else {
                dataAdapter = new GsonAdapter(this);
            }
        });

        // Prepare serializers
        initialize("data serializers", (plugin) -> {
            registerSerializer(Identifier.PERSISTENT_DATA, new BukkitSerializer.PersistentData(this));
            registerSerializer(Identifier.INVENTORY, new BukkitSerializer.Inventory(this));
            registerSerializer(Identifier.ENDER_CHEST, new BukkitSerializer.EnderChest(this));
            registerSerializer(Identifier.ADVANCEMENTS, new BukkitSerializer.Advancements(this));
            registerSerializer(Identifier.STATISTICS, new Serializer.Json<>(this, BukkitData.Statistics.class));
            registerSerializer(Identifier.POTION_EFFECTS, new BukkitSerializer.PotionEffects(this));
            registerSerializer(Identifier.GAME_MODE, new Serializer.Json<>(this, BukkitData.GameMode.class));
            registerSerializer(Identifier.FLIGHT_STATUS, new Serializer.Json<>(this, BukkitData.FlightStatus.class));
            registerSerializer(Identifier.ATTRIBUTES, new Serializer.Json<>(this, BukkitData.Attributes.class));
            registerSerializer(Identifier.HEALTH, new Serializer.Json<>(this, BukkitData.Health.class));
            registerSerializer(Identifier.HUNGER, new Serializer.Json<>(this, BukkitData.Hunger.class));
            registerSerializer(Identifier.EXPERIENCE, new Serializer.Json<>(this, BukkitData.Experience.class));
            registerSerializer(Identifier.LOCATION, new Serializer.Json<>(this, BukkitData.Location.class));
            validateDependencies();
        });

        // Setup available migrators
        initialize("data migrators/converters", (plugin) -> {
            availableMigrators.add(new LegacyMigrator(this));
            if (isDependencyLoaded("MySqlPlayerDataBridge")) {
                availableMigrators.add(new MpdbMigrator(this));
            }
            legacyConverter = new BukkitLegacyConverter(this);
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
        initialize("events", (plugin) -> eventListener.onEnable());

        // Register plugin hooks
        initialize("hooks", (plugin) -> {
            if (isDependencyLoaded("Plan") && getSettings().isEnablePlanHook()) {
                new PlanHook(this).hookIntoPlan();
            }
        });

        // Register API
        initialize("api", (plugin) -> BukkitHuskSyncAPI.register(this));

        // Hook into bStats and check for updates
        initialize("metrics", (plugin) -> this.registerMetrics(METRICS_ID));
        this.checkForUpdates();
    }

    @Override
    public void onDisable() {
        // Handle shutdown
        this.disabling = true;

        // Close the event listener / data syncer
        if (this.dataSyncer != null) {
            this.dataSyncer.terminate();
        }
        if (this.eventListener != null) {
            this.eventListener.handlePluginDisable();
        }

        // Unregister API and cancel tasks
        BukkitHuskSyncAPI.unregister();
        this.cancelTasks();

        // Complete shutdown
        log(Level.INFO, "Successfully disabled HuskSync v" + getPluginVersion());
    }

    @NotNull
    protected BukkitEventListener createEventListener() {
        return new BukkitEventListener(this);
    }

    @Override
    @NotNull
    public Set<OnlineUser> getOnlineUsers() {
        return getServer().getOnlinePlayers().stream()
                .map(player -> BukkitUser.adapt(player, this))
                .collect(Collectors.toSet());
    }

    @Override
    @NotNull
    public Optional<OnlineUser> getOnlineUser(@NotNull UUID uuid) {
        final Player player = getServer().getPlayer(uuid);
        if (player == null) {
            return Optional.empty();
        }
        return Optional.of(BukkitUser.adapt(player, this));
    }

    @Override
    public void setDataSyncer(@NotNull DataSyncer dataSyncer) {
        log(Level.INFO, String.format("Switching data syncer to %s", dataSyncer.getClass().getSimpleName()));
        this.dataSyncer = dataSyncer;
    }

    @Override
    @NotNull
    public Uniform getUniform() {
        return BukkitUniform.getInstance(this);
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
    @NotNull
    public String getServerName() {
        return serverName == null ? "server" : serverName.getName();
    }

    @Override
    public boolean isDependencyLoaded(@NotNull String name) {
        final Plugin plugin = getServer().getPluginManager().getPlugin(name);
        return plugin != null;
    }

    // Register bStats metrics
    public void registerMetrics(int metricsId) {
        if (!getPluginVersion().getMetadata().isBlank()) {
            return;
        }

        try {
            new Metrics(this, metricsId);
        } catch (Throwable e) {
            log(Level.WARNING, "Failed to register bStats metrics (%s)".formatted(e.getMessage()));
        }
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
        return Version.fromString(getServer().getBukkitVersion());
    }

    public int getDataVersion(@NotNull Version mcVersion) {
        return switch (mcVersion.toStringWithoutMetadata()) {
            case "1.16", "1.16.1", "1.16.2", "1.16.3", "1.16.4", "1.16.5" -> DataFixerUtil.VERSION1_16_5;
            case "1.17", "1.17.1" -> DataFixerUtil.VERSION1_17_1;
            case "1.18", "1.18.1", "1.18.2" -> DataFixerUtil.VERSION1_18_2;
            case "1.19", "1.19.1", "1.19.2" -> DataFixerUtil.VERSION1_19_2;
            case "1.20", "1.20.1", "1.20.2" -> DataFixerUtil.VERSION1_20_2;
            case "1.20.3", "1.20.4" -> DataFixerUtil.VERSION1_20_4;
            case "1.20.5", "1.20.6" -> DataFixerUtil.VERSION1_20_5;
            case "1.21", "1.21.1" -> DataFixerUtil.VERSION1_21;
            case "1.21.2", "1.21.3" -> DataFixerUtil.VERSION1_21_2;
            case "1.21.4" -> 4189/*DataFixerUtil.VERSION1_21_4*/;
            default -> DataFixerUtil.getCurrentVersion();
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
        return String.format("%s/%s", getServer().getName(), getServer().getVersion());
    }

    @Override
    public Optional<LegacyConverter> getLegacyConverter() {
        return Optional.of(legacyConverter);
    }

    @NotNull
    public GracefulScheduling getScheduler() {
        return paperLib.scheduling();
    }

    @NotNull
    public AsynchronousScheduler getAsyncScheduler() {
        return asyncScheduler == null
                ? asyncScheduler = getScheduler().asyncScheduler() : asyncScheduler;
    }

    @NotNull
    public RegionalScheduler getSyncScheduler() {
        return regionalScheduler == null
                ? regionalScheduler = getScheduler().globalRegionalScheduler() : regionalScheduler;
    }

    @NotNull
    public AttachedScheduler getUserSyncScheduler(@NotNull UserDataHolder user) {
        return getScheduler().entitySpecificScheduler(((BukkitUser) user).getPlayer());
    }

    @Override
    @NotNull
    public Path getConfigDirectory() {
        return getDataFolder().toPath();
    }

    @Override
    @NotNull
    public BukkitHuskSync getPlugin() {
        return this;
    }

}
