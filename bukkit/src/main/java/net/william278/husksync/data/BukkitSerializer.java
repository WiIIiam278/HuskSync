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
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBTCompoundList;
import de.tr7zw.changeme.nbtapi.utils.DataFixerUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.william278.desertwell.util.Version;
import net.william278.husksync.BukkitHuskSync;
import net.william278.husksync.HuskSync;
import net.william278.husksync.adapter.Adaptable;
import net.william278.husksync.api.HuskSyncAPI;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.william278.husksync.data.BukkitData.Items.Inventory.INVENTORY_SLOT_COUNT;
import static net.william278.husksync.data.Data.Items.Inventory.HELD_ITEM_SLOT_TAG;
import static net.william278.husksync.data.Data.Items.Inventory.ITEMS_TAG;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BukkitSerializer {

    protected final HuskSync plugin;

    @SuppressWarnings("unused")
    public BukkitSerializer(@NotNull HuskSyncAPI api) {
        this.plugin = api.getPlugin();
    }

    @ApiStatus.Internal
    @NotNull
    public HuskSync getPlugin() {
        return plugin;
    }

    public static class Inventory extends BukkitSerializer implements Serializer<BukkitData.Items.Inventory>,
            ItemDeserializer {

        public Inventory(@NotNull HuskSync plugin) {
            super(plugin);
        }

        @Override
        public BukkitData.Items.Inventory deserialize(@NotNull String serialized, @NotNull Version dataMcVersion)
                throws DeserializationException {
            final ReadWriteNBT root = NBT.parseNBT(serialized);
            final ReadWriteNBT items = root.hasTag(ITEMS_TAG) ? root.getCompound(ITEMS_TAG) : null;
            return BukkitData.Items.Inventory.from(
                    items != null ? getItems(items, dataMcVersion) : new ItemStack[INVENTORY_SLOT_COUNT],
                    root.hasTag(HELD_ITEM_SLOT_TAG) ? root.getInteger(HELD_ITEM_SLOT_TAG) : 0
            );
        }

        @Override
        public BukkitData.Items.Inventory deserialize(@NotNull String serialized) {
            return deserialize(serialized, plugin.getMinecraftVersion());
        }

        @NotNull
        @Override
        public String serialize(@NotNull BukkitData.Items.Inventory data) throws SerializationException {
            final ReadWriteNBT root = NBT.createNBTObject();
            root.setItemStackArray(ITEMS_TAG, data.getContents());
            root.setInteger(HELD_ITEM_SLOT_TAG, data.getHeldItemSlot());
            return root.toString();
        }

    }

    public static class EnderChest extends BukkitSerializer implements Serializer<BukkitData.Items.EnderChest>,
            ItemDeserializer {

        public EnderChest(@NotNull HuskSync plugin) {
            super(plugin);
        }

        @Override
        public BukkitData.Items.EnderChest deserialize(@NotNull String serialized, @NotNull Version dataMcVersion)
                throws DeserializationException {
            final ItemStack[] items = getItems(NBT.parseNBT(serialized), dataMcVersion);
            return items == null ? BukkitData.Items.EnderChest.empty() : BukkitData.Items.EnderChest.adapt(items);
        }

        @Override
        public BukkitData.Items.EnderChest deserialize(@NotNull String serialized) {
            return deserialize(serialized, plugin.getMinecraftVersion());
        }

        @NotNull
        @Override
        public String serialize(@NotNull BukkitData.Items.EnderChest data) throws SerializationException {
            return NBT.itemStackArrayToNBT(data.getContents()).toString();
        }
    }

    // Utility interface for deserializing and upgrading item stacks from legacy versions
    private interface ItemDeserializer {

        @Nullable
        default ItemStack[] getItems(@NotNull ReadWriteNBT tag, @NotNull Version mcVersion) {
            if (mcVersion.compareTo(getPlugin().getMinecraftVersion()) < 0) {
                return upgradeItemStacks((NBTCompound) tag, mcVersion);
            }
            return NBT.itemStackArrayFromNBT(tag);
        }

        @NotNull
        private ItemStack @NotNull [] upgradeItemStacks(@NotNull NBTCompound itemsNbt, @NotNull Version mcVersion) {
            final ReadWriteNBTCompoundList items = itemsNbt.getCompoundList("items");
            final ItemStack[] itemStacks = new ItemStack[itemsNbt.getInteger("size")];
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i) == null) {
                    itemStacks[i] = new ItemStack(Material.AIR);
                    continue;
                }
                try {
                    itemStacks[i] = NBT.itemStackFromNBT(upgradeItemData(items.get(i), mcVersion));
                } catch (Throwable e) {
                    itemStacks[i] = new ItemStack(Material.AIR);
                }
            }
            return itemStacks;
        }

        @NotNull
        private ReadWriteNBT upgradeItemData(@NotNull ReadWriteNBT tag, @NotNull Version mcVersion)
                throws NoSuchFieldException, IllegalAccessException {
            return DataFixerUtil.fixUpItemData(
                    tag,
                    getPlugin().getDataVersion(mcVersion),
                    DataFixerUtil.getCurrentVersion()
            );
        }

        @NotNull
        HuskSync getPlugin();
    }

    public static class PotionEffects extends BukkitSerializer implements Serializer<BukkitData.PotionEffects> {

        private static final TypeToken<List<Data.PotionEffects.Effect>> TYPE = new TypeToken<>() {
        };

        public PotionEffects(@NotNull HuskSync plugin) {
            super(plugin);
        }

        @Override
        public BukkitData.PotionEffects deserialize(@NotNull String serialized) throws DeserializationException {
            return BukkitData.PotionEffects.adapt(
                    plugin.getGson().fromJson(serialized, TYPE.getType())
            );
        }

        @NotNull
        @Override
        public String serialize(@NotNull BukkitData.PotionEffects element) throws SerializationException {
            return plugin.getGson().toJson(element.getActiveEffects());
        }

    }

    public static class Advancements extends BukkitSerializer implements Serializer<BukkitData.Advancements> {

        private static final TypeToken<List<Data.Advancements.Advancement>> TYPE = new TypeToken<>() {
        };

        public Advancements(@NotNull HuskSync plugin) {
            super(plugin);
        }

        @Override
        public BukkitData.Advancements deserialize(@NotNull String serialized) throws DeserializationException {
            return BukkitData.Advancements.from(
                    plugin.getGson().fromJson(serialized, TYPE.getType())
            );
        }

        @NotNull
        @Override
        public String serialize(@NotNull BukkitData.Advancements element) throws SerializationException {
            return plugin.getGson().toJson(element.getCompleted());
        }
    }

    public static class PersistentData extends BukkitSerializer implements Serializer<BukkitData.PersistentData> {

        public PersistentData(@NotNull HuskSync plugin) {
            super(plugin);
        }

        @Override
        public BukkitData.PersistentData deserialize(@NotNull String serialized) throws DeserializationException {
            return BukkitData.PersistentData.from((NBTContainer) NBT.parseNBT(serialized));
        }

        @NotNull
        @Override
        public String serialize(@NotNull BukkitData.PersistentData element) throws SerializationException {
            return element.getPersistentData().toString();
        }

    }

    /**
     * @deprecated Use {@link Serializer.Json} in the common module instead
     */
    @Deprecated(since = "2.6")
    public class Json<T extends Data & Adaptable> extends Serializer.Json<T> {

        public Json(@NotNull HuskSync plugin, @NotNull Class<T> type) {
            super(plugin, type);
        }

        @NotNull
        public BukkitHuskSync getPlugin() {
            return (BukkitHuskSync) plugin;
        }

    }

}
