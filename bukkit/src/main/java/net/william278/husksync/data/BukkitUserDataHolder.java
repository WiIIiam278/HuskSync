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
import net.william278.husksync.util.BukkitMapPersister;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface BukkitUserDataHolder extends UserDataHolder {

    @Override
    default Optional<? extends Data> getData(@NotNull Identifier id) {
        if (!id.isCustom()) {
            return switch (id.getKeyValue()) {
                case "inventory" -> getInventory();
                case "ender_chest" -> getEnderChest();
                case "potion_effects" -> getPotionEffects();
                case "advancements" -> getAdvancements();
                case "location" -> getLocation();
                case "statistics" -> getStatistics();
                case "health" -> getHealth();
                case "hunger" -> getHunger();
                case "attributes" -> getAttributes();
                case "experience" -> getExperience();
                case "game_mode" -> getGameMode();
                case "flight_status" -> getFlightStatus();
                case "persistent_data" -> getPersistentData();
                default -> throw new IllegalStateException(String.format("Unexpected data type: %s", id));
            };
        }
        return Optional.ofNullable(getCustomDataStore().get(id));
    }

    @Override
    default void setData(@NotNull Identifier id, @NotNull Data data) {
        if (id.isCustom()) {
            getCustomDataStore().put(id, data);
        }
        UserDataHolder.super.setData(id, data);
    }

    @NotNull
    @Override
    default Optional<Data.Items.Inventory> getInventory() {
        if ((isDead() && !getPlugin().getSettings().getSynchronization().getSaveOnDeath()
                .isSyncDeadPlayersChangingServer())) {
            return Optional.of(BukkitData.Items.Inventory.empty());
        }
        final PlayerInventory inventory = getPlayer().getInventory();
        return Optional.of(BukkitData.Items.Inventory.from(
                getMapPersister().persistLockedMaps(inventory.getContents(), getPlayer()),
                inventory.getHeldItemSlot()
        ));
    }

    @NotNull
    @Override
    default Optional<Data.Items.EnderChest> getEnderChest() {
        return Optional.of(BukkitData.Items.EnderChest.adapt(
                getMapPersister().persistLockedMaps(getPlayer().getEnderChest().getContents(), getPlayer())
        ));
    }

    @NotNull
    @Override
    default Optional<Data.PotionEffects> getPotionEffects() {
        return Optional.of(BukkitData.PotionEffects.from(getPlayer().getActivePotionEffects()));
    }

    @NotNull
    @Override
    default Optional<Data.Advancements> getAdvancements() {
        return Optional.of(BukkitData.Advancements.adapt(getPlayer()));
    }

    @NotNull
    @Override
    default Optional<Data.Location> getLocation() {
        return Optional.of(BukkitData.Location.adapt(getPlayer().getLocation()));
    }

    @NotNull
    @Override
    default Optional<Data.Statistics> getStatistics() {
        return Optional.of(BukkitData.Statistics.adapt(getPlayer()));
    }

    @NotNull
    @Override
    default Optional<Data.Health> getHealth() {
        return Optional.of(BukkitData.Health.adapt(getPlayer()));
    }

    @NotNull
    @Override
    default Optional<Data.Hunger> getHunger() {
        return Optional.of(BukkitData.Hunger.adapt(getPlayer()));
    }

    @NotNull
    @Override
    default Optional<Data.Attributes> getAttributes() {
        return Optional.of(BukkitData.Attributes.adapt(getPlayer(), getPlugin()));
    }

    @NotNull
    @Override
    default Optional<Data.Experience> getExperience() {
        return Optional.of(BukkitData.Experience.adapt(getPlayer()));
    }

    @NotNull
    @Override
    default Optional<Data.GameMode> getGameMode() {
        return Optional.of(BukkitData.GameMode.adapt(getPlayer()));
    }

    @NotNull
    @Override
    default Optional<Data.FlightStatus> getFlightStatus() {
        return Optional.of(BukkitData.FlightStatus.adapt(getPlayer()));
    }

    @NotNull
    @Override
    default Optional<Data.PersistentData> getPersistentData() {
        return Optional.of(BukkitData.PersistentData.adapt(getPlayer().getPersistentDataContainer()));
    }

    boolean isDead();

    @NotNull
    Player getPlayer();

    /**
     * @deprecated Use {@link #getPlayer()} instead
     */
    @Deprecated(since = "3.6")
    @NotNull
    default Player getBukkitPlayer() {
        return getPlayer();
    }

    @NotNull
    default BukkitMapPersister getMapPersister() {
        return (BukkitHuskSync) getPlugin();
    }


}
