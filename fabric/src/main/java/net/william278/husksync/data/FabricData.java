package net.william278.husksync.data;

import net.william278.husksync.FabricHuskSync;
import net.william278.husksync.HuskSync;
import net.william278.husksync.user.FabricUser;
import org.jetbrains.annotations.NotNull;

//TODO
public abstract class FabricData implements Data {
    @Override
    public void apply(@NotNull UserDataHolder user, @NotNull HuskSync plugin) {
        this.apply((FabricUser) user, (FabricHuskSync) plugin);
    }

    protected abstract void apply(@NotNull FabricUser user, @NotNull FabricHuskSync plugin);

}
