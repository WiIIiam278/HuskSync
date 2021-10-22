package me.william278.husksync.bukkit;

import de.themoep.minedown.MineDown;
import me.william278.husksync.HuskSyncBukkit;
import me.william278.husksync.MessageStrings;
import me.william278.husksync.PlayerData;
import me.william278.husksync.Settings;
import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.logging.Level;

public class PlayerSetter {

    private static final HuskSyncBukkit plugin = HuskSyncBukkit.getInstance();

    /**
     * Set a player from their PlayerData, based on settings
     *
     * @param player The {@link Player} to set
     * @param data   The {@link PlayerData} to assign to the player
     */
    public static void setPlayerFrom(Player player, PlayerData data) {
        // If the data is flagged as being default data, skip setting
        if (data.isUseDefaultData()) {
            return;
        }

        // Set the player's data from the PlayerData
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (Settings.syncAdvancements) {
                    // Sync advancements first so that any rewards will be overridden
                    setPlayerAdvancements(player, DataSerializer.deserializeAdvancementData(data.getSerializedAdvancements()));
                }
                if (Settings.syncInventories) {
                    setPlayerInventory(player, DataSerializer.itemStackArrayFromBase64(data.getSerializedInventory()));
                    player.getInventory().setHeldItemSlot(data.getSelectedSlot());
                }
                if (Settings.syncEnderChests) {
                    setPlayerEnderChest(player, DataSerializer.itemStackArrayFromBase64(data.getSerializedEnderChest()));
                }
                if (Settings.syncHealth) {
                    Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(data.getMaxHealth());
                    player.setHealth(data.getHealth());
                }
                if (Settings.syncHunger) {
                    player.setFoodLevel(data.getHunger());
                    player.setSaturation(data.getSaturation());
                    player.setExhaustion(data.getSaturationExhaustion());
                }
                if (Settings.syncExperience) {
                    player.setTotalExperience(data.getTotalExperience());
                    player.setLevel(data.getExpLevel());
                    player.setExp(data.getExpProgress());
                }
                if (Settings.syncPotionEffects) {
                    setPlayerPotionEffects(player, DataSerializer.potionEffectArrayFromBase64(data.getSerializedEffectData()));
                }
                if (Settings.syncStatistics) {
                    setPlayerStatistics(player, DataSerializer.deserializeStatisticData(data.getSerializedStatistics()));
                }
                if (Settings.syncGameMode) {
                    player.setGameMode(GameMode.valueOf(data.getGameMode()));
                }
                if (Settings.syncLocation) {
                    player.setFlying(player.getAllowFlight() && data.isFlying());
                    setPlayerLocation(player, DataSerializer.deserializePlayerLocationData(data.getSerializedLocation()));
                }

                // Send action bar synchronisation message
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new MineDown(MessageStrings.SYNCHRONISATION_COMPLETE).toComponent());
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to deserialize PlayerData", e);
            }
        });
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
     *
     * @param player  The player to set the potion effects of
     * @param effects The array of {@link PotionEffect}s to set
     */
    private static void setPlayerPotionEffects(Player player, PotionEffect[] effects) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        for (PotionEffect effect : effects) {
            player.addPotionEffect(effect);
        }
    }

    /**
     * Update a player's advancements and progress to match the advancementData
     *
     * @param player          The player to set the advancements of
     * @param advancementData The ArrayList of {@link DataSerializer.AdvancementRecord}s to set
     */
    private static void setPlayerAdvancements(Player player, ArrayList<DataSerializer.AdvancementRecord> advancementData) {
        // Temporarily disable advancement announcing if needed
        boolean announceAdvancementUpdate = false;
        if (Boolean.TRUE.equals(player.getWorld().getGameRuleValue(GameRule.ANNOUNCE_ADVANCEMENTS))) {
            player.getWorld().setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
            announceAdvancementUpdate = true;
        }
        final boolean finalAnnounceAdvancementUpdate = announceAdvancementUpdate;

        // Run async because advancement loading is very slow
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Apply the advancements to the player
            Iterator<Advancement> serverAdvancements = Bukkit.getServer().advancementIterator();
            while (serverAdvancements.hasNext()) {
                Advancement advancement = serverAdvancements.next();
                AdvancementProgress playerProgress = player.getAdvancementProgress(advancement);
                boolean hasAdvancement = false;
                for (DataSerializer.AdvancementRecord record : advancementData) {
                    if (record.advancementKey().equals(advancement.getKey().getNamespace() + ":" + advancement.getKey().getKey())) {
                        hasAdvancement = true;

                        // Save the experience before granting the advancement
                        final int expLevel = player.getLevel();
                        final float expProgress = player.getExp();

                        // Grant advancement criteria if the player does not have it
                        for (String awardCriteria : record.awardedAdvancementCriteria()) {
                            if (!playerProgress.getAwardedCriteria().contains(awardCriteria)) {
                                Bukkit.getScheduler().runTask(plugin, () -> player.getAdvancementProgress(advancement).awardCriteria(awardCriteria));
                            }
                        }

                        // Set experience back to before granting advancement; nullify exp gained from it
                        player.setLevel(expLevel);
                        player.setExp(expProgress);
                        break;
                    }
                }
                if (!hasAdvancement) {
                    for (String awardCriteria : playerProgress.getAwardedCriteria()) {
                        Bukkit.getScheduler().runTask(plugin, () -> player.getAdvancementProgress(advancement).revokeCriteria(awardCriteria));
                    }
                }
            }

            // Re-enable announcing advancements (back on main thread again)
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (finalAnnounceAdvancementUpdate) {
                    player.getWorld().setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, true);
                }
            });
        });
    }

    /**
     * Set a player's statistics (in the Statistic menu)
     *
     * @param player        The player to set the statistics of
     * @param statisticData The {@link DataSerializer.StatisticData} to set
     */
    private static void setPlayerStatistics(Player player, DataSerializer.StatisticData statisticData) {
        // Set untyped statistics
        for (Statistic statistic : statisticData.untypedStatisticValues().keySet()) {
            player.setStatistic(statistic, statisticData.untypedStatisticValues().get(statistic));
        }

        // Set block statistics
        for (Statistic statistic : statisticData.blockStatisticValues().keySet()) {
            for (Material blockMaterial : statisticData.blockStatisticValues().get(statistic).keySet()) {
                player.setStatistic(statistic, blockMaterial, statisticData.blockStatisticValues().get(statistic).get(blockMaterial));
            }
        }

        // Set item statistics
        for (Statistic statistic : statisticData.itemStatisticValues().keySet()) {
            for (Material itemMaterial : statisticData.itemStatisticValues().get(statistic).keySet()) {
                player.setStatistic(statistic, itemMaterial, statisticData.itemStatisticValues().get(statistic).get(itemMaterial));
            }
        }

        // Set entity statistics
        for (Statistic statistic : statisticData.entityStatisticValues().keySet()) {
            for (EntityType entityType : statisticData.entityStatisticValues().get(statistic).keySet()) {
                player.setStatistic(statistic, entityType, statisticData.entityStatisticValues().get(statistic).get(entityType));
            }
        }
    }

    /**
     * Set a player's location from {@link DataSerializer.PlayerLocation} data
     *
     * @param player   The {@link Player} to teleport
     * @param location The {@link DataSerializer.PlayerLocation}
     */
    private static void setPlayerLocation(Player player, DataSerializer.PlayerLocation location) {
        // Don't teleport if the location is invalid
        if (location == null) {
            return;
        }

        // Determine the world; if the names match, use that
        World world = Bukkit.getWorld(location.worldName());
        if (world == null) {

            // If the names don't match, find the corresponding world with the same dimension environment
            for (World worldOnServer : Bukkit.getWorlds()) {
                if (worldOnServer.getEnvironment().equals(location.environment())) {
                    world = worldOnServer;
                }
            }

            // If that still fails, return
            if (world == null) {
                return;
            }
        }

        // Teleport the player
        player.teleport(new Location(world, location.x(), location.y(), location.z(), location.yaw(), location.pitch()));
    }
}
