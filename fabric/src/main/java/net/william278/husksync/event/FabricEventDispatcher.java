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

import net.minecraft.util.ActionResult;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.user.OnlineUser;
import net.william278.husksync.user.User;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;

public interface FabricEventDispatcher extends EventDispatcher {

    @SuppressWarnings("unchecked")
    @Override
    default <T extends Event> boolean fireIsCancelled(@NotNull T event) {
        try {
            final Method field = event.getClass().getDeclaredMethod("getEvent");
            field.setAccessible(true);

            net.fabricmc.fabric.api.event.Event<?> fabricEvent =
                    (net.fabricmc.fabric.api.event.Event<?>) field.invoke(event);

            final FabricEventCallback<T> invoker = (FabricEventCallback<T>) fabricEvent.invoker();
            return invoker.invoke(event) == ActionResult.FAIL;
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            getPlugin().log(Level.WARNING, "Failed to fire event (" + event.getClass().getName() + ")", e);
            return false;
        }
    }

    @NotNull
    @Override
    default PreSyncEvent getPreSyncEvent(@NotNull OnlineUser user, @NotNull DataSnapshot.Packed userData) {
        return FabricPreSyncCallback.SUPPLIER.apply(user, userData, getPlugin());
    }

    @NotNull
    @Override
    default DataSaveEvent getDataSaveEvent(@NotNull User user, @NotNull DataSnapshot.Packed saveCause) {
        return FabricDataSaveCallback.SUPPLIER.apply(user, saveCause, getPlugin());
    }

    @NotNull
    @Override
    default SyncCompleteEvent getSyncCompleteEvent(@NotNull OnlineUser user) {
        return FabricSyncCompleteCallback.SUPPLIER.apply(user, getPlugin());
    }

}
