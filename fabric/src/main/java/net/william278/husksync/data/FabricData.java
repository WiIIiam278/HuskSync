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

package net.william278.husksync.data;

import com.google.gson.annotations.SerializedName;
import lombok.*;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.william278.husksync.FabricHuskSync;
import net.william278.husksync.HuskSync;
import net.william278.husksync.adapter.Adaptable;
import net.william278.husksync.user.FabricUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.*;

//TODO
public abstract class FabricData implements Data {
    @Override
    public void apply(@NotNull UserDataHolder user, @NotNull HuskSync plugin) {
        this.apply((FabricUser) user, (FabricHuskSync) plugin);
    }

    protected abstract void apply(@NotNull FabricUser user, @NotNull FabricHuskSync plugin);

    @Getter
    public static abstract class Items extends FabricData implements Data.Items {

        private final ItemStack[] contents;

        private Items(@NotNull ItemStack[] contents) {
            this.contents = Arrays.stream(contents).toArray(ItemStack[]::new);
        }

        @NotNull
        @Override
        public Stack[] getStack() {
            return Arrays.stream(contents)
                    .map(stack -> new Stack(
                            stack.getItem().toString(),
                            stack.getCount(),
                            stack.getName().getString(),
                            Optional.ofNullable(stack.getSubNbt(ItemStack.DISPLAY_KEY))
                                    .flatMap(display -> Optional.ofNullable(display.get(ItemStack.LORE_KEY))
                                            .map(lore -> ((List<String>) lore).stream().toList())) //todo check this is ok
                                    .orElse(null),
                            stack.getEnchantments().stream()
                                    .map(element -> EnchantmentHelper.getIdFromNbt((NbtCompound) element))
                                    .filter(Objects::nonNull).map(Identifier::toString)
                                    .toList()
                    ))
                    .toArray(Stack[]::new);
        }

        @Override
        public void clear() {
            Arrays.fill(contents, ItemStack.EMPTY);
        }

        @Override
        public void setContents(@NotNull Data.Items contents) {
            this.setContents(((FabricData.Items) contents).getContents());
        }

        public void setContents(@NotNull ItemStack[] contents) {
            System.arraycopy(contents, 0, this.contents, 0, this.contents.length);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof FabricData.Items items) {
                return Arrays.equals(contents, items.getContents());
            }
            return false;
        }

        @Getter
        public static class Inventory extends FabricData.Items implements Data.Items.Inventory {

            public static final int INVENTORY_SLOT_COUNT = 41;

            @Setter(onMethod_ = @Range(from = 0, to = 8))
            private int heldItemSlot;

            public Inventory(@NotNull ItemStack[] contents, int heldItemSlot) {
                super(contents);
                this.heldItemSlot = heldItemSlot;
            }

            @NotNull
            public static FabricData.Items.Inventory from(@NotNull ItemStack[] contents, int heldItemSlot) {
                return new FabricData.Items.Inventory(contents, heldItemSlot);
            }

            @NotNull
            public static FabricData.Items.Inventory from(@NotNull Collection<ItemStack> contents, int heldItemSlot) {
                return from(contents.toArray(ItemStack[]::new), heldItemSlot);
            }

            @NotNull
            public static FabricData.Items.Inventory empty() {
                return new FabricData.Items.Inventory(new ItemStack[INVENTORY_SLOT_COUNT], 0);
            }

            @Override
            public void apply(@NotNull FabricUser user, @NotNull FabricHuskSync plugin) throws IllegalStateException {
                final ServerPlayerEntity player = user.getPlayer();
                this.clearInventoryCraftingSlots(player);
                player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
                final ItemStack[] items = getContents();
                for (int slot = 0; slot < player.getInventory().size(); slot++) {
                    player.getInventory().setStack(
                            slot, items[slot] == null ? ItemStack.EMPTY : items[slot]
                    );
                }
                player.getInventory().selectedSlot = heldItemSlot;
                player.playerScreenHandler.sendContentUpdates();
                player.getInventory().updateItems();
            }

            private void clearInventoryCraftingSlots(@NotNull ServerPlayerEntity player) {
                player.playerScreenHandler.clearCraftingSlots();
            }

        }

        public static class EnderChest extends FabricData.Items implements Data.Items.EnderChest {

            public static final int ENDER_CHEST_SLOT_COUNT = 27;

            private EnderChest(@NotNull ItemStack[] contents) {
                super(contents);
            }

            @NotNull
            public static FabricData.Items.EnderChest adapt(@NotNull ItemStack[] items) {
                return new FabricData.Items.EnderChest(items);
            }

            @NotNull
            public static FabricData.Items.EnderChest adapt(@NotNull Collection<ItemStack> items) {
                return adapt(items.toArray(ItemStack[]::new));
            }

            @NotNull
            public static FabricData.Items.EnderChest empty() {
                return new FabricData.Items.EnderChest(new ItemStack[ENDER_CHEST_SLOT_COUNT]);
            }

