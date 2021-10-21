package me.william278.crossserversync.bukkit;

import me.william278.crossserversync.CrossServerSyncBukkit;
import me.william278.crossserversync.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.io.IOException;
import java.util.logging.Level;

public class PlayerSetter {

    private static final CrossServerSyncBukkit plugin = CrossServerSyncBukkit.getInstance();

    /**
     * Set a player from their PlayerData
     *
     * @param player The {@link Player} to set
     * @param data   The {@link PlayerData} to assign to the player
     */
    public static void setPlayerFrom(Player player, PlayerData data) {
        try {
            setPlayerInventory(player, DataSerializer.itemStackArrayFromBase64(data.getSerializedInventory()));
            setPlayerEnderChest(player, DataSerializer.itemStackArrayFromBase64(data.getSerializedEnderChest()));
            player.setHealth(data.getHealth());
            player.setMaxHealth(data.getMaxHealth());
            player.setFoodLevel(data.getHunger());
            player.setSaturation(data.getSaturation());
            player.setExhaustion(data.getSaturationExhaustion());
            player.getInventory().setHeldItemSlot(data.getSelectedSlot());
            player.setTotalExperience(data.getExperience());

            //todo potion effects not working
            setPlayerPotionEffects(player, DataSerializer.potionEffectArrayFromBase64(data.getSerializedEffectData()));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to deserialize PlayerData", e);
        }
    }

    /**
     * Sets a player's ender chest from a set of {@link ItemStack}s
     *
     * @param player The player to set the inventory of
     * @param items  The array of {@link ItemStack}s to set
     */
    private static void setPlayerEnderChest(Player player, ItemStack[] items) {
        player.getEnderChest().clear();
        int index = 0;
        for (ItemStack item : items) {
            if (item != null) {
                player.getEnderChest().setItem(index, item);
            }
            index++;
        }
    }

    /**
     * Sets a player's inventory from a set of {@link ItemStack}s
     *
     * @param player The player to set the inventory of
     * @param items  The array of {@link ItemStack}s to set
     */
    private static void setPlayerInventory(Player player, ItemStack[] items) {
        player.getInventory().clear();
        int index = 0;
        for (ItemStack item : items) {
            if (item != null) {
                player.getInventory().setItem(index, item);
            }
            index++;
        }
    }

    /**
     * Set a player's current potion effects from a set of {@link PotionEffect[]}
     * @param player The player to set the potion effects of
     * @param effects The array of {@link PotionEffect}s to set
     */
    private static void setPlayerPotionEffects(Player player, PotionEffect[] effects) {
        player.getActivePotionEffects().clear();
        for (PotionEffect effect : effects) {
            player.getActivePotionEffects().add(effect);
        }
    }
}
