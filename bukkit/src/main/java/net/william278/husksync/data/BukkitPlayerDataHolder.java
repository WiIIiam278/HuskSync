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

import net.william278.husksync.BukkitHuskSync;
import net.william278.husksync.util.MapPersister;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

public interface BukkitPlayerDataHolder extends PlayerDataHolder {

    @Override
    default Optional<? extends Data> getData(@NotNull Identifier identifier) {
        if (identifier.getKeyNamespace().equals("husksync")) {
            return switch (identifier.getKeyValue()) {
                case "inventory" -> getInventory();
                case "ender_chest" -> getEnderChest();
                case "potion_effects" -> getPotionEffects();
                case "advancements" -> getAdvancements();
                case "location" -> getLocation();
                case "statistics" -> getStatistics();
                case "health" -> getHealth();
                case "hunger" -> getHunger();
                case "experience" -> getExperience();
                case "game_mode" -> getGameMode();
                case "persistent_data" -> getPersistentData();
                default -> throw new IllegalStateException(String.format("Unexpected data type: %s", identifier));
            };
        }
        return Optional.ofNullable(getCustomDataStore().get(identifier));
    }

    @Override
    default void setData(@NotNull Identifier identifier, @NotNull Data data) {
        if (!identifier.getKeyNamespace().equals("husksync")) {
            getCustomDataStore().put(identifier, data);
        }
        PlayerDataHolder.super.setData(identifier, data);
    }

    @NotNull
    @Override
    default Optional<Data.Items.Inventory> getInventory() {
        if ((isDead() && !getPlugin().getSettings().doSynchronizeDeadPlayersChangingServer())) {
            return Optional.of(BukkitData.Items.Inventory.empty());
        }
        final PlayerInventory inventory = getBukkitPlayer().getInventory();
        return Optional.of(BukkitData.Items.Inventory.from(
                getMapPersister().persistLockedMaps(inventory.getContents(), getBukkitPlayer()),
                inventory.getHeldItemSlot()
        ));
    }

    @NotNull
    @Override
    default Optional<Data.Items.EnderChest> getEnderChest() {
        return Optional.of(BukkitData.Items.EnderChest.adapt(
                getMapPersister().persistLockedMaps(getBukkitPlayer().getEnderChest().getContents(), getBukkitPlayer())
        ));
    }

    @NotNull
    @Override
    default Optional<Data.PotionEffects> getPotionEffects() {
        return Optional.of(BukkitData.PotionEffects.adapt(getBukkitPlayer().getActivePotionEffects()));
    }

    @NotNull
    @Override
    default Optional<Data.Advancements> getAdvancements() {
        return Optional.of(BukkitData.Advancements.adapt(getBukkitPlayer()));
    }

    @NotNull
    @Override
    default Optional<Data.Location> getLocation() {
        return Optional.of(BukkitData.Location.adapt(getBukkitPlayer().getLocation()));
    }

    @NotNull
    @Override
    default Optional<Data.Statistics> getStatistics() {
        return Optional.of(BukkitData.Statistics.adapt(getBukkitPlayer()));
    }

    @NotNull
    @Override
    default Optional<Data.Health> getHealth() {
        return Optional.of(BukkitData.Health.adapt(getBukkitPlayer()));
    }

    @NotNull
    @Override
    default Optional<Data.Hunger> getHunger() {
        return Optional.of(BukkitData.Hunger.adapt(getBukkitPlayer()));
    }

    @NotNull
    @Override
    default Optional<Data.Experience> getExperience() {
        return Optional.of(BukkitData.Experience.adapt(getBukkitPlayer()));
    }

    @NotNull
    @Override
    default Optional<Data.GameMode> getGameMode() {
        return Optional.of(BukkitData.GameMode.adapt(getBukkitPlayer()));
    }

    @NotNull
    @Override
    default Optional<Data.PersistentData> getPersistentData() {
        return Optional.of(BukkitData.PersistentData.adapt(getBukkitPlayer().getPersistentDataContainer()));
    }

    boolean isDead();

    @NotNull
    Player getBukkitPlayer();

    @NotNull
    Map<Identifier, Data> getCustomDataStore();

    @NotNull
    default MapPersister getMapPersister() {
        return (BukkitHuskSync) getPlugin();
    }


}
