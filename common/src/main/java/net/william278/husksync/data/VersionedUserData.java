package net.william278.husksync.data;

import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.UUID;

/**
 * Represents a uniquely versioned and timestamped snapshot of a user's data
 *
 * @param versionUUID      The unique identifier for this user data version
 * @param versionTimestamp An epoch milliseconds timestamp of when this data was created
 * @param userData         The {@link UserData} that has been versioned
 */
public record VersionedUserData(@NotNull UUID versionUUID, @NotNull Date versionTimestamp,
                                @NotNull UserData userData) implements Comparable<VersionedUserData> {

    public VersionedUserData(@NotNull final UUID versionUUID, @NotNull final Date versionTimestamp,
                             @NotNull UserData userData) {
        this.versionUUID = versionUUID;
        this.versionTimestamp = versionTimestamp;
        this.userData = userData;
    }

    public static VersionedUserData version(@NotNull UserData userData) {
        return new VersionedUserData(UUID.randomUUID(), new Date(), userData);
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
