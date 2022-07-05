package net.william278.husksync.editor;

import de.themoep.minedown.MineDown;
import net.william278.husksync.command.Permission;
import net.william278.husksync.data.InventoryData;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.player.User;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class InventoryEditorMenu {

    public final InventoryData inventoryData;
    public final int slotCount;
    public final MineDown menuTitle;
    public final boolean canEdit;

    private CompletableFuture<InventoryData> inventoryDataCompletableFuture;

    private InventoryEditorMenu(@NotNull InventoryData inventoryData, int slotCount,
                                @NotNull MineDown menuTitle, boolean canEdit) {
        this.inventoryData = inventoryData;
        this.menuTitle = menuTitle;
        this.slotCount = slotCount;
        this.canEdit = canEdit;
    }

    public CompletableFuture<InventoryData> showInventory(@NotNull OnlineUser user) {
        inventoryDataCompletableFuture = new CompletableFuture<>();
        user.showMenu(this);
        return inventoryDataCompletableFuture;
    }

    public void closeInventory(@NotNull InventoryData inventoryData) {
        inventoryDataCompletableFuture.completeAsync(() -> inventoryData);
    }

    public static InventoryEditorMenu createInventoryMenu(@NotNull InventoryData inventoryData, @NotNull User dataOwner,
                                                          @NotNull OnlineUser viewer) {
        return new InventoryEditorMenu(inventoryData, 45,
                new MineDown(dataOwner.username + "'s Inventory"),
                viewer.hasPermission(Permission.COMMAND_EDIT_INVENTORIES.node));
    }

    public static InventoryEditorMenu createEnderChestMenu(@NotNull InventoryData inventoryData, @NotNull User dataOwner,
                                                           @NotNull OnlineUser viewer) {
        return new InventoryEditorMenu(inventoryData, 27,
                new MineDown(dataOwner.username + "'s Ender Chest"),
                viewer.hasPermission(Permission.COMMAND_EDIT_ENDER_CHESTS.node));
    }

}
