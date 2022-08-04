package net.william278.husksync.api;

import net.william278.husksync.HuskSync;
import net.william278.husksync.data.DataSaveCause;
import net.william278.husksync.data.UserData;
import net.william278.husksync.data.UserDataSnapshot;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.player.User;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The base implementation of the HuskSync API, containing cross-platform API calls.
 * </p>
 * This class should not be used directly, but rather through platform-specific extending API classes.
 */
@SuppressWarnings("unused")
public abstract class BaseHuskSyncAPI {

    /**
     * <b>(Internal use only)</b> - Instance of the implementing plugin.
     */
    protected final HuskSync plugin;

    protected BaseHuskSyncAPI(@NotNull HuskSync plugin) {
        this.plugin = plugin;
    }

    /**
     * Returns a {@link User} by the given player's account {@link UUID}, if they exist.
     *
     * @param uuid the unique id of the player to get the {@link User} instance for
     * @return future returning the {@link User} instance for the given player's unique id if they exist, otherwise an empty {@link Optional}
     * @apiNote The player does not have to be online
     * @since 2.0
     */
    public final CompletableFuture<Optional<User>> getUser(@NotNull UUID uuid) {
        return plugin.getDatabase().getUser(uuid);
    }

    /**
     * Returns a {@link User} by the given player's username (case-insensitive), if they exist.
     *
     * @param username the username of the {@link User} instance for
     * @return future returning the {@link User} instance for the given player's username if they exist,
     * otherwise an empty {@link Optional}
     * @apiNote The player does not have to be online, though their username has to be the username
     * they had when they last joined the server.
     * @since 2.0
     */
    public final CompletableFuture<Optional<User>> getUser(@NotNull String username) {
        return plugin.getDatabase().getUserByName(username);
    }

    /**
     * Returns a {@link User}'s current {@link UserData}
     *
     * @param user the {@link User} to get the {@link UserData} for
     * @return future returning the {@link UserData} for the given {@link User} if they exist, otherwise an empty {@link Optional}
     * @apiNote If the user is not online on the implementing bukkit server,
     * the {@link UserData} returned will be their last database-saved UserData.
     * </p>
     * Because of this, if the user is online on another server on the network,
     * then the {@link UserData} returned by this method will <i>not necessarily reflective of
     * their current state</i>
     * @since 2.0
     */
    public final CompletableFuture<Optional<UserData>> getUserData(@NotNull User user) {
        return CompletableFuture.supplyAsync(() -> {
            if (user instanceof OnlineUser) {
                return ((OnlineUser) user).getUserData(plugin.getLoggingAdapter(), plugin.getSettings()).join();
            } else {
                return plugin.getDatabase().getCurrentUserData(user).join().map(UserDataSnapshot::userData);
            }
        });
    }

    /**
     * Sets the {@link UserData} to the database for the given {@link User}.
     * </p>
     * If the user is online and on the same cluster, their data will be updated in game.
     *
     * @param user     the {@link User} to set the {@link UserData} for
     * @param userData the {@link UserData} to set for the given {@link User}
     * @return future returning void when complete
     * @since 2.0
     */
    public final CompletableFuture<Void> setUserData(@NotNull User user, @NotNull UserData userData) {
        return CompletableFuture.runAsync(() ->
                plugin.getDatabase().setUserData(user, userData, DataSaveCause.API)
                        .thenRun(() -> plugin.getRedisManager().sendUserDataUpdate(user, userData).join()));
    }

    /**
     * Saves the {@link UserData} of an {@link OnlineUser} to the database
     *
     * @param user the {@link OnlineUser} to save the {@link UserData} of
     * @return future returning void when complete
     * @since 2.0
     */
    public final CompletableFuture<Void> saveUserData(@NotNull OnlineUser user) {
        return CompletableFuture.runAsync(() -> user.getUserData(plugin.getLoggingAdapter(), plugin.getSettings())
                .thenAccept(optionalUserData -> optionalUserData.ifPresent(
                        userData -> plugin.getDatabase().setUserData(user, userData, DataSaveCause.API).join())));
    }

    /**
     * Returns the saved {@link UserDataSnapshot} records for the given {@link User}
     *
     * @param user the {@link User} to get the {@link UserDataSnapshot} for
     * @return future returning a list {@link UserDataSnapshot} for the given {@link User} if they exist,
     * otherwise an empty {@link Optional}
     * @apiNote The length of the list of VersionedUserData will correspond to the configured
     * {@code max_user_data_records} config option
     * @since 2.0
     */
    public final CompletableFuture<List<UserDataSnapshot>> getSavedUserData(@NotNull User user) {
        return CompletableFuture.supplyAsync(() -> plugin.getDatabase().getUserData(user).join());
    }

    /**
     * Returns the JSON string representation of the given {@link UserData}
     *
     * @param userData    the {@link UserData} to get the JSON string representation of
     * @param prettyPrint whether to pretty print the JSON string
     * @return the JSON string representation of the given {@link UserData}
     * @since 2.0
     */
    @NotNull
    public final String getUserDataJson(@NotNull UserData userData, boolean prettyPrint) {
        return plugin.getDataAdapter().toJson(userData, prettyPrint);
    }

}
