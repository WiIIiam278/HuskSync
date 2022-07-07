package net.william278.husksync.event;

import net.william278.husksync.data.DataSaveCause;
import net.william278.husksync.data.UserData;
import net.william278.husksync.player.User;
import org.jetbrains.annotations.NotNull;

public interface DataSaveEvent extends CancellableEvent {

    @NotNull
    UserData getUserData();

    void setUserData(@NotNull UserData userData);

    @NotNull User getUser();

    @NotNull
    DataSaveCause getSaveCause();

}
