package net.william278.husksync;

import org.jetbrains.annotations.NotNull;

/**
 * Indicates an exception occurred while initialising the HuskSync plugin
 */
public class HuskSyncInitializationException extends RuntimeException {
    public HuskSyncInitializationException(@NotNull String message) {
        super(message);
    }
}
