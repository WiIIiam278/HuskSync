package net.william278.husksync.command;

import org.jetbrains.annotations.NotNull;

/**
 * Interface providing console execution of commands
 */
public interface ConsoleExecutable {

    /**
     * What to do when console executes a command
     *
     * @param args command argument strings
     */
    void onConsoleExecute(@NotNull String[] args);

}
