/*
 * This file is part of HuskSync by William278. Do not redistribute!
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  All rights reserved.
 *
 *  This source code is provided as reference to licensed individuals that have purchased the HuskSync
 *  plugin once from any of the official sources it is provided. The availability of this code does
 *  not grant you the rights to modify, re-distribute, compile or redistribute this source code or
 *  "plugin" outside this intended purpose. This license does not cover libraries developed by third
 *  parties that are utilised in the plugin.
 */

package net.william278.husksync.data;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Store's a user's persistent data container, holding a map of plugin-set persistent values
 */
public class PersistentDataContainerData {

    /**
     * Map of namespaced key strings to a byte array representing the persistent data
     */
    @SerializedName("persistent_data_map")
    protected Map<String, PersistentDataTag<?>> persistentDataMap;

    public PersistentDataContainerData(@NotNull Map<String, PersistentDataTag<?>> persistentDataMap) {
        this.persistentDataMap = persistentDataMap;
    }

    @SuppressWarnings("unused")
    protected PersistentDataContainerData() {
    }

    public <T> Optional<T> getTagValue(@NotNull String tagName, @NotNull Class<T> tagClass) {
        if (!persistentDataMap.containsKey(tagName)) {
            return Optional.empty();
        }

        // If the tag cannot be cast to the specified class, return an empty optional
        final boolean canCast = tagClass.isAssignableFrom(persistentDataMap.get(tagName).value.getClass());
        if (!canCast) {
            return Optional.empty();
        }

        return Optional.of(tagClass.cast(persistentDataMap.get(tagName).value));
    }

    public Optional<PersistentDataTagType> getTagType(@NotNull String tagType) {
        if (persistentDataMap.containsKey(tagType)) {
            return PersistentDataTagType.getDataType(persistentDataMap.get(tagType).type);
        }
        return Optional.empty();
    }

    public Set<String> getTags() {
        return persistentDataMap.keySet();
    }

}
