package net.william278.husksync.bungeecord.util;

import net.william278.husksync.util.Logger;

import java.util.logging.Level;

public record BungeeLogger(java.util.logging.Logger parent) implements Logger {

    @Override
    public void log(Level level, String message, Exception e) {
        parent.log(level, message, e);
    }

    @Override
    public void log(Level level, String message) {
        parent.log(level, message);
    }

    @Override
    public void info(String message) {
        parent.info(message);
    }

    @Override
    public void severe(String message) {
        parent.severe(message);
    }

    @Override
    public void config(String message) {
        parent.config(message);
    }
}
