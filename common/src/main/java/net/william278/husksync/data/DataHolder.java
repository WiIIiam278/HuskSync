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

@SuppressWarnings("unused")
public interface DataHolder {

    @NotNull
    Map<Identifier, Data> getData();

    default Optional<? extends Data> getData(@NotNull Identifier id) {
        return getData().entrySet().stream().filter(e -> e.getKey().equals(id)).map(Map.Entry::getValue).findFirst();
    }

    default void setData(@NotNull Identifier identifier, @NotNull Data data) {
        getData().put(identifier, data);
    }

    @NotNull
    default Optional<Data.Items.Inventory> getInventory() {
        return getData(Identifier.INVENTORY).map(Data.Items.Inventory.class::cast);
    }

    default void setInventory(@NotNull Data.Items.Inventory inventory) {
        setData(Identifier.INVENTORY, inventory);
    }

    @NotNull
    default Optional<Data.Items.EnderChest> getEnderChest() {
        return getData(Identifier.ENDER_CHEST).map(Data.Items.EnderChest.class::cast);
    }

    default void setEnderChest(@NotNull Data.Items.EnderChest enderChest) {
        setData(Identifier.ENDER_CHEST, enderChest);
    }

    @NotNull
    default Optional<Data.PotionEffects> getPotionEffects() {
        return getData(Identifier.POTION_EFFECTS).map(Data.PotionEffects.class::cast);
    }

    default void setPotionEffects(@NotNull Data.PotionEffects potionEffects) {
        setData(Identifier.POTION_EFFECTS, potionEffects);
    }

    @NotNull
    default Optional<Data.Advancements> getAdvancements() {
        return getData(Identifier.ADVANCEMENTS).map(Data.Advancements.class::cast);
    }

    default void setAdvancements(@NotNull Data.Advancements advancements) {
        setData(Identifier.ADVANCEMENTS, advancements);
    }

    @NotNull
    default Optional<Data.Location> getLocation() {
        return Optional.ofNullable((Data.Location) getData().get(Identifier.LOCATION));
    }

    default void setLocation(@NotNull Data.Location location) {
        getData().put(Identifier.LOCATION, location);
    }

    @NotNull
    default Optional<Data.Statistics> getStatistics() {
        return Optional.ofNullable((Data.Statistics) getData().get(Identifier.STATISTICS));
    }

    default void setStatistics(@NotNull Data.Statistics statistics) {
        getData().put(Identifier.STATISTICS, statistics);
    }

    @NotNull
    default Optional<Data.Health> getHealth() {
        return Optional.ofNullable((Data.Health) getData().get(Identifier.HEALTH));
    }

    default void setHealth(@NotNull Data.Health health) {
        getData().put(Identifier.HEALTH, health);
    }

    @NotNull
    default Optional<Data.Hunger> getHunger() {
        return Optional.ofNullable((Data.Hunger) getData().get(Identifier.HUNGER));
    }

    default void setHunger(@NotNull Data.Hunger hunger) {
        getData().put(Identifier.HUNGER, hunger);
    }

    @NotNull
    default Optional<Data.Attributes> getAttributes() {
        return Optional.ofNullable((Data.Attributes) getData().get(Identifier.ATTRIBUTES));
    }

    default void setAttributes(@NotNull Data.Attributes attributes) {
        getData().put(Identifier.ATTRIBUTES, attributes);
    }

    @NotNull
    default Optional<Data.Experience> getExperience() {
        return Optional.ofNullable((Data.Experience) getData().get(Identifier.EXPERIENCE));
    }

    default void setExperience(@NotNull Data.Experience experience) {
        getData().put(Identifier.EXPERIENCE, experience);
    }

    @NotNull
    default Optional<Data.GameMode> getGameMode() {
        return Optional.ofNullable((Data.GameMode) getData().get(Identifier.GAME_MODE));
    }

    default void setGameMode(@NotNull Data.GameMode gameMode) {
        getData().put(Identifier.GAME_MODE, gameMode);
    }

    @NotNull
    default Optional<Data.FlightStatus> getFlightStatus() {
        return Optional.ofNullable((Data.FlightStatus) getData().get(Identifier.FLIGHT_STATUS));
    }

    default void setFlightStatus(@NotNull Data.FlightStatus flightStatus) {
        getData().put(Identifier.FLIGHT_STATUS, flightStatus);
    }

    @NotNull
    default Optional<Data.PersistentData> getPersistentData() {
        return Optional.ofNullable((Data.PersistentData) getData().get(Identifier.PERSISTENT_DATA));
    }

    default void setPersistentData(@NotNull Data.PersistentData persistentData) {
        getData().put(Identifier.PERSISTENT_DATA, persistentData);
    }

}
