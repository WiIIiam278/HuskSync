/*
 * This file is part of HuskSync by William278. Do not redistribute!
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  All rights reserved.
 *
 *  This source code is provided as reference to licensed individuals that have purchased the HuskSync
 *  plugin once from any of the official sources it is provided. The availability of this code does
 *  not grant you the rights to modify, re-distribute, compile or redistribute this source code or
 *  "plugin" outside this intended purpose. This license does not cover libraries developed by third
 *  parties that are utilised in the plugin.
 */

package net.william278.husksync.command;

import de.themoep.minedown.adventure.MineDown;
import net.william278.husksync.HuskSync;
import net.william278.husksync.data.DataSaveCause;
import net.william278.husksync.data.UserData;
import net.william278.husksync.data.UserDataBuilder;
import net.william278.husksync.data.UserDataSnapshot;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.player.User;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class InventoryCommand extends CommandBase implements TabCompletable {

    public InventoryCommand(@NotNull HuskSync implementor) {
        super("inventory", Permission.COMMAND_INVENTORY, implementor, "invsee", "openinv");
    }

    @Override
    public void onExecute(@NotNull OnlineUser player, @NotNull String[] args) {
        if (args.length == 0 || args.length > 2) {
            plugin.getLocales().getLocale("error_invalid_syntax", "/inventory <player>")
                    .ifPresent(player::sendMessage);
            return;
        }
        plugin.getDatabase().getUserByName(args[0].toLowerCase(Locale.ENGLISH)).ifPresentOrElse(user -> {
            if (args.length == 2) {
                // View user data by specified UUID
                try {
                    final UUID versionUuid = UUID.fromString(args[1]);
                    plugin.getDatabase().getUserData(user, versionUuid).ifPresentOrElse(
                            userData -> showInventoryMenu(player, userData, user, false),
                            () -> plugin.getLocales().getLocale("error_invalid_version_uuid")
                                    .ifPresent(player::sendMessage));
                } catch (IllegalArgumentException e) {
                    plugin.getLocales().getLocale("error_invalid_syntax",
                            "/inventory <player> [version_uuid]").ifPresent(player::sendMessage);
                }
            } else {
                // View (and edit) the latest user data
                plugin.getDatabase().getCurrentUserData(user).ifPresentOrElse(
                        versionedUserData -> showInventoryMenu(player, versionedUserData, user,
                                player.hasPermission(Permission.COMMAND_INVENTORY_EDIT.node)),
                        () -> plugin.getLocales().getLocale("error_no_data_to_display")
                                .ifPresent(player::sendMessage));
            }
        }, () -> plugin.getLocales().getLocale("error_invalid_player")
                .ifPresent(player::sendMessage));
    }

    private void showInventoryMenu(@NotNull OnlineUser player, @NotNull UserDataSnapshot userDataSnapshot,
                                   @NotNull User dataOwner, boolean allowEdit) {
        final UserData data = userDataSnapshot.userData();
        data.getInventory().ifPresent(itemData -> {
            // Show message
            plugin.getLocales().getLocale("inventory_viewer_opened", dataOwner.username,
                            new SimpleDateFormat("MMM dd yyyy, HH:mm:ss.sss")
                                    .format(userDataSnapshot.versionTimestamp()))
                    .ifPresent(player::sendMessage);

            // Show inventory menu
            player.showMenu(itemData, allowEdit, 5, plugin.getLocales()
                            .getLocale("inventory_viewer_menu_title", dataOwner.username)
                            .orElse(new MineDown("Inventory Viewer")))
                    .exceptionally(throwable -> {
                        plugin.log(Level.WARNING, "Exception displaying inventory menu to " + player.username, throwable);
                        return Optional.empty();
                    })
                    .thenAccept(dataOnClose -> {
                        if (dataOnClose.isEmpty() || !allowEdit) {
                            return;
                        }

                        // Create the updated data
                        final UserDataBuilder builder = UserData.builder(plugin.getMinecraftVersion());
                        data.getStatus().ifPresent(builder::setStatus);
                        data.getAdvancements().ifPresent(builder::setAdvancements);
                        data.getLocation().ifPresent(builder::setLocation);
                        data.getPersistentDataContainer().ifPresent(builder::setPersistentDataContainer);
                        data.getStatistics().ifPresent(builder::setStatistics);
                        data.getPotionEffects().ifPresent(builder::setPotionEffects);
                        data.getEnderChest().ifPresent(builder::setEnderChest);
                        builder.setInventory(dataOnClose.get());

                        // Set the updated data
                        final UserData updatedUserData = builder.build();
                        plugin.getDatabase().setUserData(dataOwner, updatedUserData, DataSaveCause.INVENTORY_COMMAND);
                        plugin.getRedisManager().sendUserDataUpdate(dataOwner, updatedUserData);
                    });
        });
    }

    @Override
    public List<String> onTabComplete(@NotNull String[] args) {
        return plugin.getOnlineUsers().stream().map(user -> user.username)
                .filter(argument -> argument.startsWith(args.length >= 1 ? args[0] : ""))
                .sorted().collect(Collectors.toList());
    }
}
