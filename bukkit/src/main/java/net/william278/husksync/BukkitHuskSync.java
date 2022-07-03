package net.william278.husksync;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import net.william278.husksync.command.BukkitCommand;
import net.william278.husksync.command.CommandBase;
import net.william278.husksync.command.HuskSyncCommand;
import net.william278.husksync.command.Permission;
import net.william278.husksync.config.Locales;
import net.william278.husksync.config.Settings;
import net.william278.husksync.database.Database;
import net.william278.husksync.database.MySqlDatabase;
import net.william278.husksync.listener.BukkitEventListener;
import net.william278.husksync.listener.EventListener;
import net.william278.husksync.player.BukkitPlayer;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.redis.RedisManager;
import net.william278.husksync.util.*;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
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

    private Settings settings;

    private Locales locales;

    private static BukkitHuskSync instance;

    public static BukkitHuskSync getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
        /*getLogger().log(Level.INFO, "Loading runtime libraries...");
        final BukkitLibraryManager libraryManager = new BukkitLibraryManager(this);
        final Library[] libraries = new Library[]{
                Library.builder().groupId("redis{}clients")
                        .artifactId("jedis")
                        .version("4.2.3")
                        .id("jedis")
                        .build()
        };
        libraryManager.addMavenCentral();
        Arrays.stream(libraries).forEach(libraryManager::loadLibrary);
        getLogger().log(Level.INFO, "Successfully loaded runtime libraries.");*/
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
                    getLoggingAdapter().log(Level.INFO, "Successfully loaded plugin configuration settings & locales");
                } else {
                    getLoggingAdapter().log(Level.SEVERE, "Failed to load plugin configuration settings and/or locales");
                }
                return loadedSettings;
            }).join();
        }).thenApply(succeeded -> {
            // Establish connection to the database
            this.database = new MySqlDatabase(settings, resourceReader, logger);
            if (succeeded) {
                getLoggingAdapter().log(Level.INFO, "Attempting to establish connection to the database...");
                final CompletableFuture<Boolean> databaseConnectFuture = new CompletableFuture<>();
                Bukkit.getScheduler().runTask(this, () -> {
                    final boolean initialized = this.database.initialize();
                    if (!initialized) {
                        getLoggingAdapter().log(Level.SEVERE, "Failed to establish a connection to the database. "
                                + "Please check the supplied database credentials in the config file");
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
            this.redisManager = new RedisManager(settings);
            if (succeeded) {
                getLoggingAdapter().log(Level.INFO, "Attempting to establish connection to the Redis server...");
                return this.redisManager.initialize().thenApply(initialized -> {
                    if (!initialized) {
                        getLoggingAdapter().log(Level.SEVERE, "Failed to establish a connection to the Redis server. "
                                + "Please check the supplied Redis credentials in the config file");
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
                final CommandBase[] commands = new CommandBase[]{new HuskSyncCommand(this)};
                for (CommandBase commandBase : commands) {
                    final PluginCommand pluginCommand = getCommand(commandBase.command);
                    if (pluginCommand != null) {
                        new BukkitCommand(commandBase, this).register(pluginCommand);
                    }
                }
                getLoggingAdapter().log(Level.INFO, "Successfully registered permissions & commands");
            }
            return succeeded;
        }).thenApply(succeeded -> {
            // Check for updates
            if (settings.getBooleanValue(Settings.ConfigOption.CHECK_FOR_UPDATES) && succeeded) {
                getLoggingAdapter().log(Level.INFO, "Checking for updates...");
                new UpdateChecker(getVersion(), getLoggingAdapter()).logToConsole();
            }
            return succeeded;
        }).thenAccept(succeeded -> {
            // Handle failed initialization
            if (!succeeded) {
                getLoggingAdapter().log(Level.SEVERE, "Failed to initialize HuskSync. " +
                        "The plugin will now be disabled");
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
