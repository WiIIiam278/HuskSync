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

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

import static net.william278.husksync.config.Settings.SynchronizationSettings.SaveOnDeathSettings;

public interface FabricUserDataHolder extends UserDataHolder {

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
                case "experience" -> getExperience();
                case "game_mode" -> getGameMode();
                case "persistent_data" -> Optional.ofNullable(getCustomDataStore().get(id));
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
        final SaveOnDeathSettings death = getPlugin().getSettings().getSynchronization().getSaveOnDeath();
        if ((isDead() && !death.isSyncDeadPlayersChangingServer())) {
            return Optional.of(FabricData.Items.Inventory.empty());
        }
        final PlayerInventory inventory = getPlayer().getInventory();
        return Optional.of(FabricData.Items.Inventory.from(
                getCombinedInventory(inventory),
                inventory.selectedSlot
        ));
    }

    private ItemStack[] getCombinedInventory(@NotNull PlayerInventory inventory) {
        final ItemStack[] combined = new ItemStack[inventory.main.size() + inventory.armor.size() + inventory.offHand.size()];
        System.arraycopy(inventory.main.toArray(
                        new ItemStack[0]), 0, combined,
                0, inventory.main.size()
        );
        System.arraycopy(
                inventory.armor.toArray(new ItemStack[0]), 0, combined,
                inventory.main.size(), inventory.armor.size()
        );
        System.arraycopy(
                inventory.offHand.toArray(new ItemStack[0]), 0, combined,
                inventory.main.size() + inventory.armor.size(), inventory.offHand.size()
        );
        return combined;
    }

    @NotNull
    @Override
    default Optional<Data.Items.EnderChest> getEnderChest() {
        return Optional.of(FabricData.Items.EnderChest.adapt(
                getPlayer().getEnderChestInventory().stacks
        ));
    }

    @NotNull
    @Override
    default Optional<Data.PotionEffects> getPotionEffects() {
//        return Optional.of(FabricData.PotionEffects.from(getPlayer().getActiveStatusEffects()));
        return Optional.empty();
    }

    @NotNull
    @Override
    default Optional<Data.Advancements> getAdvancements() {
//        return Optional.of(FabricData.Advancements.adapt(getPlayer()));
        return Optional.empty();
    }

    @NotNull
    @Override
    default Optional<Data.Location> getLocation() {
//        return Optional.of(FabricData.Location.adapt(getPlayer()));
        return Optional.empty();
    }

    @NotNull
    @Override
    default Optional<Data.Statistics> getStatistics() {
//        return Optional.of(FabricData.Statistics.adapt(getPlayer()));
        return Optional.empty();
    }

    @NotNull
    @Override
    default Optional<Data.Health> getHealth() {
//        return Optional.of(FabricData.Health.adapt(getPlayer()));
        return Optional.empty();
    }

    @NotNull
    @Override
    default Optional<Data.Hunger> getHunger() {
//        return Optional.of(FabricData.Hunger.adapt(getPlayer()));
        return Optional.empty();
    }

    @NotNull
    @Override
    default Optional<Data.Experience> getExperience() {
//        return Optional.of(FabricData.Experience.adapt(getPlayer()));
        return Optional.empty();
    }

    @NotNull
    @Override
    default Optional<Data.GameMode> getGameMode() {
//        return Optional.of(FabricData.GameMode.adapt(getPlayer()));
        return Optional.empty();
    }

    @NotNull
    @Override
    default Optional<Data.PersistentData> getPersistentData() {
        return Optional.empty();
    }

    boolean isDead();

    @NotNull
    ServerPlayerEntity getPlayer();

    @NotNull
    @Override
    Map<Identifier, Data> getCustomDataStore();

}
