package me.william278.husksync.bukkit.util.nms;

import me.william278.husksync.util.ThrowSupplier;
import org.bukkit.entity.LivingEntity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class EntityUtils {

    private final static Method GET_HANDLE;

    static {
        final Class<?> CRAFT_ENTITY = MinecraftVersionUtils.getBukkitClass("entity.CraftEntity");
        GET_HANDLE = ThrowSupplier.get(() -> CRAFT_ENTITY.getDeclaredMethod("getHandle"));
    }

    public static Object getHandle (LivingEntity livingEntity) throws RuntimeException {
        try {
            return GET_HANDLE.invoke(livingEntity);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}
