package net.william278.husksync.util;

import net.william278.husksync.HuskSync;
import net.william278.husksync.adapter.DataAdapter;
import net.william278.husksync.data.DataSnapshot;
import org.apache.commons.lang.NotImplementedException;
import org.jetbrains.annotations.NotNull;

public class BukkitLegacyConverter extends LegacyDataConverter {
    public BukkitLegacyConverter(@NotNull HuskSync plugin) {
        super(plugin);
    }

    @NotNull
    @Override
    public DataSnapshot.Unpacked convert(@NotNull byte[] data) throws DataAdapter.AdaptionException {
        //todo
        throw new NotImplementedException("BukkitLegacyConverter is not implemented yet");
    }
}
