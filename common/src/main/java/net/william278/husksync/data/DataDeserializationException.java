package net.william278.husksync.data;

/**
 * Indicates an error occurred during base-64 serialization and deserialization of data.
 * </p>
 * For example, an exception deserializing {@link ItemData} item stack or {@link PotionEffectData} potion effect arrays
 */
public class DataDeserializationException extends RuntimeException {
    protected DataDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }

}
