package net.william278.husksync.data;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Represents the type of persistent data tag, implemented by a Bukkit PersistentDataType.
 */
public enum BukkitPersistentDataTagType {

    BYTE(PersistentDataType.BYTE),
    SHORT(PersistentDataType.SHORT),
    INTEGER(PersistentDataType.INTEGER),
    LONG(PersistentDataType.LONG),
    FLOAT(PersistentDataType.FLOAT),
    DOUBLE(PersistentDataType.DOUBLE),
    STRING(PersistentDataType.STRING),
    BYTE_ARRAY(PersistentDataType.BYTE_ARRAY),
    INTEGER_ARRAY(PersistentDataType.INTEGER_ARRAY),
    LONG_ARRAY(PersistentDataType.LONG_ARRAY),
    TAG_CONTAINER_ARRAY(PersistentDataType.TAG_CONTAINER_ARRAY),
    TAG_CONTAINER(PersistentDataType.TAG_CONTAINER);

    public final PersistentDataType<?, ?> dataType;

    BukkitPersistentDataTagType(PersistentDataType<?, ?> persistentDataType) {
        this.dataType = persistentDataType;
    }

    public static Optional<BukkitPersistentDataTagType> getDataType(@NotNull String typeName) {
        for (BukkitPersistentDataTagType type : values()) {
            if (type.name().equalsIgnoreCase(typeName)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    /**
     * Determine the {@link BukkitPersistentDataTagType} of a tag in a {@link PersistentDataContainer}.
     *
     * @param container The {@link PersistentDataContainer} to check.
     * @param key       The {@link NamespacedKey} of the tag to check.
     * @return The {@link BukkitPersistentDataTagType} of the key, or {@link Optional#empty()} if the key does not exist.
     */
    public static Optional<BukkitPersistentDataTagType> getKeyDataType(@NotNull PersistentDataContainer container,
                                                                       @NotNull NamespacedKey key) {
        for (BukkitPersistentDataTagType dataType : values()) {
            if (container.has(key, dataType.dataType)) {
                return Optional.of(dataType);
            }
        }
        return Optional.empty();
    }

}
