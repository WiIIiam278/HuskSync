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

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// Utility class for adapting "Keyed" Minecraft objects
public final class FabricKeyedAdapter {

    @Nullable
    public static EntityType<?> matchEntityType(@NotNull String key) {
        return getRegistryValue(Registries.ENTITY_TYPE, key);
    }

    @Nullable
    public static String getEntityTypeId(@NotNull EntityType<?> entityType) {
        return getRegistryKey(Registries.ENTITY_TYPE, entityType);
    }

    @Nullable
    public static EntityAttribute matchAttribute(@NotNull String key) {
        return getRegistryValue(Registries.ATTRIBUTE, key);
    }

    @Nullable
    public static String getAttributeId(@NotNull EntityAttribute attribute) {
        return getRegistryKey(Registries.ATTRIBUTE, attribute);
    }

    @Nullable
    public static StatusEffect matchEffectType(@NotNull String key) {
        return getRegistryValue(Registries.STATUS_EFFECT, key);
    }

    @Nullable
    public static String getEffectId(@NotNull StatusEffect effect) {
        return getRegistryKey(Registries.STATUS_EFFECT, effect);
    }

    @Nullable
    private static <T> T getRegistryValue(@NotNull Registry<T> registry, @NotNull String keyString) {
        final Identifier key = Identifier.tryParse(keyString);
        return key != null ? registry.get(key) : null;
    }

    @Nullable
    private static <T> String getRegistryKey(@NotNull Registry<T> registry, @NotNull T value) {
        final Identifier key = registry.getId(value);
        return key != null ? key.toString() : null;
    }

}
