package net.william278.husksync.editor;

import de.themoep.minedown.adventure.MineDown;
import net.william278.husksync.command.Permission;
import net.william278.husksync.config.Locales;
import net.william278.husksync.data.ItemData;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.player.User;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class ItemEditorMenu {

    public final ItemData itemData;
    public final ItemEditorMenuType itemEditorMenuType;
    public final MineDown menuTitle;
    public final boolean canEdit;

    private CompletableFuture<ItemData> inventoryDataCompletableFuture;

    private ItemEditorMenu(@NotNull ItemData itemData, ItemEditorMenuType itemEditorMenuType,
                           @NotNull MineDown menuTitle, boolean canEdit) {
        this.itemData = itemData;
        this.menuTitle = menuTitle;
        this.itemEditorMenuType = itemEditorMenuType;
        this.canEdit = canEdit;
    }

    public CompletableFuture<ItemData> showInventory(@NotNull OnlineUser user) {
        inventoryDataCompletableFuture = new CompletableFuture<>();
        user.showMenu(this);
        return inventoryDataCompletableFuture;
    }

    public void closeInventory(@NotNull ItemData itemData) {
        inventoryDataCompletableFuture.complete(itemData);
    }

    public static ItemEditorMenu createInventoryMenu(@NotNull ItemData itemData, @NotNull User dataOwner,
                                                     @NotNull OnlineUser viewer, @NotNull Locales locales,
                                                     boolean canEdit) {
        return new ItemEditorMenu(itemData, ItemEditorMenuType.INVENTORY_VIEWER,
                locales.getLocale(ItemEditorMenuType.INVENTORY_VIEWER.localeKey, dataOwner.username).orElse(new MineDown("")),
                viewer.hasPermission(Permission.COMMAND_INVENTORY_EDIT.node) && canEdit);
    }

    public static ItemEditorMenu createEnderChestMenu(@NotNull ItemData itemData, @NotNull User dataOwner,
                                                      @NotNull OnlineUser viewer, @NotNull Locales locales,
                                                      boolean canEdit) {
        return new ItemEditorMenu(itemData, ItemEditorMenuType.ENDER_CHEST_VIEWER,
                locales.getLocale(ItemEditorMenuType.ENDER_CHEST_VIEWER.localeKey, dataOwner.username).orElse(new MineDown("")),
                viewer.hasPermission(Permission.COMMAND_ENDER_CHEST_EDIT.node) && canEdit);
    }

}
