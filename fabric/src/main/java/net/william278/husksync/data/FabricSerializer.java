package net.william278.husksync.data;

import net.william278.husksync.HuskSync;
import net.william278.husksync.api.HuskSyncAPI;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

//TODO
public abstract class FabricSerializer {

    protected final HuskSync plugin;

    private FabricSerializer(@NotNull HuskSync plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("unused")
    public FabricSerializer(@NotNull HuskSyncAPI api) {
        this.plugin = api.getPlugin();
    }

    @ApiStatus.Internal
    @NotNull
    public HuskSync getPlugin() {
        return plugin;
    }

}
