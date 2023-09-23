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
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Identifiers of different types of {@link Data}s
 */
public class Identifier {

    public static Identifier INVENTORY = huskSync("inventory", true);
    public static Identifier ENDER_CHEST = huskSync("ender_chest", true);
    public static Identifier POTION_EFFECTS = huskSync("potion_effects", true);
    public static Identifier ADVANCEMENTS = huskSync("advancements", true);
    public static Identifier LOCATION = huskSync("location", false);
    public static Identifier STATISTICS = huskSync("statistics", true);
    public static Identifier HEALTH = huskSync("health", true);
    public static Identifier HUNGER = huskSync("hunger", true);
    public static Identifier EXPERIENCE = huskSync("experience", true);
    public static Identifier GAME_MODE = huskSync("game_mode", true);
    public static Identifier PERSISTENT_DATA = huskSync("persistent_data", true);

    private final Key key;
    private final boolean configDefault;

    private Identifier(@NotNull Key key, boolean configDefault) {
        this.key = key;
        this.configDefault = configDefault;
    }

    /**
     * Create an identifier from a {@link Key}
     *
     * @param key the key
     * @return the identifier
     * @since 3.0
     */
    @NotNull
    public static Identifier from(@NotNull Key key) {
        if (key.namespace().equals("husksync")) {
            throw new IllegalArgumentException("You cannot register a key with \"husksync\" as the namespace!");
        }
        return new Identifier(key, true);
    }

    /**
     * Create an identifier from a namespace and value
     *
     * @param plugin the namespace
     * @param name   the value
     * @return the identifier
     * @since 3.0
     */
    @NotNull
    public static Identifier from(@Subst("plugin") @NotNull String plugin, @Subst("null") @NotNull String name) {
        return from(Key.key(plugin, name));
    }

    @NotNull
    private static Identifier huskSync(@Subst("null") @NotNull String name,
                                       boolean configDefault) throws InvalidKeyException {
        return new Identifier(Key.key("husksync", name), configDefault);
    }

    @NotNull
    @SuppressWarnings("unused")
    private static Identifier parse(@NotNull String key) throws InvalidKeyException {
        return huskSync(key, true);
    }

    public boolean isEnabledByDefault() {
        return configDefault;
    }

    @NotNull
    private Map.Entry<String, Boolean> getConfigEntry() {
        return Map.entry(getKeyValue(), configDefault);
    }

    /**
     * <b>(Internal use only)</b> - Get a map of the default config entries for all HuskSync identifiers
     *
     * @return a map of all the config entries
     * @since 3.0
     */
    @NotNull
    @ApiStatus.Internal
    @SuppressWarnings("unchecked")
    public static Map<String, Boolean> getConfigMap() {
        return Map.ofEntries(Stream.of(
                        INVENTORY, ENDER_CHEST, POTION_EFFECTS, ADVANCEMENTS, LOCATION,
                        STATISTICS, HEALTH, HUNGER, EXPERIENCE, GAME_MODE, PERSISTENT_DATA
                )
                .map(Identifier::getConfigEntry)
                .toArray(Map.Entry[]::new));
    }

    /**
     * Get the namespace of the identifier
     *
     * @return the namespace
     */
    @NotNull
    public String getKeyNamespace() {
        return key.namespace();
    }

    /**
     * Get the value of the identifier
     *
     * @return the value
     */
    @NotNull
    public String getKeyValue() {
        return key.value();
    }

    /**
     * Returns {@code true} if the identifier is a custom (non-HuskSync) identifier
     *
     * @return {@code false} if {@link #getKeyNamespace()} returns "husksync"; {@code true} otherwise
     */
    public boolean isCustom() {
        return !getKeyNamespace().equals("husksync");
    }

    /**
     * Returns the identifier as a string (the key)
     *
     * @return the identifier as a string
     */
    @NotNull
    @Override
    public String toString() {
        return key.asString();
    }

    /**
     * Returns {@code true} if the given object is an identifier with the same key as this identifier
     *
     * @param obj the object to compare
     * @return {@code true} if the given object is an identifier with the same key as this identifier
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Identifier other) {
            return key.equals(other.key);
        }
        return false;
    }

}
