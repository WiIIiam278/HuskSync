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
