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
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A builder utility for creating {@link UserData} instances
 *
 * @since 2.1
 */
@SuppressWarnings("UnusedReturnValue")
public class UserDataBuilder {

    @NotNull
    private final UserData userData;

    protected UserDataBuilder(@NotNull String minecraftVersion) {
        this.userData = new UserData();
        this.userData.minecraftVersion = minecraftVersion;
    }

    /**
     * Set the {@link StatusData} to this {@link UserData}
     *
     * @param status the {@link StatusData} to set
     * @return this {@link UserDataBuilder}
     * @since 2.1
     */
    @NotNull
    public UserDataBuilder setStatus(@NotNull StatusData status) {
        this.userData.statusData = status;
        return this;
    }

    /**
     * Set the inventory {@link ItemData} to this {@link UserData}
     *
     * @param inventoryData the inventory {@link ItemData} to set
     * @return this {@link UserDataBuilder}
     * @since 2.1
     */
    @NotNull
    public UserDataBuilder setInventory(@Nullable ItemData inventoryData) {
        this.userData.inventoryData = inventoryData;
        return this;
    }

    /**
     * Set the ender chest {@link ItemData} to this {@link UserData}
     *
     * @param enderChestData the ender chest {@link ItemData} to set
     * @return this {@link UserDataBuilder}
     * @since 2.1
     */
    @NotNull
    public UserDataBuilder setEnderChest(@Nullable ItemData enderChestData) {
        this.userData.enderChestData = enderChestData;
        return this;
    }

    /**
     * Set the {@link List} of {@link ItemData} to this {@link UserData}
     *
     * @param potionEffectData the {@link List} of {@link ItemData} to set
     * @return this {@link UserDataBuilder}
     * @since 2.1
     */
    @NotNull
    public UserDataBuilder setPotionEffects(@Nullable PotionEffectData potionEffectData) {
        this.userData.potionEffectData = potionEffectData;
        return this;
    }

    /**
     * Set the {@link List} of {@link ItemData} to this {@link UserData}
     *
     * @param advancementData the {@link List} of {@link ItemData} to set
     * @return this {@link UserDataBuilder}
     * @since 2.1
     */
    @NotNull
    public UserDataBuilder setAdvancements(@Nullable List<AdvancementData> advancementData) {
        this.userData.advancementData = advancementData;
        return this;
    }

    /**
     * Set the {@link StatisticsData} to this {@link UserData}
     *
     * @param statisticData the {@link StatisticsData} to set
     * @return this {@link UserDataBuilder}
     * @since 2.1
     */
    @NotNull
    public UserDataBuilder setStatistics(@Nullable StatisticsData statisticData) {
        this.userData.statisticData = statisticData;
        return this;
    }


    /**
     * Set the {@link LocationData} to this {@link UserData}
     *
     * @param locationData the {@link LocationData} to set
     * @return this {@link UserDataBuilder}
     * @since 2.1
     */
    @NotNull
    public UserDataBuilder setLocation(@Nullable LocationData locationData) {
        this.userData.locationData = locationData;
        return this;
    }

    /**
     * Set the {@link PersistentDataContainerData} to this {@link UserData}
     *
     * @param persistentDataContainerData the {@link PersistentDataContainerData} to set
     * @return this {@link UserDataBuilder}
     * @since 2.1
     */
    @NotNull
    public UserDataBuilder setPersistentDataContainer(@Nullable PersistentDataContainerData persistentDataContainerData) {
        this.userData.persistentDataContainerData = persistentDataContainerData;
        return this;
    }

    /**
     * Build and get the {@link UserData} instance
     *
     * @return the {@link UserData} instance
     * @since 2.1
     */
    @NotNull
    public UserData build() {
        return this.userData;
    }

}
