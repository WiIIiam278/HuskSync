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

import net.william278.desertwell.util.Version;
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
     * @param id         the {@link Identifier}
     * @param serializer the {@link Serializer}
     * @since 3.0
     */
    @SuppressWarnings("unchecked")
    default void registerSerializer(@NotNull Identifier id, @NotNull Serializer<? extends Data> serializer) {
        if (id.isCustom()) {
            getPlugin().log(Level.INFO, "Registered custom data type: %s".formatted(id));
        }
        id.setEnabled(id.isCustom() || getPlugin().getSettings().getSynchronization().isFeatureEnabled(id));
        getSerializers().put(id, (Serializer<Data>) serializer);
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
        getSerializers().keySet().stream().filter(Identifier::isEnabled)
                .forEach(identifier -> {
                    final List<String> unmet = identifier.getDependencies().stream()
                            .filter(Identifier.Dependency::isRequired)
                            .filter(dep -> !isDataTypeAvailable(dep.getKey().asString()))
                            .map(dep -> dep.getKey().asString()).toList();
                    if (!unmet.isEmpty()) {
                        identifier.setEnabled(false);
                        getPlugin().log(Level.WARNING, "Disabled %s syncing as the following types need to be on: %s"
                                .formatted(identifier, String.join(", ", unmet)));
                    }
                });
    }

    /**
     * Get the {@link Identifier} for the given key
     *
     * @since 3.0
     */
    default Optional<Identifier> getIdentifier(@NotNull String key) {
        return getSerializers().keySet().stream()
                .filter(id -> id.getKey().asString().equals(key)).findFirst();
    }

    /**
     * Get a data serializer for the given {@link Identifier}
     *
     * @param identifier the {@link Identifier} to get the serializer for
     * @return the {@link Serializer} for the given {@link Identifier}
     * @since 3.5.4
     */
    default Optional<Serializer<Data>> getSerializer(@NotNull Identifier identifier) {
        return getSerializers().entrySet().stream()
                .filter(entry -> entry.getKey().getKey().equals(identifier.getKey()))
                .map(Map.Entry::getValue).findFirst();
    }

    /**
     * Serialize data for the given {@link Identifier}
     *
     * @param identifier the {@link Identifier} to serialize data for
     * @param data       the data to serialize
     * @return the serialized data
     * @throws IllegalArgumentException if no serializer is found for the given {@link Identifier}
     * @since 3.5.4
     */
    @NotNull
    default String serializeData(@NotNull Identifier identifier, @NotNull Data data) throws IllegalStateException {
        return getSerializer(identifier).map(serializer -> serializer.serialize(data))
                .orElseThrow(() -> new IllegalStateException("No serializer found for %s".formatted(identifier)));
    }

    /**
     * Deserialize data of a given {@link Version Minecraft version} for the given {@link Identifier data identifier}
     *
     * @param identifier    the {@link Identifier} to deserialize data for
     * @param data          the data to deserialize
     * @param dataMcVersion the Minecraft version of the data
     * @return the deserialized data
     * @throws IllegalStateException if no serializer is found for the given {@link Identifier}
     * @since 3.6.4
     */
    @NotNull
    default Data deserializeData(@NotNull Identifier identifier, @NotNull String data,
                                 @NotNull Version dataMcVersion) throws IllegalStateException {
        return getSerializer(identifier).map(serializer -> serializer.deserialize(data, dataMcVersion)).orElseThrow(
                () -> new IllegalStateException("No serializer found for %s".formatted(identifier))
        );
    }

    /**
     * Deserialize data for the given {@link Identifier data identifier}
     *
     * @param identifier the {@link Identifier} to deserialize data for
     * @param data       the data to deserialize
     * @return the deserialized data
     * @since 3.5.4
     * @deprecated Use {@link #deserializeData(Identifier, String, Version)} instead
     */
    @NotNull
    @Deprecated(since = "3.6.5")
    default Data deserializeData(@NotNull Identifier identifier, @NotNull String data) {
        return deserializeData(identifier, data, getPlugin().getMinecraftVersion());
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

    // Returns if a data type is available and enabled in the config
    private boolean isDataTypeAvailable(@NotNull String key) {
        return getIdentifier(key).map(Identifier::isEnabled).orElse(false);
    }

    @NotNull
    HuskSync getPlugin();

}
