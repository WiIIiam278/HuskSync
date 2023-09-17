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

import net.william278.husksync.BukkitHuskSync;
import net.william278.husksync.user.BukkitUser;
import net.william278.husksync.user.OnlineUser;
import net.william278.husksync.user.User;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * The HuskSync API implementation for the Bukkit platform
 * </p>
 * Retrieve an instance of the API class via {@link #getInstance()}.
 */
@SuppressWarnings("unused")
public class HuskSyncAPI extends BaseHuskSyncAPI {

    // Instance of the plugin
    private static HuskSyncAPI instance;

    /**
     * <b>(Internal use only)</b> - Constructor, instantiating the API.
     */
    @ApiStatus.Internal
    private HuskSyncAPI(@NotNull BukkitHuskSync plugin) {
        super(plugin);
    }

    /**
     * Entrypoint to the HuskSync API - returns an instance of the API
     *
     * @return instance of the HuskSync API
     */
    @NotNull
    public static HuskSyncAPI getInstance() {
        if (instance == null) {
            throw new NotRegisteredException();
        }
        return instance;
    }

    /**
     * <b>(Internal use only)</b> - Register the API for this platform.
     *
     * @param plugin the plugin instance
     */
    @ApiStatus.Internal
    public static void register(@NotNull BukkitHuskSync plugin) {
        instance = new HuskSyncAPI(plugin);
    }

    /**
     * <b>(Internal use only)</b> - Unregister the API for this platform.
     */
    @ApiStatus.Internal
    public static void unregister() {
        instance = null;
    }

    /**
     * Returns a {@link User} instance for the given bukkit {@link Player}.
     *
     * @param player the bukkit player to get the {@link User} instance for
     * @return the {@link User} instance for the given bukkit {@link Player}
     * @since 2.0
     */
    @NotNull
    public OnlineUser getUser(@NotNull Player player) {
        return BukkitUser.adapt(player, plugin);
    }


}
