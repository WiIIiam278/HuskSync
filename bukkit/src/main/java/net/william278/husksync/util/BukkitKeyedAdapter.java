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

package net.william278.husksync.util;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// Utility class for adapting "Keyed" Bukkit objects
public final class BukkitKeyedAdapter {

    @Nullable
    public static Statistic matchStatistic(@NotNull String key) {
        return getRegistryValue(Registry.STATISTIC, key);
    }

    @Nullable
    public static EntityType matchEntityType(@NotNull String key) {
        return getRegistryValue(Registry.ENTITY_TYPE, key);
    }

    @Nullable
    public static Material matchMaterial(@NotNull String key) {
        return getRegistryValue(Registry.MATERIAL, key);
    }

    @Nullable
    public static Attribute matchAttribute(@NotNull String key) {
        return getRegistryValue(Registry.ATTRIBUTE, key);
    }

    @Nullable
    public static PotionEffectType matchEffectType(@NotNull String key) {
        return getRegistryValue(Registry.EFFECT, key);
    }

    private static <T extends Keyed> T getRegistryValue(@NotNull Registry<T> registry, @NotNull String keyString) {
        final NamespacedKey key = NamespacedKey.fromString(keyString);
        return key != null ? registry.get(key) : null;
    }

}
