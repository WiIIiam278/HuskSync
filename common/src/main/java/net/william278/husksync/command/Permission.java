package net.william278.husksync.command;

import org.jetbrains.annotations.NotNull;

/**
 * Static plugin permission nodes required to execute commands
 */
public enum Permission {

    /*
     * /husksync command permissions
     */

    /**
     * Lets the user use the {@code /husksync} command (subcommand permissions required)
     */
    COMMAND_HUSKSYNC("husksync.command.husksync", DefaultAccess.EVERYONE),
    /**
     * Lets the user view plugin info {@code /husksync info}
     */
    COMMAND_HUSKSYNC_ABOUT("husksync.command.husksync.info", DefaultAccess.EVERYONE),
    /**
     * Lets the user reload the plugin {@code /husksync reload}
     */
    COMMAND_HUSKSYNC_RELOAD("husksync.command.husksync.reload", DefaultAccess.OPERATORS),
    /**
     * Lets the user view the plugin version and check for updates {@code /husksync update}
     */
    COMMAND_HUSKSYNC_UPDATE("husksync.command.husksync.update", DefaultAccess.OPERATORS),

    /*
     * /userdata command permissions
     */

    /**
     * Lets the user view user data {@code /userdata view/list (player) (version_uuid)}
     */
    COMMAND_USER_DATA("husksync.command.userdata", DefaultAccess.OPERATORS),
    /**
     * Lets the user restore and delete user data {@code /userdata restore/delete (player) (version_uuid)}
     */
    COMMAND_USER_DATA_MANAGE("husksync.command.userdata.manage", DefaultAccess.OPERATORS),

    /**
     * Lets the user dump user data to a file or the web {@code /userdata dump (player) (version_uuid)}
     */
    COMMAND_USER_DATA_DUMP("husksync.command.userdata.dump", DefaultAccess.NOBODY),

    /*
     * /inventory command permissions
     */

    /**
     * Lets the user use the {@code /inventory (player)} command and view offline players' inventories
     */
    COMMAND_INVENTORY("husksync.command.inventory", DefaultAccess.OPERATORS),
    /**
     * Lets the user edit the contents of offline players' inventories
     */
    COMMAND_INVENTORY_EDIT("husksync.command.inventory.edit", DefaultAccess.OPERATORS),

    /*
     * /enderchest command permissions
     */

    /**
     * Lets the user use the {@code /enderchest (player)} command and view offline players' ender chests
     */
    COMMAND_ENDER_CHEST("husksync.command.enderchest", DefaultAccess.OPERATORS),
    /**
     * Lets the user edit the contents of offline players' ender chests
     */
    COMMAND_ENDER_CHEST_EDIT("husksync.command.enderchest.edit", DefaultAccess.OPERATORS);


    public final String node;
    public final DefaultAccess defaultAccess;

    Permission(@NotNull String node, @NotNull DefaultAccess defaultAccess) {
        this.node = node;
        this.defaultAccess = defaultAccess;
    }

    /**
     * Identifies who gets what permissions by default
     */
    public enum DefaultAccess {
        /**
         * Everyone gets this permission node by default
         */
        EVERYONE,
        /**
         * Nobody gets this permission node by default
         */
        NOBODY,
        /**
         * Server operators ({@code /op}) get this permission node by default
         */
        OPERATORS
    }
}
