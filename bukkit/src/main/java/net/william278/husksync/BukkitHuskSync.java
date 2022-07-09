package net.william278.husksync;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
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
import net.william278.husksync.util.*;
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
import java.util.logging.Level;
import java.util.stream.Collectors;

public class BukkitHuskSync extends JavaPlugin implements HuskSync {

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
        // Process initialization stages
        CompletableFuture.supplyAsync(() -> {
            // Set the logging adapter and resource reader
            this.logger = new BukkitLogger(this.getLogger());
            this.resourceReader = new BukkitResourceReader(this);

            // Load settings and locales
            getLoggingAdapter().log(Level.INFO, "Loading plugin configuration settings & locales...");
            return reload().thenApply(loadedSettings -> {
                if (loadedSettings) {
                    logger.showDebugLogs(settings.getBooleanValue(Settings.ConfigOption.DEBUG_LOGGING));
                    getLoggingAdapter().log(Level.INFO, "Successfully loaded plugin configuration settings & locales");
                } else {
                    getLoggingAdapter().log(Level.SEVERE, "Failed to load plugin configuration settings and/or locales");
                }
                return loadedSettings;
            }).join();
        }).thenApply(succeeded -> {
            // Prepare data adapter
            if (succeeded) {
                if (settings.getBooleanValue(Settings.ConfigOption.SYNCHRONIZATION_COMPRESS_DATA)) {
                    dataAdapter = new CompressedDataAdapter();
                } else {
                    dataAdapter = new JsonDataAdapter();
                }
            }
            return succeeded;
        }).thenApply(succeeded -> {
            // Prepare event cannon
            if (succeeded) {
                eventCannon = new BukkitEventCannon();
            }
            return succeeded;
        }).thenApply(succeeded -> {
            // Prepare data editor
            if (succeeded) {
                dataEditor = new DataEditor(locales);
            }
            return succeeded;
        }).thenApply(succeeded -> {
            // Prepare migrators
            if (succeeded) {
                availableMigrators = new ArrayList<>();
                availableMigrators.add(new LegacyMigrator(this));
                final Plugin mySqlPlayerDataBridge = Bukkit.getPluginManager().getPlugin("MySqlPlayerDataBridge");
                if (mySqlPlayerDataBridge != null) {
                    availableMigrators.add(new MpdbMigrator(this, mySqlPlayerDataBridge));
                }
            }
            return succeeded;
        }).thenApply(succeeded -> {
            // Establish connection to the database
            if (succeeded) {
                this.database = new MySqlDatabase(settings, resourceReader, logger, dataAdapter, eventCannon);
                getLoggingAdapter().log(Level.INFO, "Attempting to establish connection to the database...");
                final CompletableFuture<Boolean> databaseConnectFuture = new CompletableFuture<>();
                Bukkit.getScheduler().runTask(this, () -> {
                    final boolean initialized = this.database.initialize();
                    if (!initialized) {
                        getLoggingAdapter().log(Level.SEVERE, "Failed to establish a connection to the database. " + "Please check the supplied database credentials in the config file");
                        databaseConnectFuture.completeAsync(() -> false);
                        return;
                    }
                    getLoggingAdapter().log(Level.INFO, "Successfully established a connection to the database");
                    databaseConnectFuture.completeAsync(() -> true);
                });
                return databaseConnectFuture.join();
            }
            return false;
        }).thenApply(succeeded -> {
            // Establish connection to the Redis server
            if (succeeded) {
                this.redisManager = new RedisManager(this);
                getLoggingAdapter().log(Level.INFO, "Attempting to establish connection to the Redis server...");
                return this.redisManager.initialize().thenApply(initialized -> {
                    if (!initialized) {
                        getLoggingAdapter().log(Level.SEVERE, "Failed to establish a connection to the Redis server. " + "Please check the supplied Redis credentials in the config file");
                        return false;
                    }
                    getLoggingAdapter().log(Level.INFO, "Successfully established a connection to the Redis server");
                    return true;
                }).join();
            }
            return false;
        }).thenApply(succeeded -> {
            // Register events
            if (succeeded) {
                getLoggingAdapter().log(Level.INFO, "Registering events...");
                this.eventListener = new BukkitEventListener(this);
                getLoggingAdapter().log(Level.INFO, "Successfully registered events listener");
            }
            return succeeded;
        }).thenApply(succeeded -> {
            // Register permissions
            if (succeeded) {
                getLoggingAdapter().log(Level.INFO, "Registering permissions & commands...");
                Arrays.stream(Permission.values()).forEach(permission -> getServer().getPluginManager().addPermission(new org.bukkit.permissions.Permission(permission.node, switch (permission.defaultAccess) {
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
            }
            return succeeded;
        }).thenApply(succeeded -> {
            if (succeeded && Bukkit.getPluginManager().getPlugin("Plan") != null) {
                getLoggingAdapter().log(Level.INFO, "Enabling Plan integration...");
                new PlanHook(database, logger).hookIntoPlan();
                getLoggingAdapter().log(Level.INFO, "Plan integration enabled!");
            }
            return succeeded;
        }).thenApply(succeeded -> {
            // Check for updates
            if (succeeded && settings.getBooleanValue(Settings.ConfigOption.CHECK_FOR_UPDATES)) {
                getLoggingAdapter().log(Level.INFO, "Checking for updates...");
                new UpdateChecker(getVersion(), getLoggingAdapter()).logToConsole();
            }
            return succeeded;
        }).thenAccept(succeeded -> {
            // Handle failed initialization
            if (!succeeded) {
                getLoggingAdapter().log(Level.SEVERE, "Failed to initialize HuskSync. " + "The plugin will now be disabled");
                getServer().getPluginManager().disablePlugin(this);
            } else {
                getLoggingAdapter().log(Level.INFO, "Successfully enabled HuskSync v" + getVersion());
            }
        });
    }

    @Override
    public void onDisable() {
        if (this.eventListener != null) {
            this.eventListener.handlePluginDisable();
        }
        getLoggingAdapter().log(Level.INFO, "Successfully disabled HuskSync v" + getVersion());
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

    @Override
    public @NotNull String getVersion() {
        return getDescription().getVersion();
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
