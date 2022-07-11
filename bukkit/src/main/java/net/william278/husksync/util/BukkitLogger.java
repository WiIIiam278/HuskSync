package net.william278.husksync.util;

import de.themoep.minedown.MineDown;
import net.md_5.bungee.api.chat.TextComponent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public class BukkitLogger extends Logger {

    private final java.util.logging.Logger logger;

    public BukkitLogger(@NotNull java.util.logging.Logger logger) {
        this.logger = logger;
    }

    @Override
    public void log(@NotNull Level level, @NotNull String message, @NotNull Exception e) {
        logger.log(level, message, e);
    }

    @Override
    public void log(@NotNull Level level, @NotNull String message) {
        logger.log(level, message);
    }

    @Override
    public void log(@NotNull Level level, @NotNull MineDown mineDown) {
        logger.log(level, TextComponent.toLegacyText(mineDown.toComponent()));
    }

    @Override
    public void info(@NotNull String message) {
        logger.info(message);
    }

    @Override
    public void severe(@NotNull String message) {
        logger.severe(message);
    }

    @Override
    public void config(@NotNull String message) {
        logger.config(message);
    }

}
