package net.william278.husksync.data;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;

/**
 * Represents enabled synchronisation features
 */
public enum SynchronizationFeature {
    INVENTORIES(true),
    ENDER_CHESTS(true),
    HEALTH(true),
    MAX_HEALTH(true),
    HUNGER(true),
    EXPERIENCE(true),
    POTION_EFFECTS(true),
    ADVANCEMENTS(true),
    GAME_MODE(true),
    STATISTICS(true),
    PERSISTENT_DATA_CONTAINER(false),
    LOCKED_MAPS(false),
    LOCATION(false);

    private final boolean enabledByDefault;

    SynchronizationFeature(boolean enabledByDefault) {
        this.enabledByDefault = enabledByDefault;
    }

    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    @NotNull
    private Map.Entry<String, Boolean> toEntry() {
        return Map.entry(name().toLowerCase(), enabledByDefault);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static Map<String, Boolean> getDefaults() {
        return Map.ofEntries(Arrays.stream(values())
                .map(SynchronizationFeature::toEntry)
                .toArray(Map.Entry[]::new));
    }

}
