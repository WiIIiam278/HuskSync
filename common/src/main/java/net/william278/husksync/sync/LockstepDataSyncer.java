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

import java.util.Optional;

public class LockstepDataSyncer extends DataSyncer {

    public LockstepDataSyncer(@NotNull HuskSync plugin) {
        super(plugin);
    }

    @Override
    public void initialize() {
        getRedis().clearUsersCheckedOutOnServer();
    }

    @Override
    public void terminate() {
        getRedis().clearUsersCheckedOutOnServer();
    }

    // Consume their data when they are checked in
    @Override
    public void syncApplyUserData(@NotNull OnlineUser user) {
        this.listenForRedisData(user, () -> {
            if (user.cannotApplySnapshot()) {
                plugin.debug("Not checking data state for user who has gone offline: %s".formatted(user.getName()));
                return false;
            }

            // If they are checked out, ask the server to check them back in and return false
            final Optional<String> server = getRedis().getUserCheckedOut(user);
            if (server.isPresent() && !server.get().equals(plugin.getServerName())) {
                getRedis().petitionServerCheckin(server.get(), user);
                return false;
            }

            // If they are checked in - or checked out on *this* server - we can apply their latest data
            getRedis().setUserCheckedOut(user, true);
            getRedis().getUserData(user).ifPresentOrElse(
                    data -> user.applySnapshot(data, DataSnapshot.UpdateCause.SYNCHRONIZED),
                    () -> this.setUserFromDatabase(user)
            );
            return true;
        });
    }

    @Override
    public void syncSaveUserData(@NotNull OnlineUser onlineUser) {
        plugin.runAsync(() -> saveData(
                onlineUser, onlineUser.createSnapshot(DataSnapshot.SaveCause.DISCONNECT),
                (user, data) -> {
                    getRedis().setUserData(user, data);
                    getRedis().setUserCheckedOut(user, false);
                    plugin.unlockPlayer(user.getUuid());
                }
        ));
    }

}
