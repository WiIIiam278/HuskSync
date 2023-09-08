package net.william278.husksync.util;

import net.william278.husksync.HuskSync;
import net.william278.husksync.adapter.DataAdapter;
import net.william278.husksync.data.DataSnapshot;
import org.jetbrains.annotations.NotNull;

public abstract class LegacyDataConverter {

    protected final HuskSync plugin;

    protected LegacyDataConverter(@NotNull HuskSync plugin) {
        this.plugin = plugin;
    }

    @NotNull
    public abstract DataSnapshot.Unpacked convert(@NotNull byte[] data) throws DataAdapter.AdaptionException;

}
