package net.william278.husksync.data;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Represents the type of a {@link PersistentDataTag}
 */
public enum PersistentDataTagType {

    BYTE,
    SHORT,
    INTEGER,
    LONG,
    FLOAT,
    DOUBLE,
    STRING,
    BYTE_ARRAY,
    INTEGER_ARRAY,
    LONG_ARRAY,
    TAG_CONTAINER_ARRAY,
    TAG_CONTAINER;


    public static Optional<PersistentDataTagType> getDataType(@NotNull String typeName) {
        for (PersistentDataTagType type : values()) {
            if (type.name().equalsIgnoreCase(typeName)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

}
