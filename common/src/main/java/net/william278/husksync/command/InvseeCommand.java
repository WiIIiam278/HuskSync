package net.william278.husksync.command;

import net.william278.husksync.HuskSync;
import net.william278.husksync.data.UserData;
import net.william278.husksync.data.VersionedUserData;
import net.william278.husksync.editor.InventoryEditorMenu;
import net.william278.husksync.player.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class InvseeCommand extends CommandBase {

    public InvseeCommand(@NotNull HuskSync implementor) {
        super("invsee", Permission.COMMAND_VIEW_INVENTORIES, implementor, "openinv");
    }

    @Override
    public void onExecute(@NotNull OnlineUser player, @NotNull String[] args) {
        if (args.length == 0 || args.length > 2) {
            plugin.getLocales().getLocale("error_invalid_syntax", "/invsee <player>")
                    .ifPresent(player::sendMessage);
            return;
        }
        plugin.getDatabase().getUserByName(args[0].toLowerCase()).thenAcceptAsync(optionalUser -> {
            optionalUser.ifPresentOrElse(user -> {
                List<VersionedUserData> userData = plugin.getDatabase().getUserData(user).join();
                Optional<VersionedUserData> dataToView;
                if (args.length == 2) {
                    try {
                        final UUID version = UUID.fromString(args[1]);
                        dataToView = userData.stream().filter(data -> data.versionUUID().equals(version)).findFirst();
                    } catch (IllegalArgumentException e) {
                        plugin.getLocales().getLocale("error_invalid_syntax",
                                "/invsee <player> [version_uuid]").ifPresent(player::sendMessage);
                        return;
                    }
                } else {
                    dataToView = userData.stream().sorted().findFirst();
                }
                dataToView.ifPresentOrElse(versionedUserData -> {
                    final UserData data = versionedUserData.userData();
                    final InventoryEditorMenu menu = InventoryEditorMenu.createInventoryMenu(
                            data.getInventoryData(), user, player);
                    plugin.getLocales().getLocale("viewing_inventory_of", user.username)
                            .ifPresent(player::sendMessage);
                    plugin.getDataEditor().openInventoryMenu(player, menu).thenAcceptAsync(inventoryDataOnClose -> {
                        final UserData updatedUserData = new UserData(data.getStatusData(),
                                menu.canEdit ? inventoryDataOnClose : data.getInventoryData(),
                                data.getEnderChestData(), data.getPotionEffectData(), data.getAdvancementData(),
                                data.getStatisticData(), data.getLocationData(),
                                data.getPersistentDataContainerData());
                        plugin.getDatabase().setUserData(user, updatedUserData).join();
                    });
                }, () -> plugin.getLocales().getLocale(args.length == 2 ? "error_invalid_version_uuid"
                        : "error_no_data_to_display").ifPresent(player::sendMessage));
            }, () -> plugin.getLocales().getLocale("error_invalid_player").ifPresent(player::sendMessage));
        });
    }

}
