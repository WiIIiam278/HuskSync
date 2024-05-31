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

import net.william278.husksync.HuskSync;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Level;

public interface SerializerRegistry {

    // Comparator for ordering identifiers based on dependency
    @NotNull
    @ApiStatus.Internal
    Comparator<Identifier> DEPENDENCY_ORDER_COMPARATOR = new Identifier.DependencyOrderComparator();

    /**
     * Returns the data serializer for the given {@link Identifier}
     *
     * @since 3.0
     */
    @NotNull
    <T extends Data> TreeMap<Identifier, Serializer<T>> getSerializers();

    /**
     * Register a data serializer for the given {@link Identifier}
     *
     * @param identifier the {@link Identifier}
     * @param serializer the {@link Serializer}
     * @since 3.0
     */
    @SuppressWarnings("unchecked")
    default void registerSerializer(@NotNull Identifier identifier,
                                    @NotNull Serializer<? extends Data> serializer) {
        if (identifier.isCustom()) {
            getPlugin().log(Level.INFO, "Registered custom data type: %s".formatted(identifier));
        }
        getSerializers().put(identifier, (Serializer<Data>) serializer);
    }

    /**
     * Ensure dependencies for identifiers that have required dependencies are met
     * <p>
     * This checks the dependencies of all registered identifiers and throws an {@link IllegalStateException}
     * if a dependency has not been registered or enabled via the config
     *
     * @since 3.5.4
     */
    default void validateDependencies() throws IllegalStateException {
        getSerializers().keySet().stream().filter(this::isFeatureEnabled)
                .map(Identifier::getDependencies)
                .flatMap(Collection::stream).distinct()
                .forEach(dependency -> {
                    if (getIdentifier(dependency.getKey().toString()).map(i -> dependency.isRequired()
                            && isFeatureEnabled(i)).orElse(true)) {
                        throw new IllegalStateException("Dependency not met for %s: %s"
                                .formatted(dependency, dependency.getKey()));
                    }
                });
    }

    /**
     * Get the {@link Identifier} for the given key
     *
     * @since 3.0
     */
    default Optional<Identifier> getIdentifier(@NotNull String key) {
        return getSerializers().keySet().stream().filter(identifier -> identifier.toString().equals(key)).findFirst();
    }

    /**
     * Get a data serializer for the given {@link Identifier}
     *
     * @param identifier the {@link Identifier} to get the serializer for
     * @return the {@link Serializer} for the given {@link Identifier}
     * @since 3.5.4
     */
    default Optional<Serializer<? extends Data>> getSerializer(@NotNull Identifier identifier) {
        return Optional.ofNullable(getSerializers().get(identifier));
    }

    /**
     * Get the set of registered data types
     *
     * @return the set of registered data types
     * @since 3.0
     */
    @NotNull
    default Set<Identifier> getRegisteredDataTypes() {
        return getSerializers().keySet();
    }

    // Returns if a feature is enabled in the config
    private boolean isFeatureEnabled(@NotNull Identifier identifier) {
        return getPlugin().getSettings().getSynchronization().isFeatureEnabled(identifier);
    }

    @NotNull
    HuskSync getPlugin();

}
