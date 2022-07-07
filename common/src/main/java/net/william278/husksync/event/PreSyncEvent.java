package net.william278.husksync.event;

import net.william278.husksync.data.UserData;
import org.jetbrains.annotations.NotNull;

public interface PreSyncEvent extends CancellableEvent {

    @NotNull
    UserData getUserData();

    void setUserData(@NotNull UserData userData);

}
