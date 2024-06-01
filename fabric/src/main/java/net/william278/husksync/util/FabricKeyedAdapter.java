package net.william278.husksync.util;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// Utility class for adapting "Keyed" Minecraft objects
public final class FabricKeyedAdapter {

    @Nullable
    public static EntityType<?> matchEntityType(@NotNull String key) {
        return getRegistryValue(Registries.ENTITY_TYPE, key);
    }

    @Nullable
    public static String getEntityTypeId(@NotNull EntityType<?> entityType) {
        return getRegistryKey(Registries.ENTITY_TYPE, entityType);
    }

    @Nullable
    public static EntityAttribute matchAttribute(@NotNull String key) {
        return getRegistryValue(Registries.ATTRIBUTE, key);
    }

    @Nullable
    public static String getAttributeId(@NotNull EntityAttribute attribute) {
        return getRegistryKey(Registries.ATTRIBUTE, attribute);
    }

    @Nullable
    public static StatusEffect matchEffectType(@NotNull String key) {
        return getRegistryValue(Registries.STATUS_EFFECT, key);
    }

    @Nullable
    public static String getEffectId(@NotNull StatusEffect effect) {
        return getRegistryKey(Registries.STATUS_EFFECT, effect);
    }

    @Nullable
    private static <T> T getRegistryValue(@NotNull Registry<T> registry, @NotNull String keyString) {
        final Identifier key = Identifier.tryParse(keyString);
        return key != null ? registry.get(key) : null;
    }

    @Nullable
    private static <T> String getRegistryKey(@NotNull Registry<T> registry, @NotNull T value) {
        final Identifier key = registry.getId(value);
        return key != null ? key.toString() : null;
    }

}
