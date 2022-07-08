package net.william278.husksync.data;

import org.jetbrains.annotations.NotNull;

/**
 * Indicates an error occurred during Base-64 serialization and deserialization of data.
 * </p>
 * For example, an exception deserializing {@link ItemData} item stack or {@link PotionEffectData} potion effect arrays
 */
public class DataSerializationException extends RuntimeException {
    protected DataSerializationException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
    }

}
