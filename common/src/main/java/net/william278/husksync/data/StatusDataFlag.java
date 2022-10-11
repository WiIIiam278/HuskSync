package net.william278.husksync.data;

import net.william278.husksync.config.Settings;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * Flags for setting {@link StatusData}, indicating which elements should be synced
 *
 * @deprecated Use the more direct {@link Settings#getSynchronizationFeature(Settings.SynchronizationFeature)} instead
 */
@Deprecated(since = "2.1")
public enum StatusDataFlag {

    SET_HEALTH(Settings.SynchronizationFeature.HEALTH),
    SET_MAX_HEALTH(Settings.SynchronizationFeature.MAX_HEALTH),
    SET_HUNGER(Settings.SynchronizationFeature.HUNGER),
    SET_EXPERIENCE(Settings.SynchronizationFeature.EXPERIENCE),
    SET_GAME_MODE(Settings.SynchronizationFeature.GAME_MODE),
    SET_FLYING(Settings.SynchronizationFeature.LOCATION),
    SET_SELECTED_ITEM_SLOT(Settings.SynchronizationFeature.INVENTORIES);

    private final Settings.SynchronizationFeature feature;

    StatusDataFlag(@NotNull Settings.SynchronizationFeature feature) {
        this.feature = feature;
    }

    /**
     * Returns all status data flags
     *
     * @return all status data flags as a list
     * @deprecated Use {@link Settings#getSynchronizationFeature(Settings.SynchronizationFeature)} instead
     */
    @NotNull
    @Deprecated(since = "2.1")
    @SuppressWarnings("unused")
    public static List<StatusDataFlag> getAll() {
        return Arrays.stream(StatusDataFlag.values()).toList();
    }

    /**
     * Returns all status data flags that are enabled for setting as per the {@link Settings}
     *
     * @param settings the settings to use for determining which flags are enabled
     * @return all status data flags that are enabled for setting
     * @deprecated Use {@link Settings#getSynchronizationFeature(Settings.SynchronizationFeature)} instead
     */
    @NotNull
    @Deprecated(since = "2.1")
    public static List<StatusDataFlag> getFromSettings(@NotNull Settings settings) {
        return Arrays.stream(StatusDataFlag.values()).filter(
                flag -> settings.getSynchronizationFeature(flag.feature)).toList();
    }

}
