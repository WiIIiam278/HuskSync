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
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public interface PlayerDeathDropsCallback {

    @NotNull
    Event<PlayerDeathDropsCallback> EVENT = EventFactory.createArrayBacked(
            PlayerDeathDropsCallback.class,
            (listeners) -> (player, itemsToKeep, itemsToDrop) -> Arrays.stream(listeners)
                    .forEach(listener -> listener.drops(player, itemsToKeep, itemsToDrop))
    );

    void drops(@NotNull ServerPlayerEntity player,
               @Nullable ItemStack @NotNull [] itemsToKeep,
               @Nullable ItemStack @NotNull [] itemsToDrop);

}
