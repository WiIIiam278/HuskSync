package net.william278.husksync.data;

import org.jetbrains.annotations.NotNull;

/**
 * Identifies the cause of a player data save.
 *
 * @implNote This enum is saved in the database. Cause names have a max length of 32 characters.
 */
public enum DataSaveCause {

    /**
     * Indicates data saved when a player disconnected from the server (either to change servers, or to log off)
     */
    DISCONNECT,
    /**
     * Indicates data saved when the world saved
     */
    WORLD_SAVE,
    /**
     * Indicates data saved when the server shut down
     */
    SERVER_SHUTDOWN,
    /**
     * Indicates data was saved by editing inventory contents via the {@code /invsee} command
     */
    INVSEE_COMMAND_EDIT,
    /**
     * Indicates data was saved by editing Ender Chest contents via the {@code /echest} command
     */
    ECHEST_COMMAND_EDIT,
    /**
     * Indicates data was saved by an API call
     */
    API,
    /**
     * Indicates data was saved by an unknown cause.
     * </p>
     * This should not be used and is only used for error handling purposes.
     */
    UNKNOWN;

    @NotNull
    public static DataSaveCause getCauseByName(@NotNull String name) {
        for (DataSaveCause cause : values()) {
            if (cause.name().equalsIgnoreCase(name)) {
                return cause;
            }
        }
        return UNKNOWN;
    }

}
