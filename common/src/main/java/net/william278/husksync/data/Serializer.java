package net.william278.husksync.data;

import org.jetbrains.annotations.NotNull;

public interface Serializer<T extends DataContainer> {

    T deserialize(byte[] serialized) throws DeserializationException;

    @NotNull
    byte[] serialize(@NotNull T element) throws SerializationException;

    static final class DeserializationException extends IllegalStateException {
        DeserializationException(@NotNull String message, @NotNull Throwable cause) {
            super(message, cause);
        }
    }

    static final class SerializationException extends IllegalStateException {
        SerializationException(@NotNull String message, @NotNull Throwable cause) {
            super(message, cause);
        }
    }


}
