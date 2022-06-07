package net.william278.husksync.bukkit.migrator;

import net.william278.husksync.HuskSyncBukkit;
import net.william278.husksync.PlayerData;
import net.william278.husksync.bukkit.data.DataSerializer;
import net.william278.husksync.bukkit.util.PlayerSetter;
import net.william278.husksync.migrator.MPDBPlayerData;
import net.william278.mpdbconverter.MPDBConverter;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

public class MPDBDeserializer {

    private static final HuskSyncBukkit plugin = HuskSyncBukkit.getInstance();

    // Instance of MySqlPlayerDataBridge
    private static MPDBConverter mpdbConverter;

    public static void setMySqlPlayerDataBridge() {
        Plugin mpdbPlugin = Bukkit.getPluginManager().getPlugin("MySqlPlayerDataBridge");
        assert mpdbPlugin != null;
        mpdbConverter = MPDBConverter.getInstance(mpdbPlugin);
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
            // Set inventory contents
            Inventory inventory = Bukkit.createInventory(null, InventoryType.PLAYER);
            if (!mpdbPlayerData.inventoryData.isEmpty() && !mpdbPlayerData.inventoryData.equalsIgnoreCase("none")) {
                PlayerSetter.setInventory(inventory, mpdbConverter.getItemStackFromSerializedData(mpdbPlayerData.inventoryData));
            }

            // Set armor (if there is data; MPDB stores empty data with literally the word "none". Obviously.)
            int armorSlot = 36;
            if (!mpdbPlayerData.armorData.isEmpty() && !mpdbPlayerData.armorData.equalsIgnoreCase("none")) {
                ItemStack[] armorItems = mpdbConverter.getItemStackFromSerializedData(mpdbPlayerData.armorData);
                for (ItemStack armorPiece : armorItems) {
                    if (armorPiece != null) {
                        inventory.setItem(armorSlot, armorPiece);
                    }
                    armorSlot++;
                }

            }

            // Now apply the contents and clear the temporary inventory variable
            playerData.setSerializedInventory(DataSerializer.serializeInventory(inventory.getContents()));

            // Set ender chest (again, if there is data)
            ItemStack[] enderChestData;
            if (!mpdbPlayerData.enderChestData.isEmpty() && !mpdbPlayerData.enderChestData.equalsIgnoreCase("none")) {
                enderChestData = mpdbConverter.getItemStackFromSerializedData(mpdbPlayerData.enderChestData);
            } else {
                enderChestData = new ItemStack[0];
            }
            playerData.setSerializedEnderChest(DataSerializer.serializeInventory(enderChestData));

            // Set experience
            playerData.setExpLevel(mpdbPlayerData.expLevel);
            playerData.setExpProgress(mpdbPlayerData.expProgress);
            playerData.setTotalExperience(mpdbPlayerData.totalExperience);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to convert MPDB data to HuskSync's format!");
            e.printStackTrace();
        }
        return playerData;
    }
}
