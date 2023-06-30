package net.william278.husksync;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.minecraft.server.MinecraftServer;
import net.william278.annotaml.Annotaml;
import net.william278.desertwell.util.Version;
import net.william278.husksync.config.Locales;
import net.william278.husksync.config.Settings;
import net.william278.husksync.data.CompressedDataAdapter;
import net.william278.husksync.data.DataAdapter;
import net.william278.husksync.data.JsonDataAdapter;
import net.william278.husksync.database.Database;
import net.william278.husksync.database.MySqlDatabase;
import net.william278.husksync.event.EventCannon;
import net.william278.husksync.event.FabricEventCannon;
import net.william278.husksync.listener.EventListener;
import net.william278.husksync.listener.FabricEventListener;
import net.william278.husksync.migrator.Migrator;
import net.william278.husksync.player.FabricPlayer;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.redis.RedisManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class FabricHuskSync implements DedicatedServerModInitializer, HuskSync {
    public static Logger LOGGER;
    public static ModContainer MOD;
    public static MinecraftServer SERVER;
    public static FabricHuskSync INSTANCE;
    private Database database;
    private RedisManager redisManager;
    private EventListener eventListener;
    private DataAdapter dataAdapter;
    private EventCannon eventCannon;
    private Settings settings;
    private Locales locales;

    private FabricServerAudiences audiences;

    public FabricHuskSync() {
        INSTANCE = this;
    }

    @Override
    @SuppressWarnings("SpellCheckingInspection")
    public void onInitializeServer() {
        // static field
        FabricHuskSync.LOGGER = LoggerFactory.getLogger("HuskSync");
        FabricHuskSync.MOD = FabricLoader.getInstance().getModContainer("husksync").orElseThrow();

        // mixin

        // load HuskSync after server started
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            FabricHuskSync.SERVER = server;
            this.onEnable();
        });

        // unload HuskSync before server stopped
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> this.onDisable());
    }

    ///////////////////////// Mock Bukkit API /////////////////////////

    private void onEnable() {
        final AtomicBoolean initialized = new AtomicBoolean(true);

        try {
            // Create adventure audience
            this.audiences = FabricServerAudiences.of(FabricHuskSync.SERVER);

            // Load settings and locales
            log(Level.INFO, "Loading mod configuration settings & locales...");
            initialized.set(reload().join());
            if (initialized.get()) {
                log(Level.INFO, "Successfully loaded mod configuration settings & locales");
            } else {
                throw new HuskSyncInitializationException(
                        "Failed to load mod configuration settings and/or locales"
                );
            }

            // Prepare data adapter
            if (this.settings.doCompressData()) {
                this.dataAdapter = new CompressedDataAdapter();
            } else {
                this.dataAdapter = new JsonDataAdapter();
            }

            // Prepare event cannon
            this.eventCannon = new FabricEventCannon();

            // FIXME: The migration function is not supported.

            // Prepare database connection
            this.database = new MySqlDatabase(this);
            log(Level.INFO, "Attempting to establish connection to the database...");
            initialized.set(this.database.initialize());
            if (initialized.get()) {
                log(Level.INFO, "Successfully established a connection to the database");
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

            // Register event handler
            log(Level.INFO, "Registering events...");
            this.eventListener = new FabricEventListener(this);
            log(Level.INFO, "Successfully registered events listener");

            // Register permissions
            log(Level.INFO, "Registering permissions & commands...");

            // Register commands handler
            log(Level.INFO, "Successfully registered permissions & commands");

            // FIXME: The plan function is not supported.
            // FIXME: The bStats metrics function is not supported.
            // FIXME: The updates function is not supported.
        } catch (HuskSyncInitializationException exception) {
            log(Level.SEVERE, """
                                            
                    ***************************************************
                               
                              Failed to initialize HuskSync!
                               
                    ***************************************************
                    The mod was disabled due to an error. Please check
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
                log(Level.SEVERE, "Failed to initialize HuskSync. The mod will now be disabled");
            }
        }
    }

    private void onDisable() {
        if (this.eventListener != null) this.eventListener.handlePluginDisable();
        if (this.database != null) this.database.close();
        if (this.redisManager != null) this.redisManager.close();
        if (this.audiences != null) this.audiences.close();
    }

    ///////////////////////// GETTER /////////////////////////

    public @NotNull FabricServerAudiences getAudiences() {
        return audiences;
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

    @Override
    public @NotNull Settings getSettings() {
        return settings;
    }

    @Override
    public @NotNull Locales getLocales() {
        return locales;
    }

    ///////////////////////// Method /////////////////////////

    @Override
    public @NotNull List<Migrator> getAvailableMigrators() {
        throw new UnsupportedOperationException("The migration function is not supported.");
    }

    @Override
    public @NotNull Set<OnlineUser> getOnlineUsers() {
        return FabricHuskSync.SERVER.getPlayerManager().getPlayerList()
                .stream().map(user -> (OnlineUser) FabricPlayer.adapt(user))
                .collect(Collectors.toSet());
    }

    @Override
    public @NotNull Optional<OnlineUser> getOnlineUser(@NotNull UUID uuid) {
        // TODO getOnlineUser
        return Optional.empty();
    }

    // FIXME: Warning: Fabric mod configuration files are not stored in the same directory as the mod,
    //  which differs from the Bukkit API.
    @Override
    public InputStream getResource(@NotNull String name) throws IOException {
        URL url = this.getClass().getClassLoader().getResource(name);

        if (url == null) {
            return null;
        }

        URLConnection connection = url.openConnection();
        connection.setUseCaches(false);
        return connection.getInputStream();
    }

    @Override
    public void log(@NotNull Level level, @NotNull String message, @NotNull Throwable... throwable) {
        LoggingEventBuilder logEvent = LOGGER.makeLoggingEventBuilder(
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

    @Override
    public @NotNull Version getPluginVersion() {
        return Version.fromString(FabricHuskSync.MOD.getMetadata().getVersion().getFriendlyString(), "");
    }

    @Override
    @SuppressWarnings("SpellCheckingInspection")
    public @NotNull File getDataFolder() throws IOException {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("husksync");

        if (!Files.isDirectory(path)) {
            Files.createDirectory(path);
        }

        return path.toFile();
    }

    @Override
    public @NotNull Version getMinecraftVersion() {
        return Version.fromString("1.20.1", "");
    }

    @Override
    public CompletableFuture<Boolean> reload() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Load mod settings
                this.settings = Annotaml.create(new File(getDataFolder(), "config.yml"), new Settings()).get();

                // Load locales from language preset default
                final Locales languagePresets = Annotaml.create(Locales.class,
                        Objects.requireNonNull(getResource("locales/" + settings.getLanguage() + ".yml"))).get();
                this.locales = Annotaml.create(
                        new File(getDataFolder(),
                                "messages_" + settings.getLanguage() + ".yml"),
                        languagePresets
                ).get();
                return true;
            } catch (IOException | NullPointerException | InvocationTargetException | IllegalAccessException |
                     InstantiationException e) {
                log(Level.SEVERE, "Failed to load data from the config", e);
                return false;
            }
        });
    }

    @Override
    public Set<UUID> getLockedPlayers() {
        // TODO getLockedPlayers
        return null;
    }
}
