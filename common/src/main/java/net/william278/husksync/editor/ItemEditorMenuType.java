package net.william278.husksync.editor;

import org.jetbrains.annotations.NotNull;

public enum ItemEditorMenuType {
    INVENTORY_VIEWER(45, "inventory_viewer_menu_title"),
    ENDER_CHEST_VIEWER(27, "ender_chest_viewer_menu_title");

    public final int slotCount;
    final String localeKey;

    ItemEditorMenuType(int slotCount, @NotNull String localeKey) {
        this.slotCount = slotCount;
        this.localeKey = localeKey;
    }
}
