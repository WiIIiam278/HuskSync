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
public record VersionedUserData(@NotNull UUID versionUUID, @NotNull Date versionTimestamp,
                                @NotNull DataSaveCause cause, @NotNull UserData userData) implements Comparable<VersionedUserData> {

    /**
     * Version {@link UserData} into a {@link VersionedUserData}, assigning it a random {@link UUID} and the current timestamp {@link Date}
     * </p>
     * Note that this method will set {@code cause} to {@link DataSaveCause#API}
     *
     * @param userData The {@link UserData} to version
     * @return A new {@link VersionedUserData}
     * @implNote This isn't used to version data that is going to be set to a database to prevent UUID collisions.<p>
     * Database implementations should instead use their own UUID generation functions.
     */
    public static VersionedUserData version(@NotNull UserData userData) {
        return new VersionedUserData(UUID.randomUUID(), new Date(), DataSaveCause.API, userData);
    }

    /**
     * Compare UserData by creation timestamp
     *
     * @param other the other UserData to be compared
     * @return the comparison result; the more recent UserData is greater than the less recent UserData
     */
    @Override
    public int compareTo(@NotNull VersionedUserData other) {
        return Long.compare(this.versionTimestamp.getTime(), other.versionTimestamp.getTime());
    }

}
