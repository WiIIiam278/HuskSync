package me.william278.husksync.bukkit.util.nms;

import me.william278.husksync.util.ThrowSupplier;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Map;

public class AdvancementUtils {

    private final static Field PLAYER_ADVANCEMENTS_MAP;
    private final static Field PLAYER_ADVANCEMENTS;
    private final static Field CRITERIA_MAP;
    private final static Field CRITERIA_DATE;
    private final static Field IS_FIRST_PACKET;

    private final static Method GET_HANDLE;
    private final static Method START_PROGRESS;
    private final static Method ENSURE_ALL_VISIBLE;

    private final static Class<?> ADVANCEMENT_PROGRESS;
    private final static Class<?> CRITERION_PROGRESS;

    static {
        Class<?> SERVER_PLAYER = MinecraftVersionUtils.getMinecraftClass("level.EntityPlayer");
        PLAYER_ADVANCEMENTS = ThrowSupplier.get(() -> SERVER_PLAYER.getDeclaredField("cs"));
        PLAYER_ADVANCEMENTS.setAccessible(true);

        Class<?> CRAFT_ADVANCEMENT = MinecraftVersionUtils.getBukkitClass("advancement.CraftAdvancement");
        GET_HANDLE = ThrowSupplier.get(() -> CRAFT_ADVANCEMENT.getDeclaredMethod("getHandle"));

        ADVANCEMENT_PROGRESS = ThrowSupplier.get(() -> Class.forName("net.minecraft.advancements.AdvancementProgress"));
        CRITERIA_MAP = ThrowSupplier.get(() -> ADVANCEMENT_PROGRESS.getDeclaredField("a"));
        CRITERIA_MAP.setAccessible(true);

        CRITERION_PROGRESS = ThrowSupplier.get(() -> Class.forName("net.minecraft.advancements.CriterionProgress"));
        CRITERIA_DATE = ThrowSupplier.get(() -> CRITERION_PROGRESS.getDeclaredField("b"));
        CRITERIA_DATE.setAccessible(true);

        Class<?> ADVANCEMENT = ThrowSupplier.get(() -> Class.forName("net.minecraft.advancements.Advancement"));

        Class<?> PLAYER_ADVANCEMENTS = MinecraftVersionUtils.getMinecraftClass("AdvancementDataPlayer");
        PLAYER_ADVANCEMENTS_MAP = ThrowSupplier.get(() -> PLAYER_ADVANCEMENTS.getDeclaredField("h"));
        PLAYER_ADVANCEMENTS_MAP.setAccessible(true);

        START_PROGRESS = ThrowSupplier.get(() -> PLAYER_ADVANCEMENTS.getDeclaredMethod("a", ADVANCEMENT, ADVANCEMENT_PROGRESS));
        START_PROGRESS.setAccessible(true);

        ENSURE_ALL_VISIBLE = ThrowSupplier.get(() -> PLAYER_ADVANCEMENTS.getDeclaredMethod("c"));
        ENSURE_ALL_VISIBLE.setAccessible(true);

        IS_FIRST_PACKET = ThrowSupplier.get(() -> PLAYER_ADVANCEMENTS.getDeclaredField("n"));
        IS_FIRST_PACKET.setAccessible(true);
    }

    public static void markPlayerAdvancementsFirst(Object playerAdvancements) {
        try {
            IS_FIRST_PACKET.set(playerAdvancements, true);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static Object getPlayerAdvancements (Player player) {
        Object nativePlayer = EntityUtils.getHandle(player);
        try {
            return PLAYER_ADVANCEMENTS.get(nativePlayer);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static void clearPlayerAdvancementsMap (final Object playerAdvancement) {
        try {
            ((Map<?,?>) PLAYER_ADVANCEMENTS_MAP.get(playerAdvancement))
                    .clear();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static Object getHandle (Advancement advancement) {
        try {
            return GET_HANDLE.invoke(advancement);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static Object newCriterionProgress (final Date date) {
        try {
            Object nativeCriterionProgress = CRITERION_PROGRESS.getDeclaredConstructor().newInstance();
            CRITERIA_DATE.set(nativeCriterionProgress, date);
            return nativeCriterionProgress;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static Object newAdvancementProgress (final Map<String, Object> criteria) {
        try {
            Object nativeAdvancementProgress = ADVANCEMENT_PROGRESS.getDeclaredConstructor().newInstance();

            final Map<String, Object> criteriaMap = (Map<String, Object>) CRITERIA_MAP.get(nativeAdvancementProgress);
            criteriaMap.putAll(criteria);

            return nativeAdvancementProgress;
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static void startProgress (final Object playerAdvancements, final Object advancement, final Object advancementProgress) {
        try {
            START_PROGRESS.invoke(playerAdvancements, advancement, advancementProgress);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static void ensureAllVisible (final Object playerAdvancements) {
        try {
            ENSURE_ALL_VISIBLE.invoke(playerAdvancements);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}
