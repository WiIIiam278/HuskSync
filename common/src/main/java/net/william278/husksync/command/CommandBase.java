package net.william278.husksync.command;

import net.william278.husksync.HuskSync;
import net.william278.husksync.player.OnlineUser;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an abstract cross-platform representation for a plugin command
 */
public abstract class CommandBase {

    /**
     * The input string to match for this command
     */
    public final String command;

    /**
     * The permission node required to use this command
     */
    public final String permission;

    /**
     * Alias input strings for this command
     */
    public final String[] aliases;

    /**
     * Instance of the implementing plugin
     */
    public final HuskSync plugin;


    public CommandBase(@NotNull String command, @NotNull Permission permission, @NotNull HuskSync implementor, String... aliases) {
        this.command = command;
        this.permission = permission.node;
        this.plugin = implementor;
        this.aliases = aliases;
    }

    /**
     * Fires when the command is executed
     *
     * @param player {@link OnlineUser} executing the command
     * @param args   Command arguments
     */
    public abstract void onExecute(@NotNull OnlineUser player, @NotNull String[] args);

    /**
     * Returns the localised description string of this command
     *
     * @return the command description
     */
    public String getDescription() {
        return plugin.getLocales().getRawLocale(command + "_command_description")
                .orElse("A HuskSync command");
    }

}
