package net.william278.husksync.editor;

import net.william278.husksync.command.Permission;
import net.william278.husksync.config.Locales;
import net.william278.husksync.data.AdvancementData;
import net.william278.husksync.data.ItemData;
import net.william278.husksync.data.UserDataSnapshot;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.player.User;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Provides methods for displaying and editing user data
 */
public class DataEditor {

    /**
     * Map of currently open inventory and ender chest data editors
     */
    @NotNull
    protected final HashMap<UUID, ItemEditorMenu> openInventoryMenus;

    private final Locales locales;

    public DataEditor(@NotNull Locales locales) {
        this.openInventoryMenus = new HashMap<>();
        this.locales = locales;
    }

    /**
     * Open an inventory or ender chest editor menu
     *
     * @param user           The online user to open the editor for
     * @param itemEditorMenu The {@link ItemEditorMenu} to open
     * @see ItemEditorMenu#createInventoryMenu(ItemData, User, OnlineUser, Locales, boolean)
     * @see ItemEditorMenu#createEnderChestMenu(ItemData, User, OnlineUser, Locales, boolean)
     */
    public CompletableFuture<ItemData> openItemEditorMenu(@NotNull OnlineUser user,
                                                          @NotNull ItemEditorMenu itemEditorMenu) {
        this.openInventoryMenus.put(user.uuid, itemEditorMenu);
        return itemEditorMenu.showInventory(user);
    }

    /**
     * Close an inventory or ender chest editor menu
     *
     * @param user     The online user to close the editor for
     * @param itemData the {@link ItemData} contained within the menu at the time of closing
     */
    public void closeInventoryMenu(@NotNull OnlineUser user, @NotNull ItemData itemData) {
        if (this.openInventoryMenus.containsKey(user.uuid)) {
            this.openInventoryMenus.get(user.uuid).closeInventory(itemData);
        }
        this.openInventoryMenus.remove(user.uuid);
    }

    /**
     * Returns whether edits to the inventory or ender chest menu are allowed
     *
     * @param user The online user with an inventory open to check
     * @return {@code true} if edits to the inventory or ender chest menu are allowed; {@code false} otherwise, including if they don't have an inventory open
     */
    public boolean cancelMenuEdit(@NotNull OnlineUser user) {
        if (this.openInventoryMenus.containsKey(user.uuid)) {
            return !this.openInventoryMenus.get(user.uuid).canEdit;
        }
        return false;
    }

    /**
     * Display a chat menu detailing information about {@link UserDataSnapshot}
     *
     * @param user      The online user to display the message to
     * @param userData  The {@link UserDataSnapshot} to display information about
     * @param dataOwner The {@link User} who owns the {@link UserDataSnapshot}
     */
    public void displayDataOverview(@NotNull OnlineUser user, @NotNull UserDataSnapshot userData,
                                    @NotNull User dataOwner) {
        // Title message, timestamp, owner and cause.
        locales.getLocale("data_manager_title",
                        userData.versionUUID().toString().split("-")[0],
                        userData.versionUUID().toString(),
                        dataOwner.username,
                        dataOwner.uuid.toString())
                .ifPresent(user::sendMessage);
        locales.getLocale("data_manager_timestamp",
                        new SimpleDateFormat("MMM dd yyyy, HH:mm:ss.sss").format(userData.versionTimestamp()))
                .ifPresent(user::sendMessage);
        if (userData.pinned()) {
            locales.getLocale("data_manager_pinned").ifPresent(user::sendMessage);
        }
        locales.getLocale("data_manager_cause",
                        userData.cause().name().toLowerCase().replaceAll("_", " "))
                .ifPresent(user::sendMessage);

        // User status data, if present in the snapshot
        userData.userData().getStatus()
                .flatMap(statusData -> locales.getLocale("data_manager_status",
                        Integer.toString((int) statusData.health),
                        Integer.toString((int) statusData.maxHealth),
                        Integer.toString(statusData.hunger),
                        Integer.toString(statusData.expLevel),
                        statusData.gameMode.toLowerCase()))
                .ifPresent(user::sendMessage);

        // Advancement and statistic data, if both are present in the snapshot
        userData.userData().getAdvancements()
                .flatMap(advancementData -> userData.userData().getStatistics()
                        .flatMap(statisticsData -> locales.getLocale("data_manager_advancements_statistics",
                                Integer.toString(advancementData.size()),
                                generateAdvancementPreview(advancementData),
                                String.format("%.2f", (((statisticsData.untypedStatistics.getOrDefault(
                                        "PLAY_ONE_MINUTE", 0)) / 20d) / 60d) / 60d))))
                .ifPresent(user::sendMessage);

        if (user.hasPermission(Permission.COMMAND_INVENTORY.node)
            && user.hasPermission(Permission.COMMAND_ENDER_CHEST.node)) {
            locales.getLocale("data_manager_item_buttons",
                            dataOwner.username, userData.versionUUID().toString())
                    .ifPresent(user::sendMessage);
        }
        if (user.hasPermission(Permission.COMMAND_USER_DATA_MANAGE.node)) {
            locales.getLocale("data_manager_management_buttons",
                            dataOwner.username, userData.versionUUID().toString())
                    .ifPresent(user::sendMessage);
        }
        if (user.hasPermission(Permission.COMMAND_USER_DATA_DUMP.node)) {
            locales.getLocale("data_manager_system_buttons",
                            dataOwner.username, userData.versionUUID().toString())
                    .ifPresent(user::sendMessage);
        }
    }

    @NotNull
    private String generateAdvancementPreview(@NotNull List<AdvancementData> advancementData) {
        final StringJoiner joiner = new StringJoiner("\n");
        final List<AdvancementData> advancementsToPreview = advancementData.stream().filter(dataItem ->
                !dataItem.key.startsWith("minecraft:recipes/")).toList();
        final int PREVIEW_SIZE = 8;
        for (int i = 0; i < advancementsToPreview.size(); i++) {
            joiner.add(advancementsToPreview.get(i).key);
            if (i >= PREVIEW_SIZE) {
                break;
            }
        }
        final int remainingAdvancements = advancementsToPreview.size() - PREVIEW_SIZE;
        if (remainingAdvancements > 0) {
            joiner.add(locales.getRawLocale("data_manager_advancements_preview_remaining",
                    Integer.toString(remainingAdvancements)).orElse("+" + remainingAdvancements + "…"));
        }
        return joiner.toString();
    }

    /**
     * Display a paginated chat list of {@link UserDataSnapshot}s
     *
     * @param user         The online user to display the message to
     * @param userDataList The list of {@link UserDataSnapshot}s to display
     * @param dataOwner    The {@link User} who owns the {@link UserDataSnapshot}s
     * @param page         The page of the list to display
     */
    public void displayDataSnapshotList(@NotNull OnlineUser user, @NotNull List<UserDataSnapshot> userDataList,
                                        @NotNull User dataOwner, final int page) {
        DataSnapshotList.create(userDataList, dataOwner, locales).displayPage(user, page);
    }

    /**
     * Returns whether the user has an inventory editor menu open
     *
     * @param user {@link OnlineUser} to check
     * @return {@code true} if the user has an inventory editor open; {@code false} otherwise
     */
    public Optional<ItemEditorMenu> getEditingInventoryData(@NotNull OnlineUser user) {
        return this.openInventoryMenus.containsKey(user.uuid) ? Optional.of(this.openInventoryMenus.get(user.uuid))
                : Optional.empty();
    }
}
