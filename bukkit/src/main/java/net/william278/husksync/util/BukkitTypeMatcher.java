package net.william278.husksync.util;

import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public final class BukkitTypeMatcher {

    @Nullable
    public static Statistic matchStatistic(@NotNull String key) {
        return Arrays.stream(Statistic.values())
                .filter(stat -> stat.getKey().toString().equals(key))
                .findFirst().orElse(null);
    }

    @Nullable
    public static EntityType matchEntityType(@NotNull String key) {
        return Arrays.stream(EntityType.values())
                .filter(entityType -> entityType.getKey().toString().equals(key))
                .findFirst().orElse(null);
    }

    @Nullable
    public static Material matchMaterial(@NotNull String key) {
        return Material.matchMaterial(key);
    }

}
