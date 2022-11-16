package net.william278.husksync.data;

import net.william278.husksync.config.Locales;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.api.BaseHuskSyncAPI;
import net.william278.husksync.player.User;
import org.jetbrains.annotations.NotNull;

/**
 * Identifies the cause of a player data save.
 *
 * @implNote This enum is saved in the database.
 * </p>
 * Cause names have a max length of 32 characters.
 */
public enum DataSaveCause {

    /**
     * Indicates data saved when a player disconnected from the server (either to change servers, or to log off)
     *
     * @since 2.0
     */
    DISCONNECT,
    /**
     * Indicates data saved when the world saved
     *
     * @since 2.0
     */
    WORLD_SAVE,
    /**
     * Indicates data saved when the user died
     *
     * @since 2.1
     */
    DEATH,
    /**
     * Indicates data saved when the server shut down
     *
     * @since 2.0
     */
    SERVER_SHUTDOWN,
    /**
     * Indicates data was saved by editing inventory contents via the {@code /inventory} command
     *
     * @since 2.0
     */
    INVENTORY_COMMAND,
    /**
     * Indicates data was saved by editing Ender Chest contents via the {@code /enderchest} command
     *
     * @since 2.0
     */
    ENDERCHEST_COMMAND,
    /**
     * Indicates data was saved by restoring it from a previous version
     *
     * @since 2.0
     */
    BACKUP_RESTORE,
    /**
     * Indicates data was saved by an API call
     *
     * @see BaseHuskSyncAPI#saveUserData(OnlineUser)
     * @see BaseHuskSyncAPI#setUserData(User, UserData)
     * @since 2.0
     */
    API,
    /**
     * Indicates data was saved from being imported from MySQLPlayerDataBridge
     *
     * @since 2.0
     */
    MPDB_MIGRATION,
    /**
     * Indicates data was saved from being imported from a legacy version (v1.x)
     *
     * @since 2.0
     */
    LEGACY_MIGRATION,
    /**
     * Indicates data was saved by an unknown cause.
     * </p>
     * This should not be used and is only used for error handling purposes.
     *
     * @since 2.0
     */
    UNKNOWN;

    /**
     * Returns a {@link DataSaveCause} by name.
     *
     * @return the {@link DataSaveCause} or {@link #UNKNOWN} if the name is not valid.
     */
    @NotNull
    public static DataSaveCause getCauseByName(@NotNull String name) {
        for (DataSaveCause cause : values()) {
            if (cause.name().equalsIgnoreCase(name)) {
                return cause;
            }
        }
        return UNKNOWN;
    }

    @NotNull
    public String getDisplayName() {
        return Locales.truncate(name().toLowerCase(), 10);
    }

}
