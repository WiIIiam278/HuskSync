package net.william278.husksync.command;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Interface providing tab completions for a command
 */
public interface TabCompletable {

    /**
     * What should be returned when the player or console attempts to TAB-complete a command
     *
     * @param args Current command argumentsrf
     * @return List of String arguments to offer TAB suggestions
     */
    List<String> onTabComplete(@NotNull String[] args);

}
