package net.william278.husksync.util;

import org.jetbrains.annotations.Nullable;

import java.io.InputStream;

/**
 * Abstract representation of a reader that reads internal resource files by name
 */
public interface ResourceReader {

    /**
     * Gets the resource with given filename and reads it as an {@link InputStream}
     *
     * @param fileName Name of the resource file to read
     * @return The resource, read as an {@link InputStream}; or {@code null} if the resource was not found
     */
    @Nullable InputStream getResource(String fileName);

}
