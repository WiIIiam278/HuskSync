package net.william278.husksync.util;

import net.william278.husksync.HuskSync;
import net.william278.husksync.adapter.DataAdapter;
import net.william278.husksync.data.DataSnapshot;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

public class BukkitLegacyConverter extends LegacyDataConverter {
    public BukkitLegacyConverter(@NotNull HuskSync plugin) {
        super(plugin);
    }

    @NotNull
    @Override
    public DataSnapshot.Unpacked convert(@NotNull byte[] data) throws DataAdapter.AdaptionException {
        final String json = new String(data, StandardCharsets.UTF_8);
        return null;
    }
}
