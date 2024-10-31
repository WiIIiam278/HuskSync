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
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.william278.husksync.FabricHuskSync;
import net.william278.husksync.HuskSync;
import net.william278.husksync.config.Settings.SynchronizationSettings.SaveOnDeathSettings;
import net.william278.husksync.data.FabricData;
import net.william278.husksync.event.*;
import net.william278.husksync.user.FabricUser;
import net.william278.husksync.user.OnlineUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FabricEventListener extends EventListener implements LockedHandler {

    public FabricEventListener(@NotNull HuskSync plugin) {
        super(plugin);
        this.registerEvents();
    }

    public void registerEvents() {
        ServerPlayConnectionEvents.JOIN.register(this::handlePlayerJoin);
        ServerPlayConnectionEvents.DISCONNECT.register(this::handlePlayerQuit);
        WorldSaveCallback.EVENT.register(this::handleWorldSave);
        PlayerDeathDropsCallback.EVENT.register(this::handlePlayerDeathDrops);

        // Locked events handling
        ItemPickupCallback.EVENT.register(this::handleItemPickup);
        ItemDropCallback.EVENT.register(this::handleItemDrop);
        UseBlockCallback.EVENT.register(this::handleBlockInteract);
        UseEntityCallback.EVENT.register(this::handleEntityInteract);
        UseItemCallback.EVENT.register(this::handleItemInteract);
        PlayerBlockBreakEvents.BEFORE.register(this::handleBlockBreak);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(this::handleEntityDamage);
        InventoryClickCallback.EVENT.register(this::handleInventoryClick);
        PlayerCommandCallback.EVENT.register(this::handlePlayerCommand);
    }

    private void handlePlayerJoin(@NotNull ServerPlayNetworkHandler handler, @NotNull PacketSender sender,
                                  @NotNull MinecraftServer server) {
        final FabricUser user = FabricUser.adapt(handler.player, plugin);
        ((FabricHuskSync) plugin).getPlayerMap().put(handler.player.getUuid(), user);
        handlePlayerJoin(user);
    }

    private void handlePlayerQuit(@NotNull ServerPlayNetworkHandler handler, @NotNull MinecraftServer server) {
        ((FabricHuskSync) plugin).getPlayerMap().remove(handler.player.getUuid());
        handlePlayerQuit(FabricUser.adapt(handler.player, plugin));
    }

    private void handleWorldSave(@NotNull ServerWorld world) {
        this.saveOnWorldSave(
                world.getPlayers().stream().map(player -> (OnlineUser) FabricUser.adapt(player, plugin)).toList()
        );
    }

    private void handlePlayerDeathDrops(@NotNull ServerPlayerEntity player, @Nullable ItemStack @NotNull [] toKeep,
                                        @Nullable ItemStack @NotNull [] toDrop) {
        final SaveOnDeathSettings settings = plugin.getSettings().getSynchronization().getSaveOnDeath();
        this.saveOnPlayerDeath(
                FabricUser.adapt(player, plugin),
                FabricData.Items.ItemArray.adapt(
                        settings.getItemsToSave() == SaveOnDeathSettings.DeathItemsMode.DROPS ? toDrop : toKeep
                )
        );
    }

    private ActionResult handleItemPickup(PlayerEntity player, ItemStack itemStack) {
        return (cancelPlayerEvent(player.getUuid())) ? ActionResult.FAIL : ActionResult.PASS;
    }

    private ActionResult handleItemDrop(PlayerEntity player, ItemStack itemStack) {
        return (cancelPlayerEvent(player.getUuid())) ? ActionResult.FAIL : ActionResult.PASS;
    }

    private ActionResult handleBlockInteract(PlayerEntity player, World world, Hand hand, BlockHitResult blockHitResult) {
        return (cancelPlayerEvent(player.getUuid())) ? ActionResult.FAIL : ActionResult.PASS;
    }

    private ActionResult handleEntityInteract(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult entityHitResult) {
        return (cancelPlayerEvent(player.getUuid())) ? ActionResult.FAIL : ActionResult.PASS;
    }

    private ActionResult handleItemInteract(PlayerEntity player, World world, Hand hand) {
        return (cancelPlayerEvent(player.getUuid())) ? ActionResult.FAIL : ActionResult.PASS;
    }

    private boolean handleBlockBreak(World world, PlayerEntity player, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity) {
        return !cancelPlayerEvent(player.getUuid());
    }

    private boolean handleEntityDamage(LivingEntity livingEntity, DamageSource damageSource, float v) {
        if (livingEntity instanceof ServerPlayerEntity player) {
            return !cancelPlayerEvent(player.getUuid());
        }
        return true;
    }

    private ActionResult handleInventoryClick(PlayerEntity player, ItemStack itemStack) {
        return (cancelPlayerEvent(player.getUuid())) ? ActionResult.FAIL : ActionResult.PASS;
    }

    private ActionResult handlePlayerCommand(PlayerEntity player, String s) {
        return (cancelPlayerEvent(player.getUuid())) ? ActionResult.FAIL : ActionResult.PASS;
    }

    @Override
    @NotNull
    public HuskSync getPlugin() {
        return plugin;
    }
}
