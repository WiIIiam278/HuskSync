package net.william278.husksync.util;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;

/**
 * Abstract representation of a reader that reads internal resource files by name
 */
public interface ResourceReader {

    /**
     * Gets the resource with given filename and reads it as an {@link InputStream}
     *
     * @param fileName Name of the resource file to read
     * @return The resource, read as an {@link InputStream}
     */
    @NotNull InputStream getResource(String fileName);

    /**
     * Gets the plugin data folder where plugin configuration and data are kept
     *
     * @return the plugin data directory
     */
    @NotNull File getDataFolder();

}
