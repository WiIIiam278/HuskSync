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
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.william278.desertwell.util.Version;
import net.william278.husksync.adapter.DataAdapter;
import net.william278.husksync.adapter.GsonAdapter;
import net.william278.husksync.adapter.SnappyGsonAdapter;
import net.william278.husksync.command.BukkitCommand;
import net.william278.husksync.config.Locales;
import net.william278.husksync.config.Settings;
import net.william278.husksync.data.BukkitSerializer;
import net.william278.husksync.data.Data;
import net.william278.husksync.data.Identifier;
import net.william278.husksync.data.Serializer;
import net.william278.husksync.database.Database;
import net.william278.husksync.database.MySqlDatabase;
import net.william278.husksync.event.BukkitEventDispatcher;
import net.william278.husksync.hook.PlanHook;
import net.william278.husksync.listener.BukkitEventListener;
import net.william278.husksync.listener.EventListener;
import net.william278.husksync.migrator.Migrator;
import net.william278.husksync.player.BukkitUser;
import net.william278.husksync.player.ConsoleUser;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.redis.RedisManager;
import net.william278.husksync.util.BukkitLegacyConverter;
import net.william278.husksync.util.BukkitTask;
import net.william278.husksync.util.LegacyConverter;
import net.william278.husksync.util.MapPersister;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import space.arim.morepaperlib.MorePaperLib;
import space.arim.morepaperlib.commands.CommandRegistration;
import space.arim.morepaperlib.scheduling.GracefulScheduling;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class BukkitHuskSync extends JavaPlugin implements HuskSync, BukkitTask.Supplier, BukkitEventDispatcher, MapPersister {

    /**
     * Metrics ID for <a href="https://bstats.org/plugin/bukkit/HuskSync%20-%20Bukkit/13140">HuskSync on Bukkit</a>.
     */
    private static final int METRICS_ID = 13140;
    private static final String PLATFORM_TYPE_ID = "bukkit";

    private Database database;
    private RedisManager redisManager;
    private EventListener eventListener;
    private DataAdapter dataAdapter;
    private Map<Identifier, Serializer<? extends Data>> serializers;
    private Settings settings;
    private Locales locales;
    private List<Migrator> availableMigrators;
    private LegacyConverter legacyConverter;
    private Map<Integer, MapView> mapViews;
    private BukkitAudiences audiences;
    private MorePaperLib paperLib;
    private Gson gson;

    @Override
    public void onEnable() {
        // Initial plugin setup
        this.gson = createGson();
        this.audiences = BukkitAudiences.create(this);
        this.paperLib = new MorePaperLib(this);
        this.availableMigrators = new ArrayList<>();
        this.serializers = new ConcurrentHashMap<>();
        this.mapViews = new ConcurrentHashMap<>();

        // Load settings and locales
        initialize("plugin config & locale files", (plugin) -> this.loadConfigs());

        // Prepare data adapter
        initialize("data adapter", (plugin) -> {
            if (settings.doCompressData()) {
                dataAdapter = new SnappyGsonAdapter(this);
            } else {
                dataAdapter = new GsonAdapter(this);
            }
        });

        // Prepare serializers
        initialize("data serializers", (plugin) -> {
            registerSerializer(Identifier.INVENTORY, new BukkitSerializer.Inventory(this));
            registerSerializer(Identifier.ENDER_CHEST, new BukkitSerializer.EnderChest(this));
            registerSerializer(Identifier.POTION_EFFECTS, new BukkitSerializer.PotionEffects(this));
            registerSerializer(Identifier.ADVANCEMENTS, new BukkitSerializer.Advancements(this));
            registerSerializer(Identifier.LOCATION, new BukkitSerializer.Location(this));
            registerSerializer(Identifier.STATISTICS, new BukkitSerializer.Statistics(this));
            registerSerializer(Identifier.HEALTH, new BukkitSerializer.Health(this));
            registerSerializer(Identifier.HUNGER, new BukkitSerializer.Hunger(this));
            registerSerializer(Identifier.EXPERIENCE, new BukkitSerializer.Experience(this));
            registerSerializer(Identifier.GAME_MODE, new BukkitSerializer.GameMode(this));
            registerSerializer(Identifier.PERSISTENT_DATA, new BukkitSerializer.PersistentData(this));
        });

        // Setup available migrators - todo
        initialize("data migrators/converters", (plugin) -> {
//            availableMigrators.add(new LegacyMigrator(this));
//            if (isDependencyLoaded("MySqlPlayerDataBridge")) {
//                availableMigrators.add(new MpdbMigrator(this));
//            }
            legacyConverter = new BukkitLegacyConverter(this);
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

        // Register events
        initialize("events", (plugin) -> this.eventListener = new BukkitEventListener(this));

        // Register commands
        initialize("commands", (plugin) -> BukkitCommand.Type.registerCommands(this));

        // Register plugin hooks
        initialize("hooks", (plugin) -> {
            if (isDependencyLoaded("Plan")) {
                new PlanHook(this).hookIntoPlan();
            }
        });

        // Hook into bStats and check for updates
        initialize("metrics", (plugin) -> this.registerMetrics(METRICS_ID));
        this.checkForUpdates();
    }

    @Override
    public void onDisable() {
        if (this.eventListener != null) {
            this.eventListener.handlePluginDisable();
        }
        log(Level.INFO, "Successfully disabled HuskSync v" + getPluginVersion());
    }

    @Override
    @NotNull
    public Set<OnlineUser> getOnlineUsers() {
        return Bukkit.getOnlinePlayers().stream()
                .map(player -> BukkitUser.adapt(player, this))
                .collect(Collectors.toSet());
    }

    @Override
    @NotNull
    public Optional<OnlineUser> getOnlineUser(@NotNull UUID uuid) {
        final Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return Optional.empty();
        }
        return Optional.of(BukkitUser.adapt(player, this));
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

    @NotNull
    @Override
    public DataAdapter getDataAdapter() {
        return dataAdapter;
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public Map<Identifier, Serializer<? extends Data>> getSerializers() {
        return serializers;
    }

    @NotNull
    @Override
    public List<Migrator> getAvailableMigrators() {
        return availableMigrators;
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
        return Bukkit.getPluginManager().getPlugin(name) != null;
    }

    // Register bStats metrics
    public void registerMetrics(int metricsId) {
        if (!getPluginVersion().getMetadata().isBlank()) {
            return;
        }

        try {
            new Metrics(this, metricsId);
        } catch (Throwable e) {
            log(Level.WARNING, "Failed to register bStats metrics (" + e.getMessage() + ")");
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
    public ConsoleUser getConsole() {
        return new ConsoleUser(audiences.console());
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

    @NotNull
    @Override
    public String getPlatformType() {
        return PLATFORM_TYPE_ID;
    }

    @Override
    public Optional<LegacyConverter> getLegacyConverter() {
        return Optional.of(legacyConverter);
    }

    @NotNull
    @Override
    public Set<UUID> getLockedPlayers() {
        return this.eventListener.getLockedPlayers();
    }

    @NotNull
    @Override
    public Gson getGson() {
        return gson;
    }

    @NotNull
    public Map<Integer, MapView> getMapViews() {
        return mapViews;
    }

    @NotNull
    public GracefulScheduling getScheduler() {
        return paperLib.scheduling();
    }

    @NotNull
    public BukkitAudiences getAudiences() {
        return audiences;
    }

    @NotNull
    public CommandRegistration getCommandRegistrar() {
        return paperLib.commandRegistration();
    }

    @Override
    @NotNull
    public HuskSync getPlugin() {
        return this;
    }

}
