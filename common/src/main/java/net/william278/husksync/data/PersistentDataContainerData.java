package net.william278.husksync.data;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

/**
 * Store's a user's persistent data container, holding a map of plugin-set persistent values
 */
public class PersistentDataContainerData {

    /**
     * A base64 string of platform-serialized PersistentDataContainer data
     */
    @SerializedName("serialized_persistent_data_container")
    public String serializedPersistentDataContainer;

    public PersistentDataContainerData(@NotNull final String serializedPersistentDataContainer) {
        this.serializedPersistentDataContainer = serializedPersistentDataContainer;
    }

    public PersistentDataContainerData() {
    }

}
