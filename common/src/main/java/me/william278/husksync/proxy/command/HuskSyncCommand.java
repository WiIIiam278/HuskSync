package me.william278.husksync.proxy.command;

public interface HuskSyncCommand {

    SubCommand[] SUB_COMMANDS = {new SubCommand("about", null),
            new SubCommand("status", "husksync.command.admin"),
            new SubCommand("reload", "husksync.command.admin"),
            new SubCommand("update", "husksync.command.admin"),
            new SubCommand("invsee", "husksync.command.inventory"),
            new SubCommand("echest", "husksync.command.ender_chest")};

    /**
     * A sub command, that may require a permission
     */
    record SubCommand(String command, String permission) { }

}
