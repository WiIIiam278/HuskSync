package net.william278.husksync.config;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class DummySettings extends Settings {
    private DummySettings(@NotNull Map<ConfigOption, Object> settings) {
        super(settings);
    }

    public static DummySettings get() {
        return new DummySettings(Map.of(
                ConfigOption.SYNCHRONIZATION_SAVE_DEAD_PLAYER_INVENTORIES, true
        ));
    }
}
