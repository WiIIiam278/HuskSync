package net.william278.husksync.command;

import net.william278.husksync.HuskSync;
import net.william278.husksync.data.ItemData;
import net.william278.husksync.data.UserData;
import net.william278.husksync.data.VersionedUserData;
import net.william278.husksync.data.DataSaveCause;
import net.william278.husksync.editor.ItemEditorMenu;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.player.User;
import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EnderChestCommand extends CommandBase {

    public EnderChestCommand(@NotNull HuskSync implementor) {
        super("enderchest", Permission.COMMAND_ENDER_CHEST, implementor, "echest", "openechest");
    }

    @Override
    public void onExecute(@NotNull OnlineUser player, @NotNull String[] args) {
        if (args.length == 0 || args.length > 2) {
            plugin.getLocales().getLocale("error_invalid_syntax", "/enderchest <player>")
                    .ifPresent(player::sendMessage);
            return;
        }
        plugin.getDatabase().getUserByName(args[0].toLowerCase()).thenAccept(optionalUser ->
                optionalUser.ifPresentOrElse(user -> {
                    if (args.length == 2) {
                        // View user data by specified UUID
                        try {
                            final UUID versionUuid = UUID.fromString(args[1]);
                            plugin.getDatabase().getUserData(user).thenAccept(userDataList -> userDataList.stream()
                                    .filter(userData -> userData.versionUUID().equals(versionUuid)).findFirst().ifPresentOrElse(
                                            userData -> showEnderChestMenu(player, userData, user, userDataList.stream().sorted().findFirst()
                                                    .map(VersionedUserData::versionUUID).orElse(UUID.randomUUID()).equals(versionUuid)),
                                            () -> plugin.getLocales().getLocale("error_invalid_version_uuid")
                                                    .ifPresent(player::sendMessage)));
                        } catch (IllegalArgumentException e) {
                            plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/enderchest <player> [version_uuid]").ifPresent(player::sendMessage);
                        }
                    } else {
                        // View latest user data
                        plugin.getDatabase().getCurrentUserData(user).thenAccept(optionalData -> optionalData.ifPresentOrElse(
                                versionedUserData -> showEnderChestMenu(player, versionedUserData, user, true),
                                () -> plugin.getLocales().getLocale("error_no_data_to_display")
                                        .ifPresent(player::sendMessage)));
                    }
                }, () -> plugin.getLocales().getLocale("error_invalid_player")
                        .ifPresent(player::sendMessage)));
    }

    private void showEnderChestMenu(@NotNull OnlineUser player, @NotNull VersionedUserData versionedUserData,
                                    @NotNull User dataOwner, final boolean allowEdit) {
        CompletableFuture.runAsync(() -> {
            final UserData data = versionedUserData.userData();
            final ItemEditorMenu menu = ItemEditorMenu.createEnderChestMenu(data.getEnderChestData(),
                    dataOwner, player, plugin.getLocales(), allowEdit);
            plugin.getLocales().getLocale("viewing_ender_chest_of", dataOwner.username,
                            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
                                    .format(versionedUserData.versionTimestamp()))
                    .ifPresent(player::sendMessage);
            final ItemData enderChestDataOnClose = plugin.getDataEditor().openItemEditorMenu(player, menu).join();
            if (!menu.canEdit) {
                return;
            }
            final UserData updatedUserData = new UserData(data.getStatusData(), data.getInventoryData(),
                    enderChestDataOnClose, data.getPotionEffectsData(), data.getAdvancementData(),
                    data.getStatisticsData(), data.getLocationData(),
                    data.getPersistentDataContainerData());
            plugin.getDatabase().setUserData(dataOwner, updatedUserData, DataSaveCause.ENDER_CHEST_COMMAND_EDIT).join();
            plugin.getRedisManager().sendUserDataUpdate(dataOwner, updatedUserData).join();
        });
    }

}
