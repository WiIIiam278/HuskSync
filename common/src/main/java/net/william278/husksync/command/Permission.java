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
    COMMAND_HUSKSYNC_INFO("husksync.command.husksync.info", DefaultAccess.EVERYONE),
    /**
     * Lets the user reload the plugin {@code /husksync reload}
     */
    COMMAND_HUSKSYNC_RELOAD("husksync.command.husksync.reload", DefaultAccess.OPERATORS),
    /**
     * Lets the user view the plugin version and check for updates {@code /husksync update}
     */
    COMMAND_HUSKSYNC_UPDATE("husksync.command.husksync.update", DefaultAccess.OPERATORS),
    /**
     * Lets the user save a player's data {@code /husksync save (player)}
     */
    COMMAND_HUSKSYNC_SAVE("husksync.command.husksync.save", DefaultAccess.OPERATORS),
    /**
     * Lets the user save all online player data {@code /husksync saveall}
     */
    COMMAND_HUSKSYNC_SAVE_ALL("husksync.command.husksync.saveall", DefaultAccess.OPERATORS),
    /**
     * Lets the user view a player's backup data {@code /husksync backup (player)}
     */
    COMMAND_HUSKSYNC_BACKUPS("husksync.command.husksync.backups", DefaultAccess.OPERATORS),
    /**
     * Lets the user restore a player's backup data {@code /husksync backup (player) restore (backup_uuid)}
     */
    COMMAND_HUSKSYNC_BACKUPS_RESTORE("husksync.command.husksync.backups.restore", DefaultAccess.OPERATORS),

    /*
     * /invsee command permissions
     */

    /**
     * Lets the user use the {@code /invsee (player)} command and view offline players' inventories
     */
    COMMAND_VIEW_INVENTORIES("husksync.command.invsee", DefaultAccess.OPERATORS),
    /**
     * Lets the user edit the contents of offline players' inventories
     */
    COMMAND_EDIT_INVENTORIES("husksync.command.invsee.edit", DefaultAccess.OPERATORS),

    /*
     * /echest command permissions
     */

    /**
     * Lets the user use the {@code /echest (player)} command and view offline players' ender chests
     */
    COMMAND_VIEW_ENDER_CHESTS("husksync.command.echest", DefaultAccess.OPERATORS),
    /**
     * Lets the user edit the contents of offline players' ender chests
     */
    COMMAND_EDIT_ENDER_CHESTS("husksync.command.echest.edit", DefaultAccess.OPERATORS);

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
