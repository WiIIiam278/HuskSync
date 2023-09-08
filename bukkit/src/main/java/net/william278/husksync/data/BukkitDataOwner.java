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

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface BukkitDataOwner extends DataOwner {

    @NotNull
    @Override
    default Optional<DataContainer.Items.Inventory> getInventory() {
        //todo check if dead and apply special rule
        return Optional.of(BukkitDataContainer.Items.Inventory.adapt(getBukkitPlayer()));
    }

    @NotNull
    @Override
    default Optional<DataContainer.Items.EnderChest> getEnderChest() {
        return Optional.of(BukkitDataContainer.Items.EnderChest.adapt(getBukkitPlayer().getEnderChest().getContents()));
    }

    @NotNull
    @Override
    default Optional<DataContainer.PotionEffects> getPotionEffects() {
        return Optional.of(BukkitDataContainer.PotionEffects.adapt(getBukkitPlayer().getActivePotionEffects()));
    }

    @NotNull
    @Override
    default Optional<DataContainer.Advancements> getAdvancements() {
        return Optional.of(BukkitDataContainer.Advancements.adapt(getBukkitPlayer(), getPlugin()));
    }

    @NotNull
    @Override
    default Optional<DataContainer.Location> getLocation() {
        return Optional.of(BukkitDataContainer.Location.adapt(getBukkitPlayer().getLocation()));
    }

    @NotNull
    @Override
    default Optional<DataContainer.Statistics> getStatistics() {
        return Optional.of(BukkitDataContainer.Statistics.adapt(getBukkitPlayer()));
    }

    @NotNull
    @Override
    default Optional<DataContainer.Health> getHealth() {
        return Optional.of(BukkitDataContainer.Health.adapt(getBukkitPlayer()));
    }

    @NotNull
    @Override
    default Optional<DataContainer.Food> getFood() {
        return Optional.of(BukkitDataContainer.Food.adapt(getBukkitPlayer()));
    }

    @NotNull
    @Override
    default Optional<DataContainer.Experience> getExperience() {
        return Optional.of(BukkitDataContainer.Experience.adapt(getBukkitPlayer()));
    }

    @NotNull
    @Override
    default Optional<DataContainer.GameMode> getGameMode() {
        return Optional.of(BukkitDataContainer.GameMode.adapt(getBukkitPlayer()));
    }

    @NotNull
    @Override
    default Optional<DataContainer.PersistentData> getPersistentData() {
        return Optional.of(BukkitDataContainer.PersistentData.adapt(getBukkitPlayer().getPersistentDataContainer()));
    }

    @NotNull
    Player getBukkitPlayer();

}
