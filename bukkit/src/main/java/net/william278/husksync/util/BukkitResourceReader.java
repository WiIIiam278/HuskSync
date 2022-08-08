package net.william278.husksync.util;

import net.william278.husksync.BukkitHuskSync;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.Objects;

public class BukkitResourceReader implements ResourceReader {

    private final BukkitHuskSync plugin;

    public BukkitResourceReader(BukkitHuskSync plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull InputStream getResource(String fileName) {
        return Objects.requireNonNull(plugin.getResource(fileName));
    }

}
