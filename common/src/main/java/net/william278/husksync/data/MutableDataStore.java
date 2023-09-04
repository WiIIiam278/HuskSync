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

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

public interface MutableDataStore {

    @NotNull
    Map<DataContainer.Type, DataContainer> getData();

    @NotNull
    default Optional<DataContainer.Items.Inventory> getInventory() {
        return Optional.ofNullable((DataContainer.Items.Inventory) getData().get(DataContainer.Type.INVENTORY));
    }

    default void setInventory(@NotNull DataContainer.Items.Inventory inventory) {
        getData().put(DataContainer.Type.INVENTORY, inventory);
    }

    @NotNull
    default Optional<DataContainer.Items.EnderChest> getEnderChest() {
        return Optional.ofNullable((DataContainer.Items.EnderChest) getData().get(DataContainer.Type.ENDER_CHEST));
    }

    default void setEnderChest(@NotNull DataContainer.Items.EnderChest enderChest) {
        getData().put(DataContainer.Type.ENDER_CHEST, enderChest);
    }

    @NotNull
    default Optional<DataContainer.PotionEffects> getPotionEffects() {
        return Optional.ofNullable((DataContainer.PotionEffects) getData().get(DataContainer.Type.POTION_EFFECTS));
    }

    default void setPotionEffects(@NotNull DataContainer.PotionEffects potionEffects) {
        getData().put(DataContainer.Type.POTION_EFFECTS, potionEffects);
    }

    @NotNull
    default Optional<DataContainer.Advancements> getAdvancements() {
        return Optional.ofNullable((DataContainer.Advancements) getData().get(DataContainer.Type.ADVANCEMENTS));
    }

    default void setAdvancements(@NotNull DataContainer.Advancements advancements) {
        getData().put(DataContainer.Type.ADVANCEMENTS, advancements);
    }

    @NotNull
    default Optional<DataContainer.Location> getLocation() {
        return Optional.ofNullable((DataContainer.Location) getData().get(DataContainer.Type.LOCATION));
    }

    default void setLocation(@NotNull DataContainer.Location location) {
        getData().put(DataContainer.Type.LOCATION, location);
    }

    @NotNull
    default Optional<DataContainer.Statistics> getStatistics() {
        return Optional.ofNullable((DataContainer.Statistics) getData().get(DataContainer.Type.STATISTICS));
    }

    default void setStatistics(@NotNull DataContainer.Statistics statistics) {
        getData().put(DataContainer.Type.STATISTICS, statistics);
    }

    @NotNull
    default Optional<DataContainer.Health> getHealth() {
        return Optional.ofNullable((DataContainer.Health) getData().get(DataContainer.Type.HEALTH));
    }

    default void setHealth(@NotNull DataContainer.Health health) {
        getData().put(DataContainer.Type.HEALTH, health);
    }

    @NotNull
    default Optional<DataContainer.Food> getFood() {
        return Optional.ofNullable((DataContainer.Food) getData().get(DataContainer.Type.FOOD));
    }

    default void setFood(@NotNull DataContainer.Food food) {
        getData().put(DataContainer.Type.FOOD, food);
    }

    @NotNull
    default Optional<DataContainer.Experience> getExperience() {
        return Optional.ofNullable((DataContainer.Experience) getData().get(DataContainer.Type.EXPERIENCE));
    }

    default void setExperience(@NotNull DataContainer.Experience experience) {
        getData().put(DataContainer.Type.EXPERIENCE, experience);
    }

    @NotNull
    default Optional<DataContainer.GameMode> getGameMode() {
        return Optional.ofNullable((DataContainer.GameMode) getData().get(DataContainer.Type.GAME_MODE));
    }

    default void setGameMode(@NotNull DataContainer.GameMode gameMode) {
        getData().put(DataContainer.Type.GAME_MODE, gameMode);
    }

    @NotNull
    default Optional<DataContainer.PersistentData> getPersistentData() {
        return Optional.ofNullable((DataContainer.PersistentData) getData().get(DataContainer.Type.PERSISTENT_DATA));
    }

    default void setPersistentData(@NotNull DataContainer.PersistentData persistentData) {
        getData().put(DataContainer.Type.PERSISTENT_DATA, persistentData);
    }

}
