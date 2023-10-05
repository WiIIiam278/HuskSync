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

public class LockstepDataSyncer extends DataSyncer {

    public LockstepDataSyncer(@NotNull HuskSync plugin) {
        super(plugin);
    }

    @Override
    public void initialize() {
        plugin.getRedisManager().clearUsersCheckedOutOnServer();
    }

    @Override
    public void terminate() {
        plugin.getRedisManager().clearUsersCheckedOutOnServer();
    }

    // Consume their data when they are checked in
    @Override
    public void setUserData(@NotNull OnlineUser user) {
        this.listenForRedisData(user, () -> {
            if (plugin.getRedisManager().getUserCheckedOut(user).isEmpty()) {
                plugin.getRedisManager().setUserCheckedOut(user, true);
                plugin.getRedisManager().getUserData(user).ifPresentOrElse(
                        data -> user.applySnapshot(data, DataSnapshot.UpdateCause.SYNCHRONIZED),
                        () -> this.setUserFromDatabase(user)
                );
                return true;
            }
            return false;
        });
    }

    @Override
    public void saveUserData(@NotNull OnlineUser user) {
        plugin.runAsync(() -> {
            final DataSnapshot.Packed data = user.createSnapshot(DataSnapshot.SaveCause.DISCONNECT);
            plugin.getRedisManager().setUserData(user, data);
            plugin.getRedisManager().setUserCheckedOut(user, false);
            plugin.getDatabase().addSnapshot(user, data);
        });
    }

}
