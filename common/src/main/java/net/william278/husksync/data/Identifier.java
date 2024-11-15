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

import lombok.*;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Identifiers of different types of {@link Data}s
 */
@Getter
public class Identifier {

    // Built-in identifiers
    public static final Identifier PERSISTENT_DATA = huskSync("persistent_data", true);
    public static final Identifier INVENTORY = huskSync("inventory", true);
    public static final Identifier ENDER_CHEST = huskSync("ender_chest", true);
    public static final Identifier ADVANCEMENTS = huskSync("advancements", true);
    public static final Identifier STATISTICS = huskSync("statistics", true);
    public static final Identifier POTION_EFFECTS = huskSync("potion_effects", true);
    public static final Identifier GAME_MODE = huskSync("game_mode", true);
    public static final Identifier FLIGHT_STATUS = huskSync("flight_status", true,
            Dependency.optional("game_mode")
    );
    public static final Identifier ATTRIBUTES = huskSync("attributes", true,
            Dependency.optional("inventory"),
            Dependency.optional("potion_effects")
    );
    public static final Identifier HEALTH = huskSync("health", true,
            Dependency.optional("attributes")
    );
    public static final Identifier HUNGER = huskSync("hunger", true,
            Dependency.optional("attributes")
    );
    public static final Identifier EXPERIENCE = huskSync("experience", true,
            Dependency.optional("advancements")
    );
    public static final Identifier LOCATION = huskSync("location", false,
            Dependency.optional("flight_status"),
            Dependency.optional("potion_effects")
    );

    private final Key key;
    private final boolean enabledByDefault;
    @Getter
    private final Set<Dependency> dependencies;
    @Setter
    @Getter
    public boolean enabled;

    private Identifier(@NotNull Key key, boolean enabledByDefault, @NotNull Set<Dependency> dependencies) {
        this.key = key;
        this.enabledByDefault = enabledByDefault;
        this.enabled = enabledByDefault;
        this.dependencies = dependencies;
    }

    /**
     * Create an identifier from a {@link Key}
     *
     * @param key          the key
     * @param dependencies the dependencies
     * @return the identifier
     * @since 3.5.4
     */
    @NotNull
    public static Identifier from(@NotNull Key key, @NotNull Set<Dependency> dependencies) {
        if (key.namespace().equals("husksync")) {
            throw new IllegalArgumentException("You cannot register a key with \"husksync\" as the namespace!");
        }
        return new Identifier(key, true, dependencies);
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
        return from(key, Collections.emptySet());
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

    /**
     * Create an identifier from a namespace, value, and dependencies
     *
     * @param plugin       the namespace
     * @param name         the value
     * @param dependencies the dependencies
     * @return the identifier
     * @since 3.5.4
     */
    @NotNull
    public static Identifier from(@Subst("plugin") @NotNull String plugin, @Subst("null") @NotNull String name,
                                  @NotNull Set<Dependency> dependencies) {
        return from(Key.key(plugin, name), dependencies);
    }

    // Return an identifier with a HuskSync namespace
    @NotNull
    private static Identifier huskSync(@Subst("null") @NotNull String name,
                                       boolean configDefault) throws InvalidKeyException {
        return new Identifier(Key.key("husksync", name), configDefault, Collections.emptySet());
    }

    // Return an identifier with a HuskSync namespace
    @NotNull
    private static Identifier huskSync(@Subst("null") @NotNull String name,
                                       @SuppressWarnings("SameParameterValue") boolean configDefault,
                                       @NotNull Dependency... dependents) throws InvalidKeyException {
        return new Identifier(Key.key("husksync", name), configDefault, Set.of(dependents));
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
                        INVENTORY, ENDER_CHEST, POTION_EFFECTS, ADVANCEMENTS, LOCATION, STATISTICS,
                        HEALTH, HUNGER, ATTRIBUTES, EXPERIENCE, GAME_MODE, FLIGHT_STATUS, PERSISTENT_DATA
                )
                .map(Identifier::getConfigEntry)
                .toArray(Map.Entry[]::new));
    }

    /**
     * Returns {@code true} if the identifier depends on the given identifier
     *
     * @param identifier the identifier to check
     * @return {@code true} if the identifier depends on the given identifier
     * @since 3.5.4
     */
    public boolean dependsOn(@NotNull Identifier identifier) {
        return dependencies.contains(Dependency.required(identifier.key));
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
    public boolean equals(@Nullable Object obj) {
        return obj instanceof Identifier other ? toString().equals(other.toString()) : super.equals(obj);
    }

    // Get the config entry for the identifier
    @NotNull
    private Map.Entry<String, Boolean> getConfigEntry() {
        return Map.entry(getKeyValue(), enabledByDefault);
    }

    /**
     * Compares two identifiers based on their dependencies.
     * <p>
     * If this identifier contains a dependency on the other, it should come after & vice versa
     *
     * @since 3.5.4
     */
    @NoArgsConstructor(access = AccessLevel.PACKAGE)
    static class DependencyOrderComparator implements Comparator<Identifier> {

        @Override
        public int compare(@NotNull Identifier i1, @NotNull Identifier i2) {
            if (i1.equals(i2)) {
                return 0;
            }
            if (i1.dependsOn(i2)) {
                if (i2.dependsOn(i1)) {
                    throw new IllegalArgumentException(
                            "Found circular dependency between %s and %s".formatted(i1.getKey(), i2.getKey())
                    );
                }
                return 1;
            }
            return -1;
        }

    }

    /**
     * Represents a data dependency of an identifier, used to determine the order in which data is applied to users
     *
     * @since 3.5.4
     */
    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Dependency {
        /**
         * Key of the data dependency see {@code Identifier#key()}
         */
        private Key key;
        /**
         * Whether the data dependency is required to be present & enabled for the dependant data to enabled
         */
        private boolean required;

        @NotNull
        protected static Dependency required(@NotNull Key identifier) {
            return new Dependency(identifier, true);
        }

        @NotNull
        public static Dependency optional(@NotNull Key identifier) {
            return new Dependency(identifier, false);
        }

        @NotNull
        @SuppressWarnings("SameParameterValue")
        private static Dependency required(@Subst("null") @NotNull String identifier) {
            return required(Key.key("husksync", identifier));
        }

        @NotNull
        private static Dependency optional(@Subst("null") @NotNull String identifier) {
            return optional(Key.key("husksync", identifier));
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Dependency other) {
                return key.equals(other.key);
            }
            return false;
        }
    }

}
