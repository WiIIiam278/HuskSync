package net.william278.husksync.util;

import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * An abstract, cross-platform representation of a logger
 */
public abstract class Logger {

    private boolean debug;

    public abstract void log(@NotNull Level level, @NotNull String message, @NotNull Throwable e);

    public abstract void log(@NotNull Level level, @NotNull String message);

    public abstract void info(@NotNull String message);

    public abstract void severe(@NotNull String message);

    public final void debug(@NotNull String message) {
        if (debug) {
            log(Level.INFO, "[DEBUG] " + message);
        }
    }

    public abstract void config(@NotNull String message);

    public final void showDebugLogs(boolean debug) {
        this.debug = debug;
    }

}
