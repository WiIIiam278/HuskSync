package net.william278.husksync.data;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

/**
 * Stores potion effect data
 */
public class PotionEffectData {

    @SerializedName("serialized_potion_effects")
    public String serializedPotionEffects;

    public PotionEffectData(@NotNull final String serializedInventory) {
        this.serializedPotionEffects = serializedInventory;
    }

    @SuppressWarnings("unused")
    protected PotionEffectData() {
    }

}
