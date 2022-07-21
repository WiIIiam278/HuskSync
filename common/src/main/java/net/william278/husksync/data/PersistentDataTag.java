package net.william278.husksync.data;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Represents a persistent data tag set by a plugin.
 */
public class PersistentDataTag<T> {

    /**
     * The enumerated primitive data type name value of the tag
     */
    protected String type;

    /**
     * The value of the tag
     */
    public T value;

    public PersistentDataTag(@NotNull BukkitPersistentDataTagType type, @NotNull T value) {
        this.type = type.name();
        this.value = value;
    }

    private PersistentDataTag() {
    }

    public Optional<BukkitPersistentDataTagType> getType() {
        return BukkitPersistentDataTagType.getDataType(type);
    }

}
