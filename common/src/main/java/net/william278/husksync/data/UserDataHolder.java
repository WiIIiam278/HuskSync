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

import net.william278.desertwell.util.ThrowingConsumer;
import net.william278.husksync.HuskSync;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * A holder of data in the form of {@link Data}s, which can be synced
 */
public interface UserDataHolder extends DataHolder {

    /**
     * Get the data that is enabled for syncing in the config
     *
     * @return the data that is enabled for syncing
     * @since 3.0
     */
    @Override
    @NotNull
    default Map<Identifier, Data> getData() {
        return getPlugin().getRegisteredDataTypes().stream()
                .filter(type -> getPlugin().getSettings().getSynchronizationFeature(type))
                .map(id -> Map.entry(id, getData(id)))
                .filter(data -> data.getValue().isPresent())
                .collect(HashMap::new, (map, data) -> map.put(data.getKey(), data.getValue().get()), HashMap::putAll);
    }

    @Override
    default void setData(@NotNull Identifier identifier, @NotNull Data data) {
        data.apply(this, getPlugin());
    }

    /**
     * Create a serialized data snapshot of this data owner
     *
     * @param saveCause the cause of the snapshot
     * @return the snapshot
     * @since 3.0
     */
    @NotNull
    default DataSnapshot.Packed createSnapshot(@NotNull DataSnapshot.SaveCause saveCause) {
        return DataSnapshot.create(getPlugin(), this, saveCause);
    }

    /**
     * Deserialize and apply a data snapshot to this data owner
     * <p>
     * This method will deserialize the data on the current thread, then synchronously apply it on
     * the main server thread.
     * </p>
     * The {@code runAfter} callback function will be run after the snapshot has been applied.
     *
     * @param snapshot the snapshot to apply
     * @param runAfter the function to run asynchronously after the snapshot has been applied
     * @since 3.0
     */
    default void applySnapshot(@NotNull DataSnapshot.Packed snapshot, @NotNull ThrowingConsumer<UserDataHolder> runAfter) {
        final HuskSync plugin = getPlugin();
        final DataSnapshot.Unpacked unpacked = snapshot.unpack(plugin);
        plugin.runSync(() -> {
            try {
                unpacked.getData().forEach((type, data) -> {
                    if (plugin.getSettings().getSynchronizationFeature(type)) {
                        data.apply(this, plugin);
                    }
                });
            } catch (Throwable e) {
                plugin.log(Level.SEVERE, "An exception occurred applying data to a user", e);
                return;
            }

            plugin.runAsync(() -> runAfter.accept(this));
        });
    }

    @Override
    default void setInventory(@NotNull Data.Items.Inventory inventory) {
        inventory.apply(this, getPlugin());
    }

    @Override
    default void setEnderChest(@NotNull Data.Items.EnderChest enderChest) {
        enderChest.apply(this, getPlugin());
    }

    @Override
    default void setPotionEffects(@NotNull Data.PotionEffects potionEffects) {
        potionEffects.apply(this, getPlugin());
    }

    @Override
    default void setAdvancements(@NotNull Data.Advancements advancements) {
        advancements.apply(this, getPlugin());
    }

    @Override
    default void setLocation(@NotNull Data.Location location) {
        location.apply(this, getPlugin());
    }

    @Override
    default void setStatistics(@NotNull Data.Statistics statistics) {
        statistics.apply(this, getPlugin());
    }

    @Override
    default void setHealth(@NotNull Data.Health health) {
        health.apply(this, getPlugin());
    }

    @Override
    default void setHunger(@NotNull Data.Hunger hunger) {
        hunger.apply(this, getPlugin());
    }

    @Override
    default void setExperience(@NotNull Data.Experience experience) {
        experience.apply(this, getPlugin());
    }

    @Override
    default void setGameMode(@NotNull Data.GameMode gameMode) {
        gameMode.apply(this, getPlugin());
    }

    @Override
    default void setPersistentData(@NotNull Data.PersistentData persistentData) {
        persistentData.apply(this, getPlugin());
    }

    @NotNull
    @ApiStatus.Internal
    HuskSync getPlugin();

}
