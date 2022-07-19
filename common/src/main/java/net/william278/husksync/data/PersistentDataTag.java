package net.william278.husksync.data;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents a persistent data tag set by a plugin.
 */
public class PersistentDataTag {

    /**
     * The enumerated primitive data type name value of the tag
     */
    public String type;

    /**
     * The value of the tag
     */
    public Object value;

    public PersistentDataTag(@NotNull String type, @NotNull Object value) {
        this.type = type;
        this.value = value;
    }

    private PersistentDataTag() {
    }

}
