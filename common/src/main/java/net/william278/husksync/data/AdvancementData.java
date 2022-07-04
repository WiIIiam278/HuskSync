package net.william278.husksync.data;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Map;

/**
 * A mapped piece of advancement data
 */
public class AdvancementData {

    /**
     * The advancement namespaced key
     */
    @SerializedName("key")
    public String key;

    /**
     * A map of completed advancement criteria to when it was completed
     */
    @SerializedName("completed_criteria")
    public Map<String, Date> completedCriteria;

    public AdvancementData(@NotNull String key, @NotNull Map<String, Date> awardedCriteria) {
        this.key = key;
        this.completedCriteria = awardedCriteria;
    }

    @SuppressWarnings("unused")
    protected AdvancementData() {
    }
}
