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

package net.william278.husksync.mixins;

import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.william278.husksync.event.ItemDropCallback;
import net.william278.husksync.event.PlayerCommandCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Adapted from simplerauth (https://github.com/lolicode-org/simplerauth), which is licensed under the MIT License
@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayerEntity player;

    @Unique
    private void sendToPlayer(Packet<?> packet) {
        this.player.networkHandler.sendPacket(packet);
    }

    @Inject(method = "onPlayerAction", at = @At("HEAD"), cancellable = true)
    public void onPlayerAction(PlayerActionC2SPacket packet, CallbackInfo ci) {
        if (packet.getAction() == PlayerActionC2SPacket.Action.DROP_ITEM
                || packet.getAction() == PlayerActionC2SPacket.Action.DROP_ALL_ITEMS) {
            ItemStack stack = player.getStackInHand(Hand.MAIN_HAND);
            ActionResult result = ItemDropCallback.EVENT.invoker().interact(player, stack);

            if (result == ActionResult.FAIL) {
                ci.cancel();
                sendToPlayer(new ScreenHandlerSlotUpdateS2CPacket(
                        -2,
                        1,
                        player.getInventory().getSlotWithStack(stack),
                        stack
                ));
            }
        }
    }

    @Inject(method = "onClickSlot", at = @At("HEAD"), cancellable = true)
    public void onClickSlot(ClickSlotC2SPacket packet, CallbackInfo ci) {
        int slot = packet.getSlot();
        if (slot < 0) {
            return;
        }

        ItemStack stack = this.player.getInventory().getStack(slot);
        ActionResult result = ItemDropCallback.EVENT.invoker().interact(player, stack);

        if (result == ActionResult.FAIL) {
            ci.cancel();
            sendToPlayer(new ScreenHandlerSlotUpdateS2CPacket(-2, 1, slot, stack));
            sendToPlayer(new ScreenHandlerSlotUpdateS2CPacket(-1, 1, -1, ItemStack.EMPTY));
        }
    }

    @Inject(method = "onCreativeInventoryAction", at = @At("HEAD"), cancellable = true)
    public void onCreativeInventoryAction(CreativeInventoryActionC2SPacket packet, CallbackInfo ci) {
        int slot = packet.slot();
        if (slot < 0) {
            return;
        }

        ItemStack stack = this.player.getInventory().getStack(slot);
        ActionResult result = ItemDropCallback.EVENT.invoker().interact(player, stack);

        if (result == ActionResult.FAIL) {
            ci.cancel();
            sendToPlayer(new ScreenHandlerSlotUpdateS2CPacket(-2, 1, slot, stack));
            sendToPlayer(new ScreenHandlerSlotUpdateS2CPacket(-1, 1, -1, ItemStack.EMPTY));
        }
    }

    @Inject(method = "onCommandExecution", at = @At("HEAD"), cancellable = true)
    public void onCommandExecution(CommandExecutionC2SPacket packet, CallbackInfo ci) {
        ActionResult result = PlayerCommandCallback.EVENT.invoker().interact(player, packet.command());

        if (result == ActionResult.FAIL) {
            ci.cancel();
        }
    }
}
