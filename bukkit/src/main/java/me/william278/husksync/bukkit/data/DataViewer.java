package me.william278.husksync.bukkit.data;

import me.william278.husksync.HuskSyncBukkit;
import me.william278.husksync.PlayerData;
import me.william278.husksync.Settings;
import me.william278.husksync.bukkit.util.PlayerSetter;
import me.william278.husksync.redis.RedisMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;

/**
 * Class used for managing viewing inventories using inventory-see command
 */
public class DataViewer {

    /**
     * Show a viewer's data to a viewer
     *
     * @param viewer The viewing {@link Player} who will see the data
     * @param data   The {@link DataView} to show the viewer
     * @throws IOException If an exception occurred deserializing item data
     */
    public static void showData(Player viewer, DataView data) throws IOException, ClassNotFoundException {
        // Show an inventory with the viewer's inventory and equipment
        viewer.closeInventory();
        viewer.openInventory(createInventory(viewer, data));

        // Set the viewer as viewing
        HuskSyncBukkit.bukkitCache.setViewing(viewer.getUniqueId(), data);
    }

    /**
     * Handles what happens after a data viewer finishes viewing data
     *
     * @param viewer    The viewing {@link Player} who was looking at data
     * @param inventory The {@link Inventory} that was being viewed
     * @throws IOException If an exception occurred serializing item data
     */
    public static void stopShowing(Player viewer, Inventory inventory) throws IOException {
        // Get the DataView the player was looking at
        DataView dataView = HuskSyncBukkit.bukkitCache.getViewing(viewer.getUniqueId());

        // Set the player as no longer viewing an inventory
        HuskSyncBukkit.bukkitCache.removeViewing(viewer.getUniqueId());

        // Get and update the PlayerData with the new item data
        PlayerData playerData = dataView.playerData();
        String serializedItemData = DataSerializer.serializeInventory(inventory.getContents());
        switch (dataView.inventoryType()) {
            case INVENTORY -> playerData.setSerializedInventory(serializedItemData);
            case ENDER_CHEST -> playerData.setSerializedEnderChest(serializedItemData);
        }

        // Send a redis message with the updated data after the viewing
        new RedisMessage(RedisMessage.MessageType.PLAYER_DATA_UPDATE,
                new RedisMessage.MessageTarget(Settings.ServerType.BUNGEECORD, null),
                RedisMessage.serialize(playerData))
                .send();
    }

    /**
     * Creates the inventory object that the viewer will see
     *
     * @param viewer The {@link Player} who will view the data
     * @param data   The {@link DataView} data to view
     * @return The {@link Inventory} that the viewer will see
     * @throws IOException If an exception occurred deserializing item data
     */
    private static Inventory createInventory(Player viewer, DataView data) throws IOException, ClassNotFoundException {
        Inventory inventory = switch (data.inventoryType) {
            case INVENTORY -> Bukkit.createInventory(viewer, 45, data.ownerName + "'s Inventory");
            case ENDER_CHEST -> Bukkit.createInventory(viewer, 27, data.ownerName + "'s Ender Chest");
        };
        PlayerSetter.setInventory(inventory, data.getDeserializedData());
        return inventory;
    }

    /**
     * Represents Player Data being viewed by a {@link Player}
     */
    public record DataView(PlayerData playerData, String ownerName, InventoryType inventoryType) {
        /**
         * What kind of item data is being viewed
         */
        public enum InventoryType {
            /**
             * A player's inventory
             */
            INVENTORY,

            /**
             * A player's ender chest
             */
            ENDER_CHEST
        }

        /**
         * Gets the deserialized data currently being viewed
         *
         * @return The deserialized item data, as an {@link ItemStack[]} array
         * @throws IOException If an exception occurred deserializing item data
         */
        public ItemStack[] getDeserializedData() throws IOException, ClassNotFoundException {
            return switch (inventoryType) {
                case INVENTORY -> DataSerializer.deserializeInventory(playerData.getSerializedInventory());
                case ENDER_CHEST -> DataSerializer.deserializeInventory(playerData.getSerializedEnderChest());
            };
        }
    }

}
