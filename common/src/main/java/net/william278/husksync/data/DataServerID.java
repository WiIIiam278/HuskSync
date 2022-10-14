package net.william278.husksync.data;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

public class DataServerID {

    @SerializedName("server_id")
    @NotNull
    private static String serverID;


    /**
     * Set the {@link DataServerID}
     *
     * @param server the {@link DataServerID} to set
     * @return this {@link DataServerID#serverID}
     * @since 2.2
     */
    @NotNull
    public static String setServerID(@NotNull String server) {
        serverID = server;
        return serverID;
    }

    /**
     * Returns the {@link DataServerID#serverID}
     *
     * @return the {@link DataServerID#serverID}
     * @since 2.2
     */
    @NotNull
    public static String getServerID() {
        return serverID;
    }

}
