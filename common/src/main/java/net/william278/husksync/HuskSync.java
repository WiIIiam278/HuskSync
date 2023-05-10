package net.william278.husksync;

import net.william278.desertwell.UpdateChecker;
import net.william278.husksync.config.Locales;
import net.william278.husksync.config.Settings;
import net.william278.husksync.data.DataAdapter;
import net.william278.husksync.database.Database;
import net.william278.husksync.event.EventCannon;
import net.william278.husksync.migrator.Migrator;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.redis.RedisManager;
import net.william278.husksync.util.TaskRunner;
import net.william278.desertwell.Version;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Abstract implementation of the HuskSync plugin.
 */
public interface HuskSync extends TaskRunner {

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
     * Returns the data adapter implementation
     *
     * @return the {@link DataAdapter} implementation
     */
    @NotNull
    DataAdapter getDataAdapter();

    /**
     * Returns the event firing cannon
     *
     * @return the {@link EventCannon} implementation
     */
    @NotNull
    EventCannon getEventCannon();

    /**
     * Returns a list of available data {@link Migrator}s
     *
     * @return a list of {@link Migrator}s
     */
    @NotNull
    List<Migrator> getAvailableMigrators();

    /**
     * Returns the plugin {@link Settings}
     *
     * @return the {@link Settings}
     */
    @NotNull
    Settings getSettings();

    /**
     * Returns the plugin {@link Locales}
     *
     * @return the {@link Locales}
     */
    @NotNull
    Locales getLocales();

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
        if (getSettings().doDebugLogging()) {
            log(Level.INFO, "[DEBUG] " + message, throwable);
        }
    }

    /**
     * Returns the plugin version
     *
     * @return the plugin {@link Version}
     */
    @NotNull
    Version getPluginVersion();

    /**
     * Returns the plugin data folder
     *
     * @return the plugin data folder as a {@link File}
     */
    @NotNull
    File getDataFolder();

    /**
     * Returns a future returning the latest plugin {@link Version} if the plugin is out-of-date
     *
     * @return a {@link CompletableFuture} returning the latest {@link Version} if the current one is out-of-date
     */
    default CompletableFuture<Optional<Version>> getLatestVersionIfOutdated() {
        final UpdateChecker updateChecker = UpdateChecker.create(getPluginVersion(), SPIGOT_RESOURCE_ID);
        return updateChecker.isUpToDate().thenApply(upToDate -> {
            if (upToDate) {
                return Optional.empty();
            } else {
                return Optional.of(updateChecker.getLatestVersion().join());
            }
        });
    }

    /**
     * Returns the Minecraft version implementation
     *
     * @return the Minecraft {@link Version}
     */
    @NotNull
    Version getMinecraftVersion();

    /**
     * Reloads the {@link Settings} and {@link Locales} from their respective config files
     *
     * @return a {@link CompletableFuture} that will be completed when the plugin reload is complete and if it was successful
     */
    CompletableFuture<Boolean> reload();

    Set<UUID> getLockedPlayers();

}
