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

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Statistic;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

// Utility class for adapting "Keyed" Bukkit objects
public final class BukkitKeyedAdapter {

    @Nullable
    public static Statistic matchStatistic(@NotNull String key) {
        return Registry.STATISTIC.get(Objects.requireNonNull(NamespacedKey.fromString(key), "Null key"));
    }

    @Nullable
    public static EntityType matchEntityType(@NotNull String key) {
        return Registry.ENTITY_TYPE.get(Objects.requireNonNull(NamespacedKey.fromString(key), "Null key"));
    }

    @Nullable
    public static Material matchMaterial(@NotNull String key) {
        return Registry.MATERIAL.get(Objects.requireNonNull(NamespacedKey.fromString(key), "Null key"));
    }

    @Nullable
    public static Attribute matchAttribute(@NotNull String key) {
        return Registry.ATTRIBUTE.get(Objects.requireNonNull(NamespacedKey.fromString(key), "Null key"));
    }

}
