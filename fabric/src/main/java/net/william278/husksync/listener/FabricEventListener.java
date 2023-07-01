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
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameRules;
import net.william278.husksync.HuskSync;
import net.william278.husksync.data.FabricSerializer;
import net.william278.husksync.data.ItemData;
import net.william278.husksync.player.FabricPlayer;
import net.william278.husksync.player.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class FabricEventListener extends EventListener {
    public FabricEventListener(@NotNull HuskSync instance) {
        super(instance);
        this.registerEvents();
    }

    public void registerEvents() {
        // Join event
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> handlePlayerJoin(
                FabricPlayer.adapt(handler.player)
        ));

        // Quit event
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> handlePlayerQuit(
                FabricPlayer.adapt(handler.player)
        ));

        // Death event
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity player) {
                OnlineUser user = FabricPlayer.adapt(player);
                if (cancelPlayerEvent(user.uuid)) {
                    // scan player inventory for items to drop
                    List<ItemStack> drops = new ArrayList<>();

                    if (!player.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) {
                        for (int i = 0; i < player.getInventory().size(); ++i) {
                            ItemStack item = player.getInventory().getStack(i);
                            if (!item.isEmpty() && !EnchantmentHelper.hasVanishingCurse(item)) {
                                drops.add(item);
                            }
                        }
                    }

                    // If the player is locked or the plugin disabling, clear their drops
                    if (cancelPlayerEvent(user.uuid)) {
                        drops.forEach(drop -> player.dropItem(drop, false, false));
                        return;
                    }

                    // Handle saving player data snapshots on death
                    if (!plugin.getSettings().doSaveOnDeath()) return;

                    // save the player's inventory
                    FabricSerializer.serializeItemStackArray(drops.toArray(new ItemStack[0]))
                            .thenAccept(serialized -> super.saveOnPlayerDeath(user, new ItemData(serialized)));
                }
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            // Handle saving player data snapshots when the world saves
            if (!plugin.getSettings().doSaveOnWorldSave()) return;

            CompletableFuture.runAsync(() ->
                    super.saveOnWorldSave(
                            server.getPlayerManager()
                                    .getPlayerList()
                                    .stream().map(FabricPlayer::adapt)
                                    .collect(Collectors.toList())
                    )
            );
        });

        // TODO: Events of extra things to cancel if the player has not been set yet
    }
}
