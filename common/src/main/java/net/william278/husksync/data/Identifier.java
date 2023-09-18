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

import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Identifiers of different types of {@link Data}s
 */
public class Identifier {

    public static Identifier INVENTORY = from("inventory", true);
    public static Identifier ENDER_CHEST = from("ender_chest", true);
    public static Identifier POTION_EFFECTS = from("potion_effects", true);
    public static Identifier ADVANCEMENTS = from("advancements", true);
    public static Identifier LOCATION = from("location", false);
    public static Identifier STATISTICS = from("statistics", true);
    public static Identifier HEALTH = from("health", true);
    public static Identifier HUNGER = from("hunger", true);
    public static Identifier EXPERIENCE = from("experience", true);
    public static Identifier GAME_MODE = from("game_mode", true);
    public static Identifier PERSISTENT_DATA = from("persistent_data", true);

    private final Key key;
    private final boolean configDefault;

    private Identifier(@NotNull Key key, boolean configDefault) {
        this.key = key;
        this.configDefault = configDefault;
    }

    @NotNull
    private static Identifier from(@Subst("null") @NotNull String name, boolean configDefault) throws InvalidKeyException {
        return new Identifier(Key.key("husksync", name), configDefault);
    }

    @NotNull
    @SuppressWarnings("unused")
    private static Identifier parse(@NotNull String key) throws InvalidKeyException {
        return from(key, true);
    }

    @NotNull
    public static Identifier from(@NotNull Key key) {
        return new Identifier(key, false);
    }


    public boolean isEnabledByDefault() {
        return configDefault;
    }

    @NotNull
    private Map.Entry<String, Boolean> getConfigEntry() {
        return Map.entry(getKeyValue(), configDefault);
    }

    @NotNull
    public static Map<String, Boolean> getConfigMap() {
        return Map.ofEntries(Stream.of(
                        INVENTORY, ENDER_CHEST, POTION_EFFECTS, ADVANCEMENTS, LOCATION,
                        STATISTICS, HEALTH, HUNGER, EXPERIENCE, GAME_MODE, PERSISTENT_DATA
                )
                .map(Identifier::getConfigEntry)
                .toArray(Map.Entry[]::new));
    }

    @NotNull
    public String getKeyNamespace() {
        return key.namespace();
    }

    @NotNull
    public String getKeyValue() {
        return key.value();
    }

    public boolean isCustom() {
        return !getKeyNamespace().equals("husksync");
    }

    @Override
    @NotNull
    public String toString() {
        return key.asString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Identifier other) {
            return key.equals(other.key);
        }
        return false;
    }

}
