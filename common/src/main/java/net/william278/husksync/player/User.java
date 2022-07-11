package net.william278.husksync.player;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents a user who has their data synchronised by HuskSync
 */
public class User {

    /**
     * The user's unique account ID
     */
    public UUID uuid;

    /**
     * The user's username
     */
    public String username;

    public User(@NotNull UUID uuid, @NotNull String username) {
        this.username = username;
        this.uuid = uuid;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof User other) {
            return this.uuid.equals(other.uuid);
        }
        return super.equals(object);
    }
}
