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

package net.william278.husksync.player;

import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.audience.Audience;
import net.william278.desertwell.util.Version;
import net.william278.husksync.config.Settings;
import net.william278.husksync.data.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class DummyPlayer extends OnlineUser {

    private DummyPlayer() {
        super(UUID.fromString("00000000-0000-0000-0000-000000000000"),
                "DummyPlayer");
    }

    public static DummyPlayer create() {
        return new DummyPlayer();
    }

    @Override
    @NotNull
    public StatusData getStatus() {
        return new StatusData(20, 20, 0,
                20, 5, 5, 1,
                100, 1, 1f, "SURVIVAL", false);
    }

    @Override
    public void setStatus(@NotNull StatusData statusData, @NotNull Settings settings) {
        // do nothing
    }

    @NotNull
    @Override
    public ItemData getInventory() {
        return new ItemData("");
    }

    @Override
    public void setInventory(@NotNull ItemData itemData) {
        // do nothing
    }

    @NotNull
    @Override
    public ItemData getEnderChest() {
        return new ItemData("");
    }

    @Override
    public void setEnderChest(@NotNull ItemData enderChestData) {
        // do nothing
    }

    @NotNull
    @Override
    public PotionEffectData getPotionEffects() {
        return new PotionEffectData("");
    }

    @Override
    public void setPotionEffects(@NotNull PotionEffectData potionEffectData) {
        // do nothing
    }

    @NotNull
    @Override
    public List<AdvancementData> getAdvancements() {
        return new ArrayList<>();
    }

    @Override
    public void setAdvancements(@NotNull List<AdvancementData> advancementData) {
        // do nothing
    }

    @NotNull
    @Override
    public StatisticsData getStatistics() {
        return new StatisticsData(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    @Override
    public void setStatistics(@NotNull StatisticsData statisticsData) {
        // do nothing
    }

    @Override
    public LocationData getLocation() {
        return new LocationData(
                "dummy_world",
                UUID.fromString("00000000-0000-0000-0000-000000000000"),
                "NORMAL",
                0, 64, 0,
                90f, 180f
        );
    }

    @Override
    public void setLocation(@NotNull LocationData locationData) {
        // do nothing
    }

    @NotNull
    @Override
    public PersistentDataContainerData getPersistentDataContainer() {
        return new PersistentDataContainerData(new HashMap<>());
    }

    @Override
    public void setPersistentDataContainer(@NotNull PersistentDataContainerData persistentDataContainerData) {
        // do nothing
    }

    @Override
    public boolean isOffline() {
        return false;
    }

    @NotNull
    @Override
    public Version getMinecraftVersion() {
        return Version.fromString("1.19-beta123456");
    }

    @Override
    @NotNull
    public Audience getAudience() {
        return Audience.empty();
    }

    @Override
    public void sendMessage(@NotNull MineDown mineDown) {
        // do nothing
    }

    @Override
    public void sendActionBar(@NotNull MineDown mineDown) {
        // do nothing
    }

    @Override
    public void sendToast(@NotNull MineDown title, @NotNull MineDown description,
                          @NotNull String iconMaterial, @NotNull String backgroundType) {
        // do nothing
    }

    @Override
    public boolean hasPermission(@NotNull String node) {
        return true;
    }

    @Override
    public CompletableFuture<Optional<ItemData>> showMenu(@NotNull ItemData itemData, boolean editable,
                                                          int minimumRows, @NotNull MineDown title) {
        // do nothing
        return CompletableFuture.completedFuture(Optional.empty());
    }


    @Override
    public boolean isDead() {
        return false;
    }

    @Override
    public boolean isLocked() {
        return false;
    }

    @Override
    public boolean isNpc() {
        return false;
    }

}
