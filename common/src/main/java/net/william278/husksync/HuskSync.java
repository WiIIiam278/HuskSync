package net.william278.husksync;

import net.william278.husksync.config.Locales;
import net.william278.husksync.config.Settings;
import net.william278.husksync.data.DataAdapter;
import net.william278.husksync.editor.DataEditor;
import net.william278.husksync.database.Database;
import net.william278.husksync.event.EventCannon;
import net.william278.husksync.migrator.Migrator;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.redis.RedisManager;
import net.william278.husksync.util.Logger;
import net.william278.husksync.util.ResourceReader;
import net.william278.husksync.util.Version;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract implementation of the HuskSync plugin.
 */
public interface HuskSync {

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
     * Returns the data editor implementation
     *
     * @return the {@link DataEditor} implementation
     */
    @NotNull
    DataEditor getDataEditor();

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
     * Returns the plugin {@link Logger}
     *
     * @return the {@link Logger}
     */
    @NotNull
    Logger getLoggingAdapter();

    /**
     * Returns the plugin resource file reader
     *
     * @return the {@link ResourceReader}
     */
    @NotNull
    ResourceReader getResourceReader();

    /**
     * Returns the plugin version
     *
     * @return the plugin {@link Version}
     */
    @NotNull
    Version getPluginVersion();

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

}
