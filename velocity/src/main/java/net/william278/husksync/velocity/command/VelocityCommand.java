package net.william278.husksync.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.themoep.minedown.adventure.MineDown;
import net.william278.husksync.HuskSyncVelocity;
import net.william278.husksync.PlayerData;
import net.william278.husksync.Server;
import net.william278.husksync.Settings;
import net.william278.husksync.migrator.MPDBMigrator;
import net.william278.husksync.proxy.command.HuskSyncCommand;
import net.william278.husksync.redis.RedisMessage;
import net.william278.husksync.util.MessageManager;
import net.william278.husksync.velocity.util.VelocityUpdateChecker;
import net.william278.husksync.velocity.config.ConfigLoader;
import net.william278.husksync.velocity.config.ConfigManager;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class VelocityCommand implements SimpleCommand, HuskSyncCommand {

    private static final HuskSyncVelocity plugin = HuskSyncVelocity.getInstance();

    @Override
    public void execute(Invocation invocation) {
        final String[] args = invocation.arguments();
        final CommandSource sender = invocation.source();
        if (sender instanceof Player player) {
            if (HuskSyncVelocity.synchronisedServers.size() == 0) {
                player.sendMessage(new MineDown(MessageManager.getMessage("error_no_servers_proxied")).toComponent());
                return;
            }
            if (args.length >= 1) {
                switch (args[0].toLowerCase(Locale.ROOT)) {
                    case "about", "info" -> sendAboutInformation(player);
                    case "update" -> {
                        if (!player.hasPermission("husksync.command.inventory")) {
                            sender.sendMessage(new MineDown(MessageManager.getMessage("error_no_permission")).toComponent());
                            return;
                        }
                        sender.sendMessage(new MineDown("[Checking for HuskSync updates...](gray)").toComponent());
                        plugin.getProxyServer().getScheduler().buildTask(plugin, () -> {
                            // Check Bukkit servers needing updates
                            int updatesNeeded = 0;
                            String bukkitBrand = "Spigot";
                            String bukkitVersion = "1.0";
                            for (Server server : HuskSyncVelocity.synchronisedServers) {
                                VelocityUpdateChecker updateChecker = new VelocityUpdateChecker(server.huskSyncVersion());
                                if (!updateChecker.isUpToDate()) {
                                    updatesNeeded++;
                                    bukkitBrand = server.serverBrand();
                                    bukkitVersion = server.huskSyncVersion();
                                }
                            }

                            // Check Velocity servers needing updates and send message
                            VelocityUpdateChecker proxyUpdateChecker = new VelocityUpdateChecker(HuskSyncVelocity.VERSION);
                            if (proxyUpdateChecker.isUpToDate() && updatesNeeded == 0) {
                                sender.sendMessage(new MineDown("[HuskSync](#00fb9a bold) [| HuskSync is up-to-date, running Version " + proxyUpdateChecker.getLatestVersion() + "](#00fb9a)").toComponent());
                            } else {
                                sender.sendMessage(new MineDown("[HuskSync](#00fb9a bold) [| Your server(s) are not up-to-date:](#00fb9a)").toComponent());
                                if (!proxyUpdateChecker.isUpToDate()) {
                                    sender.sendMessage(new MineDown("[•](white) [HuskSync on the Velocity proxy is outdated (Latest: " + proxyUpdateChecker.getLatestVersion() + ", Running: " + proxyUpdateChecker.getCurrentVersion() + ")](#00fb9a)").toComponent());
                                }
                                if (updatesNeeded > 0) {
                                    sender.sendMessage(new MineDown("[•](white) [HuskSync on " + updatesNeeded + " connected " + bukkitBrand + " server(s) are outdated (Latest: " + proxyUpdateChecker.getLatestVersion() + ", Running: " + bukkitVersion + ")](#00fb9a)").toComponent());
                                }
                                sender.sendMessage(new MineDown("[•](white) [Download links:](#00fb9a) [[⏩ Spigot]](gray open_url=https://www.spigotmc.org/resources/husktowns.92672/updates) [•](#262626) [[⏩ Polymart]](gray open_url=https://polymart.org/resource/husktowns.1056/updates)").toComponent());
                            }
                        }).schedule();
                    }
                    case "invsee", "openinv", "inventory" -> {
                        if (!player.hasPermission("husksync.command.inventory")) {
                            sender.sendMessage(new MineDown(MessageManager.getMessage("error_no_permission")).toComponent());
                            return;
                        }
                        String clusterId;
                        if (Settings.clusters.size() > 1) {
                            if (args.length == 3) {
                                clusterId = args[2];
                            } else {
                                sender.sendMessage(new MineDown(MessageManager.getMessage("error_invalid_cluster")).toComponent());
                                return;
                            }
                        } else {
                            clusterId = "main";
                            for (Settings.SynchronisationCluster cluster : Settings.clusters) {
                                clusterId = cluster.clusterId();
                                break;
                            }
                        }
                        if (args.length == 2 || args.length == 3) {
                            String playerName = args[1];
                            openInventory(player, playerName, clusterId);
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
                        String clusterId;
                        if (Settings.clusters.size() > 1) {
                            if (args.length == 3) {
                                clusterId = args[2];
                            } else {
                                sender.sendMessage(new MineDown(MessageManager.getMessage("error_invalid_cluster")).toComponent());
                                return;
                            }
                        } else {
                            clusterId = "main";
                            for (Settings.SynchronisationCluster cluster : Settings.clusters) {
                                clusterId = cluster.clusterId();
                                break;
                            }
                        }
                        if (args.length == 2 || args.length == 3) {
                            String playerName = args[1];
                            openEnderChest(player, playerName, clusterId);
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
                                .replaceAll("%1%", "Velocity")).toComponent());
                    }
                    case "status" -> {
                        if (!player.hasPermission("husksync.command.admin")) {
                            sender.sendMessage(new MineDown(MessageManager.getMessage("error_no_permission")).toComponent());
                            return;
                        }
                        int playerDataSize = 0;
                        for (Settings.SynchronisationCluster cluster : Settings.clusters) {
                            playerDataSize += HuskSyncVelocity.dataManager.playerDataCache.get(cluster).playerData.size();
                        }
                        sender.sendMessage(new MineDown(MessageManager.PLUGIN_STATUS.toString()
                                .replaceAll("%1%", String.valueOf(HuskSyncVelocity.synchronisedServers.size()))
                                .replaceAll("%2%", String.valueOf(playerDataSize))).toComponent());
                    }
                    case "reload" -> {
                        if (!player.hasPermission("husksync.command.admin")) {
                            sender.sendMessage(new MineDown(MessageManager.getMessage("error_no_permission")).toComponent());
                            return;
                        }
                        ConfigManager.loadConfig();
                        ConfigLoader.loadSettings(Objects.requireNonNull(ConfigManager.getConfig()));

                        ConfigManager.loadMessages();
                        ConfigLoader.loadMessageStrings(Objects.requireNonNull(ConfigManager.getMessages()));

                        // Send reload request to all bukkit servers
                        try {
                            new RedisMessage(RedisMessage.MessageType.RELOAD_CONFIG,
                                    new RedisMessage.MessageTarget(Settings.ServerType.BUKKIT, null, null),
                                    "reload")
                                    .send();
                        } catch (IOException e) {
                            plugin.getVelocityLogger().log(Level.WARNING, "Failed to serialize reload notification message data");
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
                    MPDBMigrator migrator = HuskSyncVelocity.mpdbMigrator;
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
                                                                                        
                                            targetCluster: %9%
                                                                                            
                                            To change a setting, type:
                                            husksync migrate setting <settingName> <value>
                                                                                            
                                            Please ensure no players are logged in to the network
                                            and that at least one Spigot server is online with
                                            both HuskSync AND MySqlPlayerDataBridge installed AND
                                            that the server has been configured with the correct
                                            Redis credentials.
                                                                                            
                                            Warning: Data will be saved to your configured data
                                            source, which is currently a %10% database.
                                            Please make sure you are happy with this, or stop
                                            the proxy server and edit this in config.yml
                                                                                            
                                            Warning: Migration will overwrite any current data
                                            saved by HuskSync. It will not, however, delete any
                                            data from the source MySQLPlayerDataBridge database.
                                                                                    
                                            >When done, type: husksync migrate start"""
                                            .replaceAll("%1%", migrator.migrationSettings.sourceHost)
                                            .replaceAll("%2%", String.valueOf(migrator.migrationSettings.sourcePort))
                                            .replaceAll("%3%", migrator.migrationSettings.sourceDatabase)
                                            .replaceAll("%4%", migrator.migrationSettings.sourceUsername)
                                            .replaceAll("%5%", migrator.migrationSettings.sourcePassword)
                                            .replaceAll("%6%", migrator.migrationSettings.inventoryDataTable)
                                            .replaceAll("%7%", migrator.migrationSettings.enderChestDataTable)
                                            .replaceAll("%8%", migrator.migrationSettings.expDataTable)
                                            .replaceAll("%9%", migrator.migrationSettings.targetCluster)
                                            .replaceAll("%10%", Settings.dataStorageType.toString())
                            ).toComponent());
                            case "setting" -> {
                                if (args.length == 4) {
                                    String value = args[3];
                                    switch (args[2]) {
                                        case "sourceHost", "host" -> migrator.migrationSettings.sourceHost = value;
                                        case "sourcePort", "port" -> {
                                            try {
                                                migrator.migrationSettings.sourcePort = Integer.parseInt(value);
                                            } catch (NumberFormatException e) {
                                                sender.sendMessage(new MineDown("Error: Invalid value; port must be a number").toComponent());
                                                return;
                                            }
                                        }
                                        case "sourceDatabase", "database" -> migrator.migrationSettings.sourceDatabase = value;
                                        case "sourceUsername", "username" -> migrator.migrationSettings.sourceUsername = value;
                                        case "sourcePassword", "password" -> migrator.migrationSettings.sourcePassword = value;
                                        case "sourceInventoryTableName", "inventoryTableName", "inventoryTable" -> migrator.migrationSettings.inventoryDataTable = value;
                                        case "sourceEnderChestTableName", "enderChestTableName", "enderChestTable" -> migrator.migrationSettings.enderChestDataTable = value;
                                        case "sourceExperienceTableName", "experienceTableName", "experienceTable" -> migrator.migrationSettings.expDataTable = value;
                                        case "targetCluster", "cluster" -> migrator.migrationSettings.targetCluster = value;
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

                                // If the migrator is ready, execute the migration asynchronously
                                if (HuskSyncVelocity.mpdbMigrator.readyToMigrate(plugin.getProxyServer().getPlayerCount(),
                                        HuskSyncVelocity.synchronisedServers)) {
                                    plugin.getProxyServer().getScheduler().buildTask(plugin, () ->
                                            HuskSyncVelocity.mpdbMigrator.executeMigrationOperations(HuskSyncVelocity.dataManager,
                                                    HuskSyncVelocity.synchronisedServers, HuskSyncVelocity.redisListener)).schedule();
                                }
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
    private void openInventory(Player viewer, String targetPlayerName, String clusterId) {
        if (viewer.getUsername().equalsIgnoreCase(targetPlayerName)) {
            viewer.sendMessage(new MineDown(MessageManager.getMessage("error_cannot_view_own_inventory")).toComponent());
            return;
        }
        if (plugin.getProxyServer().getPlayer(targetPlayerName).isPresent()) {
            viewer.sendMessage(new MineDown(MessageManager.getMessage("error_cannot_view_inventory_online")).toComponent());
            return;
        }
        plugin.getProxyServer().getScheduler().buildTask(plugin, () -> {
            for (Settings.SynchronisationCluster cluster : Settings.clusters) {
                if (!cluster.clusterId().equals(clusterId)) continue;
                PlayerData playerData = HuskSyncVelocity.dataManager.getPlayerDataByName(targetPlayerName, cluster.clusterId());
                if (playerData == null) {
                    viewer.sendMessage(new MineDown(MessageManager.getMessage("error_invalid_player")).toComponent());
                    return;
                }
                try {
                    new RedisMessage(RedisMessage.MessageType.OPEN_INVENTORY,
                            new RedisMessage.MessageTarget(Settings.ServerType.BUKKIT, viewer.getUniqueId(), null),
                            targetPlayerName, RedisMessage.serialize(playerData))
                            .send();
                    viewer.sendMessage(new MineDown(MessageManager.getMessage("viewing_inventory_of").replaceAll("%1%",
                            targetPlayerName)).toComponent());
                } catch (IOException e) {
                    plugin.getVelocityLogger().log(Level.WARNING, "Failed to serialize inventory-see player data", e);
                }
                return;
            }
            viewer.sendMessage(new MineDown(MessageManager.getMessage("error_invalid_cluster")).toComponent());
        }).schedule();
    }

    // View the ender chest of a player specified by their name
    public void openEnderChest(Player viewer, String targetPlayerName, String clusterId) {
        if (viewer.getUsername().equalsIgnoreCase(targetPlayerName)) {
            viewer.sendMessage(new MineDown(MessageManager.getMessage("error_cannot_view_own_ender_chest")).toComponent());
            return;
        }
        if (plugin.getProxyServer().getPlayer(targetPlayerName).isPresent()) {
            viewer.sendMessage(new MineDown(MessageManager.getMessage("error_cannot_view_ender_chest_online")).toComponent());
            return;
        }
        plugin.getProxyServer().getScheduler().buildTask(plugin, () -> {
            for (Settings.SynchronisationCluster cluster : Settings.clusters) {
                if (!cluster.clusterId().equals(clusterId)) continue;
                PlayerData playerData = HuskSyncVelocity.dataManager.getPlayerDataByName(targetPlayerName, cluster.clusterId());
                if (playerData == null) {
                    viewer.sendMessage(new MineDown(MessageManager.getMessage("error_invalid_player")).toComponent());
                    return;
                }
                try {
                    new RedisMessage(RedisMessage.MessageType.OPEN_ENDER_CHEST,
                            new RedisMessage.MessageTarget(Settings.ServerType.BUKKIT, viewer.getUniqueId(), null),
                            targetPlayerName, RedisMessage.serialize(playerData))
                            .send();
                    viewer.sendMessage(new MineDown(MessageManager.getMessage("viewing_ender_chest_of").replaceAll("%1%",
                            targetPlayerName)).toComponent());
                } catch (IOException e) {
                    plugin.getVelocityLogger().log(Level.WARNING, "Failed to serialize inventory-see player data", e);
                }
                return;
            }
            viewer.sendMessage(new MineDown(MessageManager.getMessage("error_invalid_cluster")).toComponent());
        }).schedule();
    }

    /**
     * Send information about the plugin
     *
     * @param player The player to send it to
     */
    private void sendAboutInformation(Player player) {
        try {
            new RedisMessage(RedisMessage.MessageType.SEND_PLUGIN_INFORMATION,
                    new RedisMessage.MessageTarget(Settings.ServerType.BUKKIT, player.getUniqueId(), null),
                    "Velocity", HuskSyncVelocity.VERSION).send();
        } catch (IOException e) {
            plugin.getVelocityLogger().log(Level.WARNING, "Failed to serialize plugin information to send", e);
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        final CommandSource sender = invocation.source();
        final String[] args = invocation.arguments();

        if (sender instanceof Player player) {
            if (args.length == 1) {
                final ArrayList<String> subCommands = new ArrayList<>();
                for (SubCommand subCommand : SUB_COMMANDS) {
                    if (subCommand.permission() != null) {
                        if (!player.hasPermission(subCommand.permission())) {
                            continue;
                        }
                    }
                    subCommands.add(subCommand.command());
                }
                // Return list of subcommands
                if (args[0].length() == 0) {
                    return subCommands;
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
}