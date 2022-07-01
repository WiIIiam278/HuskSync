package net.william278.husksync.util;

import java.util.logging.Level;

/**
 * An abstract, cross-platform representation of a logger
 */
public interface Logger {

    void log(Level level, String message, Exception e);

    void log(Level level, String message);

    void info(String message);

    void severe(String message);

    void config(String message);

}
