package me.william278.husksync.util;

import java.util.logging.Level;

/**
 * Logger interface to allow for implementation of different logger platforms used by Bungee and Velocity
 */
public interface Logger {

    void log(Level level, String message, Exception e);

    void log(Level level, String message);

    void info(String message);

    void severe(String message);

    void config(String message);
}