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
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.william278.desertwell.util.Version;
import net.william278.husksync.FabricHuskSync;
import net.william278.husksync.HuskSync;
import net.william278.husksync.api.HuskSyncAPI;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

import static net.william278.husksync.data.Data.Items.Inventory.*;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class FabricSerializer {

    @ApiStatus.Internal
    protected final HuskSync plugin;

    @SuppressWarnings("unused")
    public FabricSerializer(@NotNull HuskSyncAPI api) {
        this.plugin = api.getPlugin();
    }

    @ApiStatus.Internal
    @NotNull
    public HuskSync getPlugin() {
        return plugin;
    }

    public static class Inventory extends FabricSerializer implements Serializer<FabricData.Items.Inventory>,
            ItemDeserializer {

        public Inventory(@NotNull HuskSync plugin) {
            super(plugin);
        }

        @Override
        public FabricData.Items.Inventory deserialize(@NotNull String serialized, @NotNull Version dataMcVersion)
                throws DeserializationException {
            // Read item NBT from string
            final FabricHuskSync plugin = (FabricHuskSync) getPlugin();
            final NbtCompound root;
            try {
                root = StringNbtReader.parse(serialized);
            } catch (Throwable e) {
                throw new DeserializationException("Failed to read item NBT from string (%s)".formatted(serialized), e);
            }

            // Deserialize the inventory data
            final NbtList items = root.contains(ITEMS_TAG) ? root.getList(ITEMS_TAG, NbtElement.COMPOUND_TYPE) : null;
            return FabricData.Items.Inventory.from(
                    items != null ? getItems(items, dataMcVersion, plugin) : new ItemStack[INVENTORY_SLOT_COUNT],
                    root.contains(HELD_ITEM_SLOT_TAG) ? root.getInt(HELD_ITEM_SLOT_TAG) : 0
            );
        }

        @Override
        public FabricData.Items.Inventory deserialize(@NotNull String serialized) {
            return deserialize(serialized, plugin.getMinecraftVersion());
        }

        @NotNull
        @Override
        public String serialize(@NotNull FabricData.Items.Inventory data) throws SerializationException {
            try {
                final NbtCompound root = new NbtCompound();
                final NbtList items = new NbtList();
                Arrays.stream(data.getContents()).forEach(item -> items.add(
                        item != null ? item.writeNbt(new NbtCompound()) : new NbtCompound()
                ));
                root.put(ITEMS_TAG, items);
                root.putInt(HELD_ITEM_SLOT_TAG, data.getHeldItemSlot());
                return root.toString();
            } catch (Throwable e) {
                throw new SerializationException("Failed to serialize item NBT to string", e);
            }
        }

    }

    public static class EnderChest extends FabricSerializer implements Serializer<FabricData.Items.EnderChest>,
            ItemDeserializer {

        public EnderChest(@NotNull HuskSync plugin) {
            super(plugin);
        }

        @Override
        public FabricData.Items.EnderChest deserialize(@NotNull String serialized, @NotNull Version dataMcVersion)
                throws DeserializationException {
            final FabricHuskSync plugin = (FabricHuskSync) getPlugin();
            final NbtList items;
            try {
                items = (NbtList) new StringNbtReader(new StringReader(serialized)).parseElement();
            } catch (Throwable e) {
                throw new DeserializationException("Failed to read item NBT from string (%s)".formatted(serialized), e);
            }
            return FabricData.Items.EnderChest.adapt(getItems(items, dataMcVersion, plugin));
        }

        @Override
        public FabricData.Items.EnderChest deserialize(@NotNull String serialized) {
            return deserialize(serialized, plugin.getMinecraftVersion());
        }

        @NotNull
        @Override
        public String serialize(@NotNull FabricData.Items.EnderChest data) throws SerializationException {
            try {
                final NbtList items = new NbtList();
                Arrays.stream(data.getContents()).forEach(item -> items.add(
                        item != null ? item.writeNbt(new NbtCompound()) : new NbtCompound()
                ));
                return items.toString();
            } catch (Throwable e) {
                throw new SerializationException("Failed to serialize item NBT to string", e);
            }
        }
    }

    private interface ItemDeserializer {

        int VERSION1_16_5 = 2586;
        int VERSION1_17_1 = 2730;
        int VERSION1_18_2 = 2975;
        int VERSION1_19_2 = 3120;
        int VERSION1_19_4 = 3337;
        int VERSION1_20_1 = 3465;
        int VERSION1_20_2 = 3578; // Future
        int VERSION1_20_4 = 3700; // Future
        int VERSION1_20_5 = 3837; // Future

        @NotNull
        default ItemStack[] getItems(@NotNull NbtList tag, @NotNull Version mcVersion, @NotNull FabricHuskSync plugin) {
            try {
                if (mcVersion.compareTo(plugin.getMinecraftVersion()) < 0) {
                    return upgradeItemStacks(tag, mcVersion, plugin);
                }
                final ItemStack[] itemStacks = new ItemStack[tag.size()];
                for (int i = 0; i < tag.size(); i++) {
                    itemStacks[i] = ItemStack.fromNbt(tag.getCompound(i));
                }
                return itemStacks;
            } catch (Throwable e) {
                plugin.debug("Failed to read/upgrade item NBT from string (%s)".formatted(tag), e);
                return new ItemStack[tag.size()];
            }
        }

        @NotNull
        private ItemStack @NotNull [] upgradeItemStacks(@NotNull NbtList items, @NotNull Version mcVersion,
                                                        @NotNull FabricHuskSync plugin) {
            final ItemStack[] itemStacks = new ItemStack[items.size()];
            for (int i = 0; i < items.size(); i++) {
                if (items.getCompound(i) == null) {
                    itemStacks[i] = ItemStack.EMPTY;
                    continue;
                }
                try {
                    itemStacks[i] = ItemStack.fromNbt(upgradeItemData(items.getCompound(i), mcVersion, plugin));
                } catch (Throwable e) {
                    itemStacks[i] = ItemStack.EMPTY;
                }
            }
            return itemStacks;
        }


        @NotNull
        @SuppressWarnings({"rawtypes", "unchecked"}) // For NBTOps lookup
        private NbtCompound upgradeItemData(@NotNull NbtCompound tag, @NotNull Version mcVersion,
                                            @NotNull FabricHuskSync plugin) {
            return (NbtCompound) plugin.getMinecraftServer().getDataFixer().update(
                    TypeReferences.ITEM_STACK, new Dynamic<Object>((DynamicOps) NbtOps.INSTANCE, tag),
                    getDataVersion(mcVersion), getDataVersion(plugin.getMinecraftVersion())
            ).getValue();
        }

        private int getDataVersion(@NotNull Version mcVersion) {
            return switch (mcVersion.toStringWithoutMetadata()) {
                case "1.16", "1.16.1", "1.16.2", "1.16.3", "1.16.4", "1.16.5" -> VERSION1_16_5;
                case "1.17", "1.17.1" -> VERSION1_17_1;
                case "1.18", "1.18.1", "1.18.2" -> VERSION1_18_2;
                case "1.19", "1.19.1", "1.19.2" -> VERSION1_19_2;
                case "1.19.4" -> VERSION1_19_4;
                case "1.20", "1.20.1" -> VERSION1_20_1;
                case "1.20.2" -> VERSION1_20_2; // Future
                case "1.20.4" -> VERSION1_20_4; // Future
                case "1.20.5", "1.20.6" -> VERSION1_20_5; // Future
                default -> VERSION1_20_1; // Current supported ver
            };
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

    public static class Advancements extends FabricSerializer implements Serializer<FabricData.Advancements> {

        private static final TypeToken<List<Data.Advancements.Advancement>> TYPE = new TypeToken<>() {
        };

        public Advancements(@NotNull HuskSync plugin) {
            super(plugin);
        }

        @Override
        public FabricData.Advancements deserialize(@NotNull String serialized) throws DeserializationException {
            return FabricData.Advancements.from(
                    plugin.getGson().fromJson(serialized, TYPE.getType())
            );
        }

        @NotNull
        @Override
        public String serialize(@NotNull FabricData.Advancements element) throws SerializationException {
            return plugin.getGson().toJson(element.getCompleted());
        }
    }

}
