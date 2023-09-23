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

package net.william278.husksync.listener;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.william278.husksync.HuskSync;
import net.william278.husksync.user.FabricUser;
import org.jetbrains.annotations.NotNull;

public class FabricEventListener extends EventListener {
    public FabricEventListener(@NotNull HuskSync plugin) {
        super(plugin);
        this.registerEvents();
    }

    public void registerEvents() {
        ServerPlayConnectionEvents.JOIN.register(this::handlePlayerJoin);
        ServerPlayConnectionEvents.DISCONNECT.register(this::handlePlayerQuit);

        // todo player death event mixin exposing death drops
        // todo world save mixin

        // TODO: Events of extra things to cancel if the player has not been set yet
    }

    private void handlePlayerJoin(@NotNull ServerPlayNetworkHandler handler, @NotNull PacketSender sender,
                                  @NotNull MinecraftServer server) {
        handlePlayerJoin(FabricUser.adapt(handler.player, plugin));
    }

    private void handlePlayerQuit(@NotNull ServerPlayNetworkHandler handler, @NotNull MinecraftServer server) {
        handlePlayerQuit(FabricUser.adapt(handler.player, plugin));
    }


}
