package net.william278.husksync.logger;

import net.william278.husksync.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public class DummyLogger extends Logger {

    public DummyLogger() {
    }

    @Override
    public void log(@NotNull Level level, @NotNull String message, @NotNull Throwable e) {
        System.out.println(level.getName() + ": " + message);
        e.printStackTrace();
    }

    @Override
    public void log(@NotNull Level level, @NotNull String message) {
        System.out.println(level.getName() + ": " + message);
    }

    @Override
    public void info(@NotNull String message) {
        System.out.println(Level.INFO.getName() + ": " + message);
    }

    @Override
    public void severe(@NotNull String message) {
        System.out.println(Level.SEVERE.getName() + ": " + message);
    }

    @Override
    public void config(@NotNull String message) {
        System.out.println(Level.CONFIG.getName() + ": " + message);
    }
}
