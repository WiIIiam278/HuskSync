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

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.util.ActionResult;
import net.william278.husksync.HuskSync;
import net.william278.husksync.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

public interface FabricSyncCompleteCallback extends FabricEventCallback<SyncCompleteEvent> {

    @NotNull
    Event<FabricSyncCompleteCallback> EVENT = EventFactory.createArrayBacked(FabricSyncCompleteCallback.class,
            (listeners) -> (event) -> {
                for (FabricSyncCompleteCallback listener : listeners) {
                    listener.invoke(event);
                }

                return ActionResult.PASS;
            });

    @NotNull
    BiFunction<OnlineUser, HuskSync, SyncCompleteEvent> SUPPLIER = (user, plugin) ->

            new SyncCompleteEvent() {

                @NotNull
                @Override
                public OnlineUser getUser() {
                    return user;
                }

                @NotNull
                @SuppressWarnings("unused")
                public Event<FabricSyncCompleteCallback> getEvent() {
                    return EVENT;
                }
            };

}
