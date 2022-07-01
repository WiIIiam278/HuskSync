package net.william278.husksync.command;

import net.william278.husksync.player.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Interface providing tab completions for a command
 */
public interface TabCompletable {

    /**
     * What should be returned when the player attempts to TAB-complete the command
     *
     * @param player {@link OnlineUser} doing the TAB completion
     * @param args   Current command arguments
     * @return List of String arguments to offer TAB suggestions
     */
    List<String> onTabComplete(@NotNull OnlineUser player, @NotNull String[] args);

}
