package net.william278.husksync.command;

import net.william278.husksync.HuskSync;
import net.william278.husksync.data.DataSaveCause;
import net.william278.husksync.data.ItemData;
import net.william278.husksync.data.UserData;
import net.william278.husksync.data.VersionedUserData;
import net.william278.husksync.editor.ItemEditorMenu;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.player.User;
import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
        plugin.getDatabase().getUserByName(args[0].toLowerCase()).thenAccept(optionalUser ->
                optionalUser.ifPresentOrElse(user -> {
                    if (args.length == 2) {
                        // View user data by specified UUID
                        try {
                            final UUID versionUuid = UUID.fromString(args[1]);
                            plugin.getDatabase().getUserData(user, versionUuid).thenAccept(data -> data.ifPresentOrElse(
                                    userData -> showInventoryMenu(player, userData, user, false),
                                    () -> plugin.getLocales().getLocale("error_invalid_version_uuid")
                                            .ifPresent(player::sendMessage)));
                        } catch (IllegalArgumentException e) {
                            plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/inventory <player> [version_uuid]").ifPresent(player::sendMessage);
                        }
                    } else {
                        // View latest user data
                        plugin.getDatabase().getCurrentUserData(user).thenAccept(optionalData -> optionalData.ifPresentOrElse(
                                versionedUserData -> showInventoryMenu(player, versionedUserData, user, true),
                                () -> plugin.getLocales().getLocale("error_no_data_to_display")
                                        .ifPresent(player::sendMessage)));
                    }
                }, () -> plugin.getLocales().getLocale("error_invalid_player")
                        .ifPresent(player::sendMessage)));
    }

    private void showInventoryMenu(@NotNull OnlineUser player, @NotNull VersionedUserData versionedUserData,
                                   @NotNull User dataOwner, boolean allowEdit) {
        CompletableFuture.runAsync(() -> {
            final UserData data = versionedUserData.userData();
            final ItemEditorMenu menu = ItemEditorMenu.createInventoryMenu(data.getInventoryData(),
                    dataOwner, player, plugin.getLocales(), allowEdit);
            plugin.getLocales().getLocale("viewing_inventory_of", dataOwner.username,
                            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
                                    .format(versionedUserData.versionTimestamp()))
                    .ifPresent(player::sendMessage);
            final ItemData inventoryDataOnClose = plugin.getDataEditor().openItemEditorMenu(player, menu).join();
            if (!menu.canEdit) {
                return;
            }
            final UserData updatedUserData = new UserData(data.getStatusData(), inventoryDataOnClose,
                    data.getEnderChestData(), data.getPotionEffectsData(), data.getAdvancementData(),
                    data.getStatisticsData(), data.getLocationData(),
                    data.getPersistentDataContainerData());
            plugin.getDatabase().setUserData(dataOwner, updatedUserData, DataSaveCause.INVENTORY_COMMAND_EDIT).join();
            plugin.getRedisManager().sendUserDataUpdate(dataOwner, updatedUserData).join();
        });
    }

    @Override
    public List<String> onTabComplete(@NotNull OnlineUser player, @NotNull String[] args) {
        return plugin.getOnlineUsers().stream().map(user -> user.username)
                .filter(argument -> argument.startsWith(args.length >= 1 ? args[0] : ""))
                .sorted().collect(Collectors.toList());
    }
}
