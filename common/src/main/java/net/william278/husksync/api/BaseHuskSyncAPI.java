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

package net.william278.husksync.api;

import net.william278.husksync.HuskSync;
import net.william278.husksync.data.DataSaveCause;
import net.william278.husksync.data.UserData;
import net.william278.husksync.data.UserDataSnapshot;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.player.User;
import org.jetbrains.annotations.ApiStatus;
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

    /**
     * <b>(Internal use only)</b> - Constructor, instantiating the base API class.
     */
    @ApiStatus.Internal
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
        return plugin.supplyAsync(() -> plugin.getDatabase().getUser(uuid));
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
        return plugin.supplyAsync(() -> plugin.getDatabase().getUserByName(username));
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
        return plugin.supplyAsync(() -> {
            if (user instanceof OnlineUser) {
                return ((OnlineUser) user).getUserData(plugin);
            } else {
                return plugin.getDatabase().getCurrentUserData(user).map(UserDataSnapshot::userData);
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
        final CompletableFuture<Void> future = new CompletableFuture<>();
        plugin.runAsync(() -> {
            plugin.getDatabase().setUserData(user, userData, DataSaveCause.API);
            plugin.getRedisManager().sendUserDataUpdate(user, userData);
            future.complete(null);
        });
        return future;
    }

    /**
     * Saves the {@link UserData} of an {@link OnlineUser} to the database
     *
     * @param user the {@link OnlineUser} to save the {@link UserData} of
     * @return future returning void when complete
     * @since 2.0
     */
    public final CompletableFuture<Void> saveUserData(@NotNull OnlineUser user) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        plugin.runAsync(() -> user.getUserData(plugin).ifPresentOrElse(userData -> {
            plugin.getDatabase().setUserData(user, userData, DataSaveCause.API);
            future.complete(null);
        }, () -> future.completeExceptionally(new IllegalStateException("User data not present"))));
        return future;
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
        return plugin.supplyAsync(() -> plugin.getDatabase().getUserData(user));
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

    /**
     * An exception indicating the plugin has been accessed before it has been registered.
     */
    static final class NotRegisteredException extends IllegalStateException {

        private static final String MESSAGE = """
                Could not access the HuskSync API as it has not yet been registered. This could be because:
                1) HuskSync has failed to enable successfully
                2) Your plugin isn't set to load after HuskSync has
                   (Check if it set as a (soft)depend in plugin.yml or to load: BEFORE in paper-plugin.yml?)
                3) You are attempting to access HuskSync on plugin construction/before your plugin has enabled.
                4) You have shaded HuskSync into your plugin jar and need to fix your maven/gradle/build script
                   to only include HuskSync as a dependency and not as a shaded dependency.""";

        NotRegisteredException() {
            super(MESSAGE);
        }

    }
    
}
