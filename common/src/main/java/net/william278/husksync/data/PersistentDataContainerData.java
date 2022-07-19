package net.william278.husksync.data;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Store's a user's persistent data container, holding a map of plugin-set persistent values
 */
public class PersistentDataContainerData {

    /**
     * Map of namespaced key strings to a byte array representing the persistent data
     */
    @SerializedName("persistent_data_map")
    public Map<String, PersistentDataTag> persistentDataMap;

    public PersistentDataContainerData(@NotNull final Map<String, PersistentDataTag> persistentDataMap) {
        this.persistentDataMap = persistentDataMap;
    }

    @SuppressWarnings("unused")
    protected PersistentDataContainerData() {
    }

}
