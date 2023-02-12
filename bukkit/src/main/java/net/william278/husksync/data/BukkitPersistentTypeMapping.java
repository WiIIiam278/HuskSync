package net.william278.husksync.data;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public record BukkitPersistentTypeMapping<T, Z>(PersistentDataTagType type, PersistentDataType<T, Z> bukkitType) {

    public static final BukkitPersistentTypeMapping<?, ?>[] PRIMITIVE_TYPE_MAPPINGS = new BukkitPersistentTypeMapping<?, ?>[]{
            new BukkitPersistentTypeMapping<>(PersistentDataTagType.BYTE, PersistentDataType.BYTE),
            new BukkitPersistentTypeMapping<>(PersistentDataTagType.SHORT, PersistentDataType.SHORT),
            new BukkitPersistentTypeMapping<>(PersistentDataTagType.INTEGER, PersistentDataType.INTEGER),
            new BukkitPersistentTypeMapping<>(PersistentDataTagType.LONG, PersistentDataType.LONG),
            new BukkitPersistentTypeMapping<>(PersistentDataTagType.FLOAT, PersistentDataType.FLOAT),
            new BukkitPersistentTypeMapping<>(PersistentDataTagType.DOUBLE, PersistentDataType.DOUBLE),
            new BukkitPersistentTypeMapping<>(PersistentDataTagType.STRING, PersistentDataType.STRING),
            new BukkitPersistentTypeMapping<>(PersistentDataTagType.BYTE_ARRAY, PersistentDataType.BYTE_ARRAY),
            new BukkitPersistentTypeMapping<>(PersistentDataTagType.INTEGER_ARRAY, PersistentDataType.INTEGER_ARRAY),
            new BukkitPersistentTypeMapping<>(PersistentDataTagType.LONG_ARRAY, PersistentDataType.LONG_ARRAY),
            new BukkitPersistentTypeMapping<>(PersistentDataTagType.TAG_CONTAINER_ARRAY, PersistentDataType.TAG_CONTAINER_ARRAY),
            new BukkitPersistentTypeMapping<>(PersistentDataTagType.TAG_CONTAINER, PersistentDataType.TAG_CONTAINER)
    };

    public BukkitPersistentTypeMapping(@NotNull PersistentDataTagType type, @NotNull PersistentDataType<T, Z> bukkitType) {
        this.type = type;
        this.bukkitType = bukkitType;
    }

    @NotNull
    public PersistentDataTag<Z> getContainerValue(@NotNull PersistentDataContainer container, @NotNull NamespacedKey key) throws NullPointerException {
        return new PersistentDataTag<>(type, Objects.requireNonNull(container.get(key, bukkitType)));
    }

    public void setContainerValue(@NotNull PersistentDataContainerData container, @NotNull Player player, @NotNull NamespacedKey key) throws NullPointerException {
        container.getTagValue(key.toString(), bukkitType.getPrimitiveType())
                .ifPresent(value -> player.getPersistentDataContainer().set(key, bukkitType, (Z) value));
    }

    public static Optional<BukkitPersistentTypeMapping<?, ?>> getMapping(@NotNull PersistentDataTagType type) {
        for (BukkitPersistentTypeMapping<?, ?> mapping : PRIMITIVE_TYPE_MAPPINGS) {
            if (mapping.type().equals(type)) {
                return Optional.of(mapping);
            }
        }
        return Optional.empty();
    }


}
