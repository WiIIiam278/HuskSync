package me.william278.husksync.bungeecord.command;

import de.themoep.minedown.MineDown;
import me.william278.husksync.HuskSyncBungeeCord;
import me.william278.husksync.MessageManager;
import me.william278.husksync.PlayerData;
import me.william278.husksync.Settings;
import me.william278.husksync.bungeecord.config.ConfigLoader;
import me.william278.husksync.bungeecord.config.ConfigManager;
import me.william278.husksync.bungeecord.data.DataManager;
import me.william278.husksync.bungeecord.migrator.MPDBMigrator;
import me.william278.husksync.redis.RedisMessage;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class HuskSyncCommand extends Command implements TabExecutor {

    private final static HuskSyncBungeeCord plugin = HuskSyncBungeeCord.getInstance();
    private final static SubCommand[] SUB_COMMANDS = {new SubCommand("about", null),
            new SubCommand("status", "husksync.command.admin"),
            new SubCommand("reload", "husksync.command.admin"),
            new SubCommand("invsee", "husksync.command.inventory"),
            new SubCommand("echest", "husksync.command.ender_chest")};

    public HuskSyncCommand() {
        super("husksync", null, "hs");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof ProxiedPlayer player) {
            if (HuskSyncBungeeCord.synchronisedServers.size() == 0) {
                player.sendMessage(new MineDown(MessageManager.getMessage("error_no_servers_proxied")).toComponent());
                return;
            }
            if (args.length >= 1) {
                switch (args[0].toLowerCase(Locale.ROOT)) {
                    case "about", "info" -> sendAboutInformation(player);
                    case "invsee", "openinv", "inventory" -> {
                        if (!player.hasPermission("husksync.command.inventory")) {
                            sender.sendMessage(new MineDown(MessageManager.getMessage("error_no_permission")).toComponent());
                            return;
                        }
                        if (args.length == 2) {
                            String playerName = args[1];
                            openInventory(player, playerName);
                        } else {
                            sender.sendMessage(new MineDown(MessageManager.getMessage("error_invalid_syntax").replaceAll("%1%",
                                    "/husksync invsee <player>")).toComponent());
                        }
                    }
                    case "echest", "enderchest" -> {
                        if (!player.hasPermission("husksync.command.ender_chest")) {
                            sender.sendMessage(new MineDown(MessageManager.getMessage("error_no_permission")).toComponent());
                            return;
                        }
                        if (args.length == 2) {
                            String playerName = args[1];
                            openEnderChest(player, playerName);
                        } else {
                            sender.sendMessage(new MineDown(MessageManager.getMessage("error_invalid_syntax")
                                    .replaceAll("%1%", "/husksync echest <player>")).toComponent());
                        }
                    }
                    case "migrate" -> {
                        if (!player.hasPermission("husksync.command.admin")) {
                            sender.sendMessage(new MineDown(MessageManager.getMessage("error_no_permission")).toComponent());
                            return;
                        }
                        sender.sendMessage(new MineDown(MessageManager.getMessage("error_console_command_only")
                                .replaceAll("%1%", ProxyServer.getInstance().getName())).toComponent());
                    }
                    case "status" -> {
                        if (!player.hasPermission("husksync.command.admin")) {
                            sender.sendMessage(new MineDown(MessageManager.getMessage("error_no_permission")).toComponent());
                            return;
                        }
                        sender.sendMessage(new MineDown(MessageManager.PLUGIN_STATUS.toString()
                                .replaceAll("%1%", String.valueOf(HuskSyncBungeeCord.synchronisedServers.size()))
                                .replaceAll("%2%", String.valueOf(DataManager.playerDataCache.playerData.size()))).toComponent());
                    }
                    case "reload" -> {
                        if (!player.hasPermission("husksync.command.admin")) {
                            sender.sendMessage(new MineDown(MessageManager.getMessage("error_no_permission")).toComponent());
                            return;
                        }
                        ConfigManager.loadConfig();
                        ConfigLoader.loadSettings(Objects.requireNonNull(ConfigManager.getConfig()));
                        ConfigManager.loadMessages(Settings.language);
                        ConfigLoader.loadMessages(Objects.requireNonNull(ConfigManager.getMessages(Settings.language)));

                        // Send reload request to all bukkit servers
                        try {
                            new RedisMessage(RedisMessage.MessageType.RELOAD_CONFIG,
                                    new RedisMessage.MessageTarget(Settings.ServerType.BUKKIT, null),
                                    "reload")
                                    .send();
                        } catch (IOException e) {
                            plugin.getLogger().log(Level.WARNING, "Failed to serialize reload notification message data");
                        }

                        sender.sendMessage(new MineDown(MessageManager.getMessage("reload_complete")).toComponent());
                    }
                    default -> sender.sendMessage(new MineDown(MessageManager.getMessage("error_invalid_syntax").replaceAll("%1%",
                            "/husksync <about/status/invsee/echest>")).toComponent());
                }
            } else {
                sendAboutInformation(player);
            }
        } else {
            // Database migration wizard
            if (args.length >= 1) {
                if (args[0].equalsIgnoreCase("migrate")) {
                    if (args.length == 1) {
                        sender.sendMessage(new MineDown(
                                """
                                        === MySQLPlayerDataBridge Migration Wizard ==========
                                        This will migrate data from the MySQLPlayerDataBridge
                                        plugin to HuskSync.
                                                                                
                                        Data that will be migrated:
                                        - Inventories
                                        - Ender Chests
                                        - Experience points
                                                                                
                                        Other non-vital data, such as current health, hunger
                                        & potion effects will not be migrated to ensure that
                                        migration does not take an excessive amount of time.
                                        
                                        To do this, you need to have MySqlPlayerDataBridge
                                        and HuskSync installed on one Spigot server as well
                                        as HuskSync installed on the proxy (which you have)
                                                                                
                                        >To proceed, type: husksync migrate setup""").toComponent());
                    } else {
                        switch (args[1].toLowerCase()) {
                            case "setup" -> sender.sendMessage(new MineDown(
                                    """
                                            === MySQLPlayerDataBridge Migration Wizard ==========
                                            The following database settings will be used.
                                            Please make sure they match the correct settings to
                                            access your MySQLPlayerDataBridge Data
                                                                                            
                                            sourceHost: %1%
                                            sourcePort: %2%
                                            sourceDatabase: %3%
                                            sourceUsername: %4%
                                            sourcePassword: %5%
                                                                                            
                                            sourceInventoryTableName: %6%
                                            sourceEnderChestTableName: %7%
                                            sourceExperienceTableName: %8%
                                                                                            
                                            To change a setting, type:
                                            husksync migrate setting <settingName> <value>
                                                                                            
                                            Please ensure no players are logged in to the network
                                            and that at least one Spigot server is online with
                                            both HuskSync AND MySqlPlayerDataBridge installed AND
                                            that the server has been configured with the correct
                                            Redis credentials.
                                                                                            
                                            Warning: Data will be saved to your configured data
                                            source, which is currently a %9% database.
                                            Please make sure you are happy with this, or stop
                                            the proxy server and edit this in config.yml
                                                                                            
                                            Warning: Migration will overwrite any current data
                                            saved by HuskSync. It will not, however, delete any
                                            data from the source MySQLPlayerDataBridge database.
                                                                                    
                                            >When done, type: husksync migrate start"""
                                            .replaceAll("%1%", MPDBMigrator.migrationSettings.sourceHost)
                                            .replaceAll("%2%", String.valueOf(MPDBMigrator.migrationSettings.sourcePort))
                                            .replaceAll("%3%", MPDBMigrator.migrationSettings.sourceDatabase)
                                            .replaceAll("%4%", MPDBMigrator.migrationSettings.sourceUsername)
                                            .replaceAll("%5%", MPDBMigrator.migrationSettings.sourcePassword)
                                            .replaceAll("%6%", MPDBMigrator.migrationSettings.inventoryDataTable)
                                            .replaceAll("%7%", MPDBMigrator.migrationSettings.enderChestDataTable)
                                            .replaceAll("%8%", MPDBMigrator.migrationSettings.expDataTable)
                                            .replaceAll("%9%", Settings.dataStorageType.toString())
                            ).toComponent());
                            case "setting" -> {
                                if (args.length == 4) {
                                    String value = args[3];
                                    switch (args[2]) {
                                        case "sourceHost", "host" -> MPDBMigrator.migrationSettings.sourceHost = value;
                                        case "sourcePort", "port" -> {
                                            try {
                                                MPDBMigrator.migrationSettings.sourcePort = Integer.parseInt(value);
                                            } catch (NumberFormatException e) {
                                                sender.sendMessage(new MineDown("Error: Invalid value; port must be a number").toComponent());
                                                return;
                                            }
                                        }
                                        case "sourceDatabase", "database" -> MPDBMigrator.migrationSettings.sourceDatabase = value;
                                        case "sourceUsername", "username" -> MPDBMigrator.migrationSettings.sourceUsername = value;
                                        case "sourcePassword", "password" -> MPDBMigrator.migrationSettings.sourcePassword = value;
                                        case "sourceInventoryTableName", "inventoryTableName", "inventoryTable" -> MPDBMigrator.migrationSettings.inventoryDataTable = value;
                                        case "sourceEnderChestTableName", "enderChestTableName", "enderChestTable" -> MPDBMigrator.migrationSettings.enderChestDataTable = value;
                                        case "sourceExperienceTableName", "experienceTableName", "experienceTable" -> MPDBMigrator.migrationSettings.expDataTable = value;
                                        default -> {
                                            sender.sendMessage(new MineDown("Error: Invalid setting; please use \"husksync migrate setup\" to view a list").toComponent());
                                            return;
                                        }
                                    }
                                    sender.sendMessage(new MineDown("Successfully updated setting: \"" + args[2] + "\" --> \"" + value + "\"").toComponent());
                                } else {
                                    sender.sendMessage(new MineDown("Error: Invalid usage. Syntax: husksync migrate setting <settingName> <value>").toComponent());
                                }
                            }
                            case "start" -> {
                                sender.sendMessage(new MineDown("Starting MySQLPlayerDataBridge migration!...").toComponent());
                                HuskSyncBungeeCord.mpdbMigrator.start();
                            }
                            default -> sender.sendMessage(new MineDown("Error: Invalid argument for migration. Use \"husksync migrate\" to start the process").toComponent());
                        }
                    }
                    return;
                }
            }
            sender.sendMessage(new MineDown("Error: Invalid syntax. Usage: husksync migrate <args>").toComponent());
        }
    }

    // View the inventory of a player specified by their name
    private void openInventory(ProxiedPlayer viewer, String targetPlayerName) {
        if (viewer.getName().equalsIgnoreCase(targetPlayerName)) {
            viewer.sendMessage(new MineDown(MessageManager.getMessage("error_cannot_view_own_ender_chest")).toComponent());
            return;
        }
        if (ProxyServer.getInstance().getPlayer(targetPlayerName) != null) {
            viewer.sendMessage(new MineDown(MessageManager.getMessage("error_cannot_view_inventory_online")).toComponent());
            return;
        }
        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            PlayerData playerData = DataManager.getPlayerDataByName(targetPlayerName);
            if (playerData == null) {
                viewer.sendMessage(new MineDown(MessageManager.getMessage("error_invalid_player")).toComponent());
                return;
            }
            try {
                new RedisMessage(RedisMessage.MessageType.OPEN_INVENTORY,
                        new RedisMessage.MessageTarget(Settings.ServerType.BUKKIT, viewer.getUniqueId()),
                        targetPlayerName, RedisMessage.serialize(playerData))
                        .send();
                viewer.sendMessage(new MineDown(MessageManager.getMessage("viewing_inventory_of").replaceAll("%1%",
                        targetPlayerName)).toComponent());
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to serialize inventory-see player data", e);
            }
        });
    }

    // View the ender chest of a player specified by their name
    private void openEnderChest(ProxiedPlayer viewer, String targetPlayerName) {
        if (viewer.getName().equalsIgnoreCase(targetPlayerName)) {
            viewer.sendMessage(new MineDown(MessageManager.getMessage("error_cannot_view_own_ender_chest")).toComponent());
            return;
        }
        if (ProxyServer.getInstance().getPlayer(targetPlayerName) != null) {
            viewer.sendMessage(new MineDown(MessageManager.getMessage("error_cannot_view_ender_chest_online")).toComponent());
            return;
        }
        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            PlayerData playerData = DataManager.getPlayerDataByName(targetPlayerName);
            if (playerData == null) {
                viewer.sendMessage(new MineDown(MessageManager.getMessage("error_invalid_player")).toComponent());
                return;
            }
            try {
                new RedisMessage(RedisMessage.MessageType.OPEN_ENDER_CHEST,
                        new RedisMessage.MessageTarget(Settings.ServerType.BUKKIT, viewer.getUniqueId()),
                        targetPlayerName, RedisMessage.serialize(playerData))
                        .send();
                viewer.sendMessage(new MineDown(MessageManager.getMessage("viewing_ender_chest_of").replaceAll("%1%",
                        targetPlayerName)).toComponent());
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to serialize inventory-see player data", e);
            }
        });
    }

    /**
     * Send information about the plugin
     *
     * @param player The player to send it to
     */
    private void sendAboutInformation(ProxiedPlayer player) {
        try {
            new RedisMessage(RedisMessage.MessageType.SEND_PLUGIN_INFORMATION,
                    new RedisMessage.MessageTarget(Settings.ServerType.BUKKIT, player.getUniqueId()),
                    plugin.getProxy().getName(), plugin.getDescription().getVersion()).send();
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to serialize plugin information to send", e);
        }
    }

    // Tab completion
    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (sender instanceof ProxiedPlayer player) {
            if (args.length == 1) {
                final ArrayList<String> subCommands = new ArrayList<>();
                for (SubCommand subCommand : SUB_COMMANDS) {
                    if (subCommand.doesPlayerHavePermission(player)) {
                        subCommands.add(subCommand.command());
                    }
                }
                // Automatically filter the sub commands' order in tab completion by what the player has typed
                return subCommands.stream().filter(val -> val.startsWith(args[0]))
                        .sorted().collect(Collectors.toList());
            } else {
                return Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }

    /**
     * A sub command, that may require a permission
     */
    public record SubCommand(String command, String permission) {
        /**
         * Returns if the player can use the sub command
         *
         * @param player The {@link ProxiedPlayer} to check
         * @return {@code true} if the player can use the sub command; {@code false} otherwise
         */
        public boolean doesPlayerHavePermission(ProxiedPlayer player) {
            return permission == null || player.hasPermission(permission);
        }
    }

}
