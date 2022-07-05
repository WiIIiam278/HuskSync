package net.william278.husksync.editor;

import net.william278.husksync.config.Locales;
import net.william278.husksync.data.InventoryData;
import net.william278.husksync.data.VersionedUserData;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.player.User;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Provides methods for displaying and editing user data
 */
public class DataEditor {

    /**
     * Map of currently open inventory and ender chest data editors
     */
    @NotNull
    protected final HashMap<UUID, InventoryEditorMenu> openInventoryMenus;

    public DataEditor() {
        this.openInventoryMenus = new HashMap<>();
    }

    /**
     * Open an inventory or ender chest editor menu
     *
     * @param user                The online user to open the editor for
     * @param inventoryEditorMenu The {@link InventoryEditorMenu} to open
     * @return The inventory editor menu
     * @see InventoryEditorMenu#createInventoryMenu(InventoryData, User, OnlineUser)
     * @see InventoryEditorMenu#createEnderChestMenu(InventoryData, User, OnlineUser)
     */
    public CompletableFuture<InventoryData> openInventoryMenu(@NotNull OnlineUser user,
                                                              @NotNull InventoryEditorMenu inventoryEditorMenu) {
        this.openInventoryMenus.put(user.uuid, inventoryEditorMenu);
        return inventoryEditorMenu.showInventory(user);
    }

    /**
     * Close an inventory or ender chest editor menu
     *
     * @param user          The online user to close the editor for
     * @param inventoryData the {@link InventoryData} contained within the menu at the time of closing
     */
    public void closeInventoryMenu(@NotNull OnlineUser user, @NotNull InventoryData inventoryData) {
        if (this.openInventoryMenus.containsKey(user.uuid)) {
            this.openInventoryMenus.get(user.uuid).closeInventory(inventoryData);
        }
        this.openInventoryMenus.remove(user.uuid);
    }

    /**
     * Returns whether edits to the inventory or ender chest menu are allowed
     *
     * @param user The online user with an inventory open to check
     * @return {@code true} if edits to the inventory or ender chest menu are allowed; {@code false} otherwise, including if they don't have an inventory open
     */
    public boolean cancelInventoryEdit(@NotNull OnlineUser user) {
        if (this.openInventoryMenus.containsKey(user.uuid)) {
            return !this.openInventoryMenus.get(user.uuid).canEdit;
        }
        return false;
    }

    /**
     * Display a chat message detailing information about {@link VersionedUserData}
     *
     * @param user      The online user to display the message to
     * @param userData  The {@link VersionedUserData} to display information about
     * @param dataOwner The {@link User} who owns the {@link VersionedUserData}
     */
    public void displayDataOverview(@NotNull OnlineUser user, @NotNull VersionedUserData userData,
                                    @NotNull User dataOwner) {
        //todo
    }

    /**
     * Returns whether the user has an inventory editor menu open
     * @param user {@link OnlineUser} to check
     * @return {@code true} if the user has an inventory editor open; {@code false} otherwise
     */
    public boolean isEditingInventoryData(@NotNull OnlineUser user) {
        return this.openInventoryMenus.containsKey(user.uuid);
    }
}
