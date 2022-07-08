package net.william278.husksync.editor;

import de.themoep.minedown.MineDown;
import net.william278.husksync.command.Permission;
import net.william278.husksync.config.Locales;
import net.william278.husksync.data.ItemData;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.player.User;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class ItemEditorMenu {

    public final ItemData itemData;
    public final int slotCount;
    public final MineDown menuTitle;
    public boolean canEdit;

    private CompletableFuture<ItemData> inventoryDataCompletableFuture;

    private ItemEditorMenu(@NotNull ItemData itemData, int slotCount,
                           @NotNull MineDown menuTitle, boolean canEdit) {
        this.itemData = itemData;
        this.menuTitle = menuTitle;
        this.slotCount = slotCount;
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
        return new ItemEditorMenu(itemData, 45,
                locales.getLocale("inventory_viewer_menu_title", dataOwner.username).orElse(new MineDown("")),
                viewer.hasPermission(Permission.COMMAND_INVENTORY_EDIT.node) && canEdit);
    }

    public static ItemEditorMenu createEnderChestMenu(@NotNull ItemData itemData, @NotNull User dataOwner,
                                                      @NotNull OnlineUser viewer, @NotNull Locales locales,
                                                      boolean canEdit) {
        return new ItemEditorMenu(itemData, 27,
                locales.getLocale("ender_chest_viewer_menu_title", dataOwner.username).orElse(new MineDown("")),
                viewer.hasPermission(Permission.COMMAND_ENDER_CHEST_EDIT.node) && canEdit);
    }

}
