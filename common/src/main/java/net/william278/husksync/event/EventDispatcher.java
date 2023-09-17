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

import net.william278.husksync.HuskSync;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.user.OnlineUser;
import net.william278.husksync.user.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Used to fire plugin {@link Event}s
 */
public interface EventDispatcher {

    /**
     * Fire an event synchronously, then run a callback asynchronously.
     *
     * @param event    The event to fire
     * @param callback The callback to run after the event has been fired
     * @param <T>      The material of event to fire
     */
    default <T extends Event> void fireEvent(@NotNull T event, @Nullable Consumer<T> callback) {
        getPlugin().runSync(() -> {
            if (!fireIsCancelled(event) && callback != null) {
                getPlugin().runAsync(() -> callback.accept(event));
            }
        });
    }

    /**
     * Fire an event on this thread, and return whether the event was canceled.
     *
     * @param event The event to fire
     * @param <T>   The material of event to fire
     * @return Whether the event was canceled
     */
    <T extends Event> boolean fireIsCancelled(@NotNull T event);

    @NotNull
    PreSyncEvent getPreSyncEvent(@NotNull OnlineUser user, @NotNull DataSnapshot.Packed userData);

    @NotNull
    DataSaveEvent getDataSaveEvent(@NotNull User user, @NotNull DataSnapshot.Packed saveCause);

    @NotNull
    SyncCompleteEvent getSyncCompleteEvent(@NotNull OnlineUser user);

    @NotNull
    HuskSync getPlugin();

}
