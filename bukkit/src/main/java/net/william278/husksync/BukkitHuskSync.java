package net.william278.husksync;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import net.william278.desertwell.Version;
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
import net.william278.husksync.editor.DataEditor;
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
import net.william278.husksync.util.BukkitLogger;
import net.william278.husksync.util.BukkitResourceReader;
import net.william278.husksync.util.Logger;
import net.william278.husksync.util.ResourceReader;
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
    private Logger logger;
    private ResourceReader resourceReader;
    private EventListener eventListener;
    private DataAdapter dataAdapter;
    private DataEditor dataEditor;
    private EventCannon eventCannon;
    private Settings settings;
    private Locales locales;
    private List<Migrator> availableMigrators;
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
            // Set the logging adapter and resource reader
            this.logger = new BukkitLogger(this.getLogger());
            this.resourceReader = new BukkitResourceReader(this);

            // Load settings and locales
            getLoggingAdapter().log(Level.INFO, "Loading plugin configuration settings & locales...");
            initialized.set(reload().join());
            if (initialized.get()) {
                logger.showDebugLogs(settings.getBooleanValue(Settings.ConfigOption.DEBUG_LOGGING));
                getLoggingAdapter().log(Level.INFO, "Successfully loaded plugin configuration settings & locales");
            } else {
                throw new HuskSyncInitializationException("Failed to load plugin configuration settings and/or locales");
            }

            // Prepare data adapter
            if (settings.getBooleanValue(Settings.ConfigOption.SYNCHRONIZATION_COMPRESS_DATA)) {
                dataAdapter = new CompressedDataAdapter();
            } else {
                dataAdapter = new JsonDataAdapter();
            }

            // Prepare event cannon
            eventCannon = new BukkitEventCannon();

            // Prepare data editor
            dataEditor = new DataEditor(locales);

            // Prepare migrators
            availableMigrators = new ArrayList<>();
            availableMigrators.add(new LegacyMigrator(this));
            final Plugin mySqlPlayerDataBridge = Bukkit.getPluginManager().getPlugin("MySqlPlayerDataBridge");
            if (mySqlPlayerDataBridge != null) {
                availableMigrators.add(new MpdbMigrator(this, mySqlPlayerDataBridge));
            }

            // Prepare database connection
            this.database = new MySqlDatabase(settings, resourceReader, logger, dataAdapter, eventCannon);
            getLoggingAdapter().log(Level.INFO, "Attempting to establish connection to the database...");
            initialized.set(this.database.initialize());
            if (initialized.get()) {
                getLoggingAdapter().log(Level.INFO, "Successfully established a connection to the database");
            } else {
                throw new HuskSyncInitializationException("Failed to establish a connection to the database. " +
                        "Please check the supplied database credentials in the config file");
            }

            // Prepare redis connection
            this.redisManager = new RedisManager(this);
            getLoggingAdapter().log(Level.INFO, "Attempting to establish connection to the Redis server...");
            initialized.set(this.redisManager.initialize().join());
            if (initialized.get()) {
                getLoggingAdapter().log(Level.INFO, "Successfully established a connection to the Redis server");
            } else {
                throw new HuskSyncInitializationException("Failed to establish a connection to the Redis server. " +
                        "Please check the supplied Redis credentials in the config file");
            }

            // Register events
            getLoggingAdapter().log(Level.INFO, "Registering events...");
            this.eventListener = new BukkitEventListener(this);
            getLoggingAdapter().log(Level.INFO, "Successfully registered events listener");

            // Register permissions
            getLoggingAdapter().log(Level.INFO, "Registering permissions & commands...");
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
            getLoggingAdapter().log(Level.INFO, "Successfully registered permissions & commands");

            // Hook into plan
            if (Bukkit.getPluginManager().getPlugin("Plan") != null) {
                getLoggingAdapter().log(Level.INFO, "Enabling Plan integration...");
                new PlanHook(database, logger).hookIntoPlan();
                getLoggingAdapter().log(Level.INFO, "Plan integration enabled!");
            }

            // Hook into bStats metrics
            try {
                new Metrics(this, METRICS_ID);
            } catch (final Exception e) {
                getLoggingAdapter().log(Level.WARNING, "Skipped bStats metrics initialization due to an exception.");
            }

            // Check for updates
            if (settings.getBooleanValue(Settings.ConfigOption.CHECK_FOR_UPDATES)) {
                getLoggingAdapter().log(Level.INFO, "Checking for updates...");
                getLatestVersionIfOutdated().thenAccept(newestVersion ->
                        newestVersion.ifPresent(newVersion -> getLoggingAdapter().log(Level.WARNING,
                                "An update is available for HuskSync, v" + newVersion
                                        + " (Currently running v" + getPluginVersion() + ")")));
            }
        } catch (HuskSyncInitializationException exception) {
            getLoggingAdapter().log(Level.SEVERE, """
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
            getLoggingAdapter().log(Level.SEVERE, "An unhandled exception occurred initializing HuskSync!", exception);
            initialized.set(false);
        } finally {
            // Validate initialization
            if (initialized.get()) {
                getLoggingAdapter().log(Level.INFO, "Successfully enabled HuskSync v" + getPluginVersion());
            } else {
                getLoggingAdapter().log(Level.SEVERE, "Failed to initialize HuskSync. The plugin will now be disabled");
                getServer().getPluginManager().disablePlugin(this);
            }
        }
    }

    @Override
    public void onDisable() {
        if (this.eventListener != null) {
            this.eventListener.handlePluginDisable();
        }
        getLoggingAdapter().log(Level.INFO, "Successfully disabled HuskSync v" + getPluginVersion());
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
    public @NotNull DataEditor getDataEditor() {
        return dataEditor;
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
    public @NotNull Logger getLoggingAdapter() {
        return logger;
    }

    @NotNull
    @Override
    public ResourceReader getResourceReader() {
        return resourceReader;
    }

    @NotNull
    @Override
    public Version getPluginVersion() {
        return Version.fromString(getDescription().getVersion(), "-");
    }

    @NotNull
    @Override
    public Version getMinecraftVersion() {
        return Version.fromMinecraftVersionString(Bukkit.getBukkitVersion());
    }

    @Override
    public CompletableFuture<Boolean> reload() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.settings = Settings.load(YamlDocument.create(new File(getDataFolder(), "config.yml"), Objects.requireNonNull(resourceReader.getResource("config.yml")), GeneralSettings.builder().setUseDefaults(false).build(), LoaderSettings.builder().setAutoUpdate(true).build(), DumperSettings.builder().setEncoding(DumperSettings.Encoding.UNICODE).build(), UpdaterSettings.builder().setVersioning(new BasicVersioning("config_version")).build()));

                this.locales = Locales.load(YamlDocument.create(new File(getDataFolder(), "messages-" + settings.getStringValue(Settings.ConfigOption.LANGUAGE) + ".yml"), Objects.requireNonNull(resourceReader.getResource("locales/" + settings.getStringValue(Settings.ConfigOption.LANGUAGE) + ".yml"))));
                return true;
            } catch (IOException | NullPointerException e) {
                getLoggingAdapter().log(Level.SEVERE, "Failed to load data from the config", e);
                return false;
            }
        });
    }
}
