package me.william278.husksync.bukkit.migrator;

import me.william278.husksync.HuskSyncBukkit;
import me.william278.husksync.PlayerData;
import me.william278.husksync.bukkit.PlayerSetter;
import me.william278.husksync.bukkit.data.DataSerializer;
import me.william278.husksync.migrator.MPDBPlayerData;
import net.craftersland.data.bridge.PD;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

public class MPDBDeserializer {

    private static final HuskSyncBukkit plugin = HuskSyncBukkit.getInstance();

    // Instance of MySqlPlayerDataBridge
    private static PD mySqlPlayerDataBridge;
    public static void setMySqlPlayerDataBridge() {
        mySqlPlayerDataBridge = (PD) Bukkit.getPluginManager().getPlugin("MySqlPlayerDataBridge");
    }

    /**
     * Convert MySqlPlayerDataBridge ({@link MPDBPlayerData}) data to HuskSync's {@link PlayerData}
     *
     * @param mpdbPlayerData The {@link MPDBPlayerData} to convert
     * @return The converted {@link PlayerData}
     */
    public static PlayerData convertMPDBData(MPDBPlayerData mpdbPlayerData) {
        PlayerData playerData = PlayerData.DEFAULT_PLAYER_DATA(mpdbPlayerData.playerUUID);
        playerData.useDefaultData = false;
        if (!HuskSyncBukkit.isMySqlPlayerDataBridgeInstalled) {
            plugin.getLogger().log(Level.SEVERE, "MySqlPlayerDataBridge is not installed, failed to serialize data!");
            return null;
        }

        // Convert the data
        try {
            // Set inventory
            Inventory inventory = Bukkit.createInventory(null, InventoryType.PLAYER);
            PlayerSetter.setInventory(inventory, getItemStackArrayFromMPDBBase64String(mpdbPlayerData.inventoryData));

            playerData.setSerializedInventory(DataSerializer.getSerializedInventoryContents(inventory));
            inventory.clear();

            // Set ender chest
            playerData.setSerializedEnderChest(DataSerializer.itemStackArrayToBase64(
                    getItemStackArrayFromMPDBBase64String(mpdbPlayerData.enderChestData)));

            // Set experience
            playerData.setExpLevel(mpdbPlayerData.expLevel);
            playerData.setExpProgress(mpdbPlayerData.expProgress);
            playerData.setTotalExperience(mpdbPlayerData.totalExperience);
        } catch (IOException | InvocationTargetException | IllegalAccessException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to convert MPDB data to HuskSync's format!");
            e.printStackTrace();
        }
        return playerData;
    }

    /**
     * Returns an ItemStack array from a decoded base 64 string in MySQLPlayerDataBridge's format
     *
     * @param data The encoded ItemStack[] string from MySQLPlayerDataBridge
     * @return The {@link ItemStack[]} array
     * @throws IOException               If an error occurs during decoding
     * @throws InvocationTargetException If an error occurs during decoding
     * @throws IllegalAccessException    If an error occurs during decoding
     */
    public static ItemStack[] getItemStackArrayFromMPDBBase64String(String data) throws IOException, InvocationTargetException, IllegalAccessException {
        if (data.isEmpty()) {
            return new ItemStack[0];
        }
        return mySqlPlayerDataBridge.getItemStackSerializer().fromBase64(data);
    }
}
