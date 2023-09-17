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

package net.william278.husksync.event;

import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.user.OnlineUser;
import net.william278.husksync.user.User;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public interface BukkitEventDispatcher extends EventDispatcher {

    @Override
    default <T extends Event> boolean fireIsCancelled(@NotNull T event) {
        Bukkit.getPluginManager().callEvent((org.bukkit.event.Event) event);
        return event instanceof Cancellable cancellable && cancellable.isCancelled();
    }

    @NotNull
    @Override
    default PreSyncEvent getPreSyncEvent(@NotNull OnlineUser user, @NotNull DataSnapshot.Packed data) {
        return new BukkitPreSyncEvent(user, data, getPlugin());
    }

    @NotNull
    @Override
    default DataSaveEvent getDataSaveEvent(@NotNull User user, @NotNull DataSnapshot.Packed data) {
        return new BukkitDataSaveEvent(user, data, getPlugin());
    }

    @NotNull
    @Override
    default SyncCompleteEvent getSyncCompleteEvent(@NotNull OnlineUser user) {
        return new BukkitSyncCompleteEvent(user, getPlugin());
    }

}
