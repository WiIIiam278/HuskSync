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
import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import net.william278.husksync.HuskSync;
import net.william278.husksync.adapter.Adaptable;
import net.william278.husksync.api.HuskSyncAPI;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.william278.husksync.data.BukkitData.Items.Inventory.INVENTORY_SLOT_COUNT;

public class BukkitSerializer {

    protected final HuskSync plugin;

    private BukkitSerializer(@NotNull HuskSync plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("unused")
    public BukkitSerializer(@NotNull HuskSyncAPI api) {
        this.plugin = api.getPlugin();
    }

    @ApiStatus.Internal
    @NotNull
    public HuskSync getPlugin() {
        return plugin;
    }

    public static class Inventory extends BukkitSerializer implements Serializer<BukkitData.Items.Inventory> {
        private static final String ITEMS_TAG = "items";
        private static final String HELD_ITEM_SLOT_TAG = "held_item_slot";

        public Inventory(@NotNull HuskSync plugin) {
            super(plugin);
        }

        @Override
        public BukkitData.Items.Inventory deserialize(@NotNull String serialized) throws DeserializationException {
            final ReadWriteNBT root = NBT.parseNBT(serialized);
            final ItemStack[] items = root.getItemStackArray(ITEMS_TAG);
            final int heldItemSlot = root.getInteger(HELD_ITEM_SLOT_TAG);
            return BukkitData.Items.Inventory.from(
                    items == null ? new ItemStack[INVENTORY_SLOT_COUNT] : items,
                    heldItemSlot
            );
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

    public static class EnderChest extends BukkitSerializer implements Serializer<BukkitData.Items.EnderChest> {

        public EnderChest(@NotNull HuskSync plugin) {
            super(plugin);
        }

        @Override
        public BukkitData.Items.EnderChest deserialize(@NotNull String serialized) throws DeserializationException {
            final ItemStack[] items = NBT.itemStackArrayFromNBT(NBT.parseNBT(serialized));
            return items == null ? BukkitData.Items.EnderChest.empty() : BukkitData.Items.EnderChest.adapt(items);
        }

        @NotNull
        @Override
        public String serialize(@NotNull BukkitData.Items.EnderChest data) throws SerializationException {
            return NBT.itemStackArrayToNBT(data.getContents()).toString();
        }
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

    public static class Location extends BukkitSerializer implements Serializer<BukkitData.Location> {

        public Location(@NotNull HuskSync plugin) {
            super(plugin);
        }

        @Override
        public BukkitData.Location deserialize(@NotNull String serialized) throws DeserializationException {
            return plugin.getDataAdapter().fromJson(serialized, BukkitData.Location.class);
        }

        @NotNull
        @Override
        public String serialize(@NotNull BukkitData.Location element) throws SerializationException {
            return plugin.getDataAdapter().toJson(element);
        }
    }

    public static class Statistics extends BukkitSerializer implements Serializer<BukkitData.Statistics> {

        public Statistics(@NotNull HuskSync plugin) {
            super(plugin);
        }

        @Override
        public BukkitData.Statistics deserialize(@NotNull String serialized) throws DeserializationException {
            return BukkitData.Statistics.from(plugin.getGson().fromJson(
                    serialized,
                    BukkitData.Statistics.StatisticsMap.class
            ));
        }

        @NotNull
        @Override
        public String serialize(@NotNull BukkitData.Statistics element) throws SerializationException {
            return plugin.getGson().toJson(element.getStatisticsSet());
        }

    }

    public static class PersistentData extends BukkitSerializer implements Serializer<BukkitData.PersistentData> {

        public PersistentData(@NotNull HuskSync plugin) {
            super(plugin);
        }

        @Override
        public BukkitData.PersistentData deserialize(@NotNull String serialized) throws DeserializationException {
            return BukkitData.PersistentData.from(new NBTContainer(serialized));
        }

        @NotNull
        @Override
        public String serialize(@NotNull BukkitData.PersistentData element) throws SerializationException {
            return element.getPersistentData().toString();
        }

    }

    public static class Health extends Json<BukkitData.Health> implements Serializer<BukkitData.Health> {

        public Health(@NotNull HuskSync plugin) {
            super(plugin, BukkitData.Health.class);
        }

    }

    public static class Hunger extends Json<BukkitData.Hunger> implements Serializer<BukkitData.Hunger> {

        public Hunger(@NotNull HuskSync plugin) {
            super(plugin, BukkitData.Hunger.class);
        }

    }

    public static class Experience extends Json<BukkitData.Experience> implements Serializer<BukkitData.Experience> {

        public Experience(@NotNull HuskSync plugin) {
            super(plugin, BukkitData.Experience.class);
        }

    }

    public static class GameMode extends Json<BukkitData.GameMode> implements Serializer<BukkitData.GameMode> {

        public GameMode(@NotNull HuskSync plugin) {
            super(plugin, BukkitData.GameMode.class);
        }

    }

    public static abstract class Json<T extends Data & Adaptable> extends BukkitSerializer implements Serializer<T> {

        private final Class<T> type;

        protected Json(@NotNull HuskSync plugin, Class<T> type) {
            super(plugin);
            this.type = type;
        }

        @Override
        public T deserialize(@NotNull String serialized) throws DeserializationException {
            return plugin.getDataAdapter().fromJson(serialized, type);
        }

        @NotNull
        @Override
        public String serialize(@NotNull T element) throws SerializationException {
            return plugin.getDataAdapter().toJson(element);
        }

    }

}
