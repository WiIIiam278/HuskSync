package net.william278.husksync.data;

import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.UUID;

/**
 * Represents a uniquely versioned and timestamped snapshot of a user's data, including why it was saved.
 *
 * @param versionUUID      The unique identifier for this user data version
 * @param versionTimestamp An epoch milliseconds timestamp of when this data was created
 * @param userData         The {@link UserData} that has been versioned
 * @param cause            The {@link DataSaveCause} that caused this data to be saved
 */
public record UserDataSnapshot(@NotNull UUID versionUUID, @NotNull Date versionTimestamp,
                               @NotNull DataSaveCause cause, boolean pinned,
                               @NotNull UserData userData) implements Comparable<UserDataSnapshot> {

    /**
     * Version {@link UserData} into a {@link UserDataSnapshot}, assigning it a random {@link UUID} and the current timestamp {@link Date}
     * </p>
     * Note that this method will set {@code cause} to {@link DataSaveCause#API}
     *
     * @param userData The {@link UserData} to version
     * @return A new {@link UserDataSnapshot}
     * @implNote This isn't used to version data that is going to be set to a database to prevent UUID collisions.<p>
     * Database implementations should instead use their own UUID generation functions.
     */
    public static UserDataSnapshot create(@NotNull UserData userData) {
        return new UserDataSnapshot(UUID.randomUUID(), new Date(),
                DataSaveCause.API, false, userData);
    }

    /**
     * Compare UserData by creation timestamp
     *
     * @param other the other UserData to be compared
     * @return the comparison result; the more recent UserData is greater than the less recent UserData
     */
    @Override
    public int compareTo(@NotNull UserDataSnapshot other) {
        return Long.compare(this.versionTimestamp.getTime(), other.versionTimestamp.getTime());
    }

}
