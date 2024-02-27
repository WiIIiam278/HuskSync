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

import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.StringReader;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.StringNbtReader;
import net.william278.husksync.HuskSync;
import net.william278.husksync.api.HuskSyncAPI;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

import static net.william278.husksync.data.Data.Items.Inventory.HELD_ITEM_SLOT_TAG;
import static net.william278.husksync.data.Data.Items.Inventory.ITEMS_TAG;

//TODO
public abstract class FabricSerializer {

    protected final HuskSync plugin;

    private FabricSerializer(@NotNull HuskSync plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("unused")
    public FabricSerializer(@NotNull HuskSyncAPI api) {
        this.plugin = api.getPlugin();
    }

    @ApiStatus.Internal
    @NotNull
    public HuskSync getPlugin() {
        return plugin;
    }

    public static class Inventory extends FabricSerializer implements Serializer<FabricData.Items.Inventory> {

        public Inventory(@NotNull HuskSync plugin) {
            super(plugin);
        }

        @Override
        @SuppressWarnings("DuplicatedCode")
        public FabricData.Items.Inventory deserialize(@NotNull String serialized) throws DeserializationException {
            try {
                final NbtCompound root = StringNbtReader.parse(serialized);
                final NbtList items = root.getList(ITEMS_TAG, NbtElement.COMPOUND_TYPE);
                final ItemStack[] contents = new ItemStack[items.size()];
                for (int i = 0; i < items.size(); i++) {
                    final NbtCompound item = items.getCompound(i);
                    contents[i] = ItemStack.fromNbt(item);
                }
                final int heldItemSlot = root.getInt(HELD_ITEM_SLOT_TAG);
                return FabricData.Items.Inventory.from(
                        contents,
                        heldItemSlot
                );
            } catch (Throwable e) {
                throw new DeserializationException("Failed to read item NBT", e);
            }
        }

        @NotNull
        @Override
        public String serialize(@NotNull FabricData.Items.Inventory data) throws SerializationException {
            final NbtCompound root = new NbtCompound();
            final NbtList items = new NbtList();
            Arrays.stream(data.getContents()).forEach(item -> items.add(item.writeNbt(new NbtCompound())));
            root.put(ITEMS_TAG, items);
            root.putInt(HELD_ITEM_SLOT_TAG, data.getHeldItemSlot());
            return root.toString();
        }

    }

    public static class EnderChest extends FabricSerializer implements Serializer<FabricData.Items.EnderChest> {

        public EnderChest(@NotNull HuskSync plugin) {
            super(plugin);
        }

        @Override
        @SuppressWarnings("DuplicatedCode")
        public FabricData.Items.EnderChest deserialize(@NotNull String serialized) throws DeserializationException {
            try {
                final NbtList items = (NbtList) new StringNbtReader(new StringReader(serialized)).parseElement();
                final ItemStack[] contents = new ItemStack[items.size()];
                for (int i = 0; i < items.size(); i++) {
                    contents[i] = items.get(i) != null ? ItemStack.fromNbt(items.getCompound(i)) : ItemStack.EMPTY;
                }
                return FabricData.Items.EnderChest.adapt(contents);
            } catch (Throwable e) {
                throw new DeserializationException("Failed to read item NBT", e);
            }
        }

        @NotNull
        @Override
        public String serialize(@NotNull FabricData.Items.EnderChest data) throws SerializationException {
            final NbtList items = new NbtList();
            Arrays.stream(data.getContents()).forEach(item -> items.add(item.writeNbt(new NbtCompound())));
            return items.toString();
        }
    }

    public static class PotionEffects extends FabricSerializer implements Serializer<FabricData.PotionEffects> {

        private static final TypeToken<List<Data.PotionEffects.Effect>> TYPE = new TypeToken<>() {
        };

        public PotionEffects(@NotNull HuskSync plugin) {
            super(plugin);
        }

        @Override
        public FabricData.PotionEffects deserialize(@NotNull String serialized) throws DeserializationException {
            return FabricData.PotionEffects.adapt(
                    plugin.getGson().fromJson(serialized, TYPE.getType())
            );
        }

        @NotNull
        @Override
        public String serialize(@NotNull FabricData.PotionEffects element) throws SerializationException {
            return plugin.getGson().toJson(element.getActiveEffects());
        }

    }
    
}
