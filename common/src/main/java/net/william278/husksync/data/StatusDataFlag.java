package net.william278.husksync.data;

import net.william278.husksync.config.Settings;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * Flags for setting {@link StatusData}, indicating which elements should be synced
 */
public enum StatusDataFlag {

    SET_HEALTH(Settings.ConfigOption.SYNCHRONIZATION_SYNC_HEALTH),
    SET_MAX_HEALTH(Settings.ConfigOption.SYNCHRONIZATION_SYNC_MAX_HEALTH),
    SET_HUNGER(Settings.ConfigOption.SYNCHRONIZATION_SYNC_HUNGER),
    SET_EXPERIENCE(Settings.ConfigOption.SYNCHRONIZATION_SYNC_EXPERIENCE),
    SET_GAME_MODE(Settings.ConfigOption.SYNCHRONIZATION_SYNC_GAME_MODE),
    SET_FLYING(Settings.ConfigOption.SYNCHRONIZATION_SYNC_LOCATION),
    SET_SELECTED_ITEM_SLOT(Settings.ConfigOption.SYNCHRONIZATION_SYNC_INVENTORIES);

    private final Settings.ConfigOption configOption;

    StatusDataFlag(@NotNull Settings.ConfigOption configOption) {
        this.configOption = configOption;
    }

    /**
     * Returns all status data flags
     *
     * @return all status data flags as a list
     */
    @NotNull
    @SuppressWarnings("unused")
    public static List<StatusDataFlag> getAll() {
        return Arrays.stream(StatusDataFlag.values()).toList();
    }

    /**
     * Returns all status data flags that are enabled for setting as per the {@link Settings}
     *
     * @param settings the settings to use for determining which flags are enabled
     * @return all status data flags that are enabled for setting
     */
    @NotNull
    public static List<StatusDataFlag> getFromSettings(@NotNull Settings settings) {
        return Arrays.stream(StatusDataFlag.values()).filter(
                flag -> settings.getBooleanValue(flag.configOption)).toList();
    }

}
