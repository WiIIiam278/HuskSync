package net.william278.husksync.editor;

import de.themoep.minedown.MineDown;
import net.william278.husksync.command.Permission;
import net.william278.husksync.data.ItemData;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.player.User;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class InventoryEditorMenu {

    public final ItemData itemData;
    public final int slotCount;
    public final MineDown menuTitle;
    public final boolean canEdit;

    private CompletableFuture<ItemData> inventoryDataCompletableFuture;

    private InventoryEditorMenu(@NotNull ItemData itemData, int slotCount,
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
        inventoryDataCompletableFuture.completeAsync(() -> itemData);
    }

    public static InventoryEditorMenu createInventoryMenu(@NotNull ItemData itemData, @NotNull User dataOwner,
                                                          @NotNull OnlineUser viewer) {
        return new InventoryEditorMenu(itemData, 45,
                new MineDown(dataOwner.username + "'s Inventory"),
                viewer.hasPermission(Permission.COMMAND_EDIT_INVENTORIES.node));
    }

    public static InventoryEditorMenu createEnderChestMenu(@NotNull ItemData itemData, @NotNull User dataOwner,
                                                           @NotNull OnlineUser viewer) {
        return new InventoryEditorMenu(itemData, 27,
                new MineDown(dataOwner.username + "'s Ender Chest"),
                viewer.hasPermission(Permission.COMMAND_EDIT_ENDER_CHESTS.node));
    }

}
