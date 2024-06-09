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
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.user.OnlineUser;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.NotNull;

public interface FabricPreSyncCallback extends FabricEventCallback<PreSyncEvent> {

    @NotNull
    Event<FabricPreSyncCallback> EVENT = EventFactory.createArrayBacked(FabricPreSyncCallback.class,
            (listeners) -> (event) -> {
                for (FabricPreSyncCallback listener : listeners) {
                    final ActionResult result = listener.invoke(event);
                    if (event.isCancelled()) {
                        return ActionResult.CONSUME;
                    } else if (result != ActionResult.PASS) {
                        event.setCancelled(true);
                        return result;
                    }
                }

                return ActionResult.PASS;
            });

    @NotNull
    TriFunction<OnlineUser, DataSnapshot.Packed, HuskSync, PreSyncEvent> SUPPLIER = (user, data, plugin) ->

            new PreSyncEvent() {
                private boolean cancelled = false;

                @Override
                public boolean isCancelled() {
                    return cancelled;
                }

                @Override
                public void setCancelled(boolean cancelled) {
                    this.cancelled = cancelled;
                }

                @NotNull
                @Override
                public DataSnapshot.Packed getData() {
                    return data;
                }

                @NotNull
                @Override
                public HuskSync getPlugin() {
                    return plugin;
                }

                @NotNull
                @Override
                public OnlineUser getUser() {
                    return user;
                }

                @NotNull
                @SuppressWarnings("unused")
                public Event<FabricPreSyncCallback> getEvent() {
                    return EVENT;
                }
            };

}
