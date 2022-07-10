package net.william278.husksync.command;

import net.william278.husksync.HuskSync;
import net.william278.husksync.data.DataSaveCause;
import net.william278.husksync.data.ItemData;
import net.william278.husksync.data.UserData;
import net.william278.husksync.data.UserDataSnapshot;
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

public class EnderChestCommand extends CommandBase implements TabCompletable {

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
                            plugin.getDatabase().getUserData(user, versionUuid).thenAccept(data -> data.ifPresentOrElse(
                                    userData -> showEnderChestMenu(player, userData, user, false),
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

    private void showEnderChestMenu(@NotNull OnlineUser player, @NotNull UserDataSnapshot userDataSnapshot,
                                    @NotNull User dataOwner, final boolean allowEdit) {
        CompletableFuture.runAsync(() -> {
            final UserData data = userDataSnapshot.userData();
            final ItemEditorMenu menu = ItemEditorMenu.createEnderChestMenu(data.getEnderChestData(),
                    dataOwner, player, plugin.getLocales(), allowEdit);
            plugin.getLocales().getLocale("viewing_ender_chest_of", dataOwner.username,
                            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
                                    .format(userDataSnapshot.versionTimestamp()))
                    .ifPresent(player::sendMessage);
            final ItemData enderChestDataOnClose = plugin.getDataEditor().openItemEditorMenu(player, menu).join();
            if (!menu.canEdit) {
                return;
            }
            final UserData updatedUserData = new UserData(data.getStatusData(), data.getInventoryData(),
                    enderChestDataOnClose, data.getPotionEffectsData(), data.getAdvancementData(),
                    data.getStatisticsData(), data.getLocationData(),
                    data.getPersistentDataContainerData(),
                    plugin.getMinecraftVersion().getWithoutMeta());
            plugin.getDatabase().setUserData(dataOwner, updatedUserData, DataSaveCause.ENDER_CHEST_COMMAND_EDIT).join();
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
