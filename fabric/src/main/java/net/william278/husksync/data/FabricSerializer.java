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
import org.jetbrains.annotations.Nullable;

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
            final NbtCompound items = root.contains(ITEMS_TAG) ? root.getCompound(ITEMS_TAG) : null;
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
                root.put(ITEMS_TAG, serializeItemArray(data.getContents()));
                root.putInt(HELD_ITEM_SLOT_TAG, data.getHeldItemSlot());
                return root.toString();
            } catch (Throwable e) {
                throw new SerializationException("Failed to serialize inventory item NBT to string", e);
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
            try {
                final NbtCompound items = StringNbtReader.parse(serialized);
                return FabricData.Items.EnderChest.adapt(getItems(items, dataMcVersion, plugin));
            } catch (Throwable e) {
                throw new DeserializationException("Failed to read item NBT from string (%s)".formatted(serialized), e);
            }
        }

        @Override
        public FabricData.Items.EnderChest deserialize(@NotNull String serialized) {
            return deserialize(serialized, plugin.getMinecraftVersion());
        }

        @NotNull
        @Override
        public String serialize(@NotNull FabricData.Items.EnderChest data) throws SerializationException {
            try {
                return serializeItemArray(data.getContents()).toString();
            } catch (Throwable e) {
                throw new SerializationException("Failed to serialize ender chest item NBT to string", e);
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
        int VERSION1_21 = 3953; // Future

        @NotNull
        default ItemStack[] getItems(@NotNull NbtCompound tag, @NotNull Version mcVersion, @NotNull FabricHuskSync plugin) {
            try {
                if (mcVersion.compareTo(plugin.getMinecraftVersion()) < 0) {
                    return upgradeItemStacks(tag, mcVersion, plugin);
                }

                final ItemStack[] contents = new ItemStack[tag.getInt("size")];
                final NbtList itemList = tag.getList("items", NbtElement.COMPOUND_TYPE);
                itemList.forEach(element -> {
                    final NbtCompound compound = (NbtCompound) element;
                    contents[compound.getInt("Slot")] = ItemStack.fromNbt(compound);
                });
                plugin.debug(Arrays.toString(contents));
                return contents;
            } catch (Throwable e) {
                throw new Serializer.DeserializationException("Failed to read item NBT string (%s)".formatted(tag), e);
            }
        }

        // Serialize items slot-by-slot
        @NotNull
        default NbtCompound serializeItemArray(@Nullable ItemStack @NotNull [] items) {
            final NbtCompound container = new NbtCompound();
            container.putInt("size", items.length);
            final NbtList itemList = new NbtList();
            for (int i = 0; i < items.length; i++) {
                final ItemStack item = items[i];
                if (item == null || item.isEmpty()) {
                    continue;
                }
                NbtCompound entry = new NbtCompound();
                entry.putInt("Slot", i);
                item.writeNbt(entry);
                itemList.add(entry);
            }
            container.put(ITEMS_TAG, itemList);
            return container;
        }

        @NotNull
        private ItemStack @NotNull [] upgradeItemStacks(@NotNull NbtCompound items, @NotNull Version mcVersion,
                                                        @NotNull FabricHuskSync plugin) {
            final int size = items.getInt("size");
            final NbtList list = items.getList("items", NbtElement.COMPOUND_TYPE);
            final ItemStack[] itemStacks = new ItemStack[size];
            Arrays.fill(itemStacks, ItemStack.EMPTY);
            for (int i = 0; i < size; i++) {
                if (list.getCompound(i) == null) {
                    continue;
                }
                final NbtCompound compound = list.getCompound(i);
                final int slot = compound.getInt("Slot");
                itemStacks[slot] = ItemStack.fromNbt(upgradeItemData(list.getCompound(i), mcVersion, plugin));
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
                case "1.21" -> VERSION1_21; // Future
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
