package net.william278.husksync.util;

import java.util.logging.Level;

public class BukkitLogger implements Logger {

    private final java.util.logging.Logger logger;

    public BukkitLogger(java.util.logging.Logger logger) {
        this.logger = logger;
    }

    @Override
    public void log(Level level, String message, Exception e) {
        logger.log(level, message, e);
    }

    @Override
    public void log(Level level, String message) {
        logger.log(level, message);
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void severe(String message) {
        logger.severe(message);
    }

    @Override
    public void config(String message) {
        logger.config(message);
    }

}
