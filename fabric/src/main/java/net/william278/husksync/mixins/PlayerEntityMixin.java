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

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.william278.husksync.event.PlayerDeathDropsCallback;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

    @Final
    @Shadow
    private PlayerInventory inventory;

    @Inject(method = "dropInventory", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;vanishCursedItems()V"))
    protected void dropInventory(@NotNull CallbackInfo ci) {
        final PlayerEntity player = (PlayerEntity) (Object) this;
        PlayerDeathDropsCallback.EVENT.invoker().drops((ServerPlayerEntity) player, getItemsToKeep(), getItemsToDrop());
    }

    @Unique
    @Nullable
    private ItemStack @NotNull [] getItemsToKeep() {
        final @Nullable ItemStack @NotNull [] toKeep = new ItemStack[inventory.size()];
        for (int i = 0; i < inventory.size(); ++i) {
            ItemStack itemStack = inventory.getStack(i);
            if (!itemStack.isEmpty() && EnchantmentHelper.hasAnyEnchantmentsIn(itemStack, TagKey.of(Enchantments.VANISHING_CURSE.getRegistryRef(), Enchantments.VANISHING_CURSE.getValue()))) {
                toKeep[i] = null;
                continue;
            }
            toKeep[i] = itemStack;
        }
        return toKeep;
    }

    @Unique
    @Nullable
    private ItemStack @NotNull [] getItemsToDrop() {
        final @Nullable ItemStack @NotNull [] toDrop = new ItemStack[inventory.size()];
        for (int i = 0; i < inventory.size(); ++i) {
            ItemStack itemStack = inventory.getStack(i);
            if (!itemStack.isEmpty() && EnchantmentHelper.hasAnyEnchantmentsIn(itemStack, TagKey.of(Enchantments.VANISHING_CURSE.getRegistryRef(), Enchantments.VANISHING_CURSE.getValue()))) {
                toDrop[i] = itemStack;
                continue;
            }
            toDrop[i] = null;
        }
        return toDrop;
    }

}
