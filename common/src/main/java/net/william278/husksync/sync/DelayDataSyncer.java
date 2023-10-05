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

package net.william278.husksync.sync;

import net.william278.husksync.HuskSync;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

/**
 * A data syncer which applies a network delay before checking the presence of user data
 */
public class DelayDataSyncer extends DataSyncer {

    public DelayDataSyncer(@NotNull HuskSync plugin) {
        super(plugin);
    }

    @Override
    public void setUserData(@NotNull OnlineUser user) {
        plugin.runAsyncDelayed(
                () -> {
                    // Fetch from the database if the user isn't changing servers
                    if (!plugin.getRedisManager().getUserServerSwitch(user)) {
                        this.setUserFromDatabase(user);
                        return;
                    }

                    // Listen for the data to be updated
                    this.listenForRedisData(
                            user,
                            () -> plugin.getRedisManager().getUserData(user).map(data -> {
                                user.applySnapshot(data, DataSnapshot.UpdateCause.SYNCHRONIZED);
                                return true;
                            }).orElse(false)
                    );
                },
                Math.max(0, plugin.getSettings().getNetworkLatencyMilliseconds() / 50L)
        );
    }

    @Override
    public void saveUserData(@NotNull OnlineUser user) {
        plugin.runAsync(() -> {
            plugin.getRedisManager().setUserServerSwitch(user);
            final DataSnapshot.Packed data = user.createSnapshot(DataSnapshot.SaveCause.DISCONNECT);
            plugin.getRedisManager().setUserData(user, data);
            plugin.getDatabase().addSnapshot(user, data);
        });
    }

}
