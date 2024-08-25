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
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

import static net.william278.husksync.config.Settings.SynchronizationSettings.SaveOnDeathSettings;

public interface FabricUserDataHolder extends UserDataHolder {

    @Override
    default Optional<? extends Data> getData(@NotNull Identifier id) {
        if (!id.isCustom()) {
            try {
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
            } catch (Throwable e) {
                getPlugin().debug("Failed to get data for key: " + id.getKeyValue(), e);
            }
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

    // Gets the player's combined inventory; their inventory, plus offhand and armor.
    @Nullable
    private ItemStack @NotNull [] getCombinedInventory(@NotNull PlayerInventory inv) {
        final ItemStack[] combined = new ItemStack[inv.main.size() + inv.armor.size() + inv.offHand.size()];
        System.arraycopy(
                inv.main.toArray(new ItemStack[0]), 0, combined,
                0, inv.main.size()
        );
        System.arraycopy(
                inv.armor.toArray(new ItemStack[0]), 0, combined,
                inv.main.size(), inv.armor.size()
        );
        System.arraycopy(
                inv.offHand.toArray(new ItemStack[0]), 0, combined,
                inv.main.size() + inv.armor.size(), inv.offHand.size()
        );
        return combined;
    }

    @NotNull
    @Override
    default Optional<Data.Items.EnderChest> getEnderChest() {
        return Optional.of(FabricData.Items.EnderChest.adapt(
                getPlayer().getEnderChestInventory().getHeldStacks()
        ));
    }

    @NotNull
    @Override
    default Optional<Data.PotionEffects> getPotionEffects() {
        return Optional.of(FabricData.PotionEffects.from(getPlayer().getActiveStatusEffects().values()));
    }

    @NotNull
    @Override
    default Optional<Data.Advancements> getAdvancements() {
        return Optional.of(FabricData.Advancements.adapt(getPlayer()));
    }

    @NotNull
    @Override
    default Optional<Data.Location> getLocation() {
        return Optional.of(FabricData.Location.adapt(getPlayer()));
    }

    @NotNull
    @Override
    default Optional<Data.Statistics> getStatistics() {
        return Optional.of(FabricData.Statistics.adapt(getPlayer()));
    }

    @Override
    @NotNull
    default Optional<Data.Attributes> getAttributes() {
        return Optional.of(FabricData.Attributes.adapt(getPlayer(), getPlugin()));
    }

    @NotNull
    @Override
    default Optional<Data.Health> getHealth() {
        return Optional.of(FabricData.Health.adapt(getPlayer()));
    }

    @NotNull
    @Override
    default Optional<Data.Hunger> getHunger() {
        return Optional.of(FabricData.Hunger.adapt(getPlayer()));
    }

    @NotNull
    @Override
    default Optional<Data.Experience> getExperience() {
        return Optional.of(FabricData.Experience.adapt(getPlayer()));
    }

    @NotNull
    @Override
    default Optional<Data.GameMode> getGameMode() {
        return Optional.of(FabricData.GameMode.adapt(getPlayer()));
    }

    @NotNull
    @Override
    default Optional<Data.FlightStatus> getFlightStatus() {
        return Optional.of(FabricData.FlightStatus.adapt(getPlayer()));
    }

    @NotNull
    @Override
    default Optional<Data.PersistentData> getPersistentData() {
        return Optional.empty(); // Not implemented on Fabric, but maybe we'll do data keys or something
    }

    boolean isDead();

    @NotNull
    ServerPlayerEntity getPlayer();

    @NotNull
    @Override
    Map<Identifier, Data> getCustomDataStore();

}