            @Override
            public void apply(@NotNull FabricUser user, @NotNull FabricHuskSync plugin) throws IllegalStateException {
                final ItemStack[] items = getContents();
                for (int slot = 0; slot < user.getPlayer().getEnderChestInventory().size(); slot++) {
                    user.getPlayer().getEnderChestInventory().setStack(
                            slot, items[slot] == null ? ItemStack.EMPTY : items[slot]
                    );
                }
            }

        }

        public static class ItemArray extends FabricData.Items implements Data.Items {

            private ItemArray(@NotNull ItemStack[] contents) {
                super(contents);
            }

            @NotNull
            public static ItemArray adapt(@NotNull Collection<ItemStack> drops) {
                return new ItemArray(drops.toArray(ItemStack[]::new));
            }

            @NotNull
            public static ItemArray adapt(@NotNull ItemStack[] drops) {
                return new ItemArray(drops);
            }

            @Override
            public void apply(@NotNull FabricUser user, @NotNull FabricHuskSync plugin) throws IllegalStateException {
                throw new UnsupportedOperationException("A generic item array cannot be applied to a player");
            }

        }

    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class PotionEffects extends FabricData implements Data.PotionEffects {

        private final Collection<StatusEffectInstance> effects;

        @NotNull
        public static FabricData.PotionEffects from(@NotNull Collection<StatusEffectInstance> effects) {
            return new FabricData.PotionEffects(effects);
        }

        @NotNull
        public static FabricData.PotionEffects adapt(@NotNull Collection<Effect> effects) {
            return from(
                    effects.stream()
                            .map(effect -> new StatusEffectInstance(
                                    Objects.requireNonNull(
                                            Registries.STATUS_EFFECT.get(Identifier.tryParse(effect.type())),
                                            "Invalid effect type when adapting effects"
                                    ),
                                    effect.duration(),
                                    effect.amplifier(),
                                    effect.isAmbient(),
                                    effect.showParticles(),
                                    effect.hasIcon()
                            ))
                            .toList()
            );
        }

        @NotNull
        @SuppressWarnings("unused")
        public static FabricData.PotionEffects empty() {
            return new FabricData.PotionEffects(List.of());
        }

        @Override
        public void apply(@NotNull FabricUser user, @NotNull FabricHuskSync plugin) throws IllegalStateException {
            final ServerPlayerEntity player = user.getPlayer();
            player.getActiveStatusEffects().forEach((effect, instance) -> player.removeStatusEffect(effect));
            getEffects().forEach(player::addStatusEffect);
        }

        @NotNull
        @Override
        public List<Effect> getActiveEffects() {
            return effects.stream()
                    .map(potionEffect -> new Effect(
                            Objects.requireNonNull(
                                    Registries.STATUS_EFFECT.getId(potionEffect.getEffectType()),
                                    "Invalid effect type when getting active effects"
                            ).toString(),
                            potionEffect.getAmplifier(),
                            potionEffect.getDuration(),
                            potionEffect.isAmbient(),
                            potionEffect.shouldShowParticles(),
                            potionEffect.shouldShowIcon()
                    ))
                    .toList();
        }

    }

    // TODO ADVANCEMENTS

    @Getter
    @Setter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Location extends FabricData implements Data.Location, Adaptable {
        @SerializedName("x")
        private double x;
        @SerializedName("y")
        private double y;
        @SerializedName("z")
        private double z;
        @SerializedName("yaw")
        private float yaw;
        @SerializedName("pitch")
        private float pitch;
        @SerializedName("world")
        private World world;

        @NotNull
        public static FabricData.Location from(double x, double y, double z,
                                               float yaw, float pitch, @NotNull World world) {
            return new FabricData.Location(x, y, z, yaw, pitch, world);
        }

        @NotNull
        public static FabricData.Location adapt(@NotNull ServerPlayerEntity player) {
            return from(
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    player.getYaw(),
                    player.getPitch(),
                    new World(
                            Objects.requireNonNull(
                                    player.getWorld(), "World is null"
                            ).getRegistryKey().getValue().toString(),
                            UUID.nameUUIDFromBytes(
                                    player.getWorld().getDimensionKey().getValue().toString().getBytes()
                            ),
                            player.getWorld().getDimensionKey().getValue().toString()
                    )
            );
        }

        @Override
        public void apply(@NotNull FabricUser user, @NotNull FabricHuskSync plugin) throws IllegalStateException {
            final ServerPlayerEntity player = user.getPlayer();
            final MinecraftServer server = plugin.getMinecraftServer();
            try {
                player.dismountVehicle();
                FabricDimensions.teleport(
                        player,
                        server.getWorld(server.getWorldRegistryKeys().stream()
                                .filter(key -> key.getValue().equals(Identifier.tryParse(world.name())))
                                .findFirst().orElseThrow(
                                        () -> new IllegalStateException("Invalid world")
                                )),
                        new TeleportTarget(
                                new Vec3d(x, y, z),
                                Vec3d.ZERO,
                                yaw,
                                pitch
                        )
                );
            } catch (Throwable e) {
                throw new IllegalStateException("Failed to apply location", e);
            }
        }

    }

}
