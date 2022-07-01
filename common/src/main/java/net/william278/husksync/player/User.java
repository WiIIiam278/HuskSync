package net.william278.husksync.player;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class User {

    @SerializedName("username")
    public String username;

    @SerializedName("uuid")
    public UUID uuid;

    public User(@NotNull UUID uuid, @NotNull String username) {
        this.username = username;
        this.uuid = uuid;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof User other) {
            return this.uuid.equals(other.uuid);
        }
        return super.equals(obj);
    }
}
