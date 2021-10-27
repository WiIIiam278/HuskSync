package me.william278.husksync.bukkit.util;

import me.william278.husksync.HuskSyncBukkit;
import me.william278.husksync.PlayerData;
import me.william278.husksync.Settings;
import me.william278.husksync.api.events.SyncCompleteEvent;
import me.william278.husksync.api.events.SyncEvent;
import me.william278.husksync.bukkit.data.PlayerSerializer;
import me.william278.husksync.redis.RedisMessage;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerSetter {

    private static final HuskSyncBukkit plugin = HuskSyncBukkit.getInstance();

    /**
     * Returns the new serialized PlayerData for a player.
     *
     * @param player The {@link Player} to get the new serialized PlayerData for
     * @return The {@link PlayerData}, serialized as a {@link String}
     * @throws IOException If the serialization fails
     */
    private static String getNewSerializedPlayerData(Player player) throws IOException {
        return RedisMessage.serialize(new PlayerData(player.getUniqueId(),
                PlayerSerializer.serializeInventory(player.getInventory().getContents()),
                PlayerSerializer.serializeInventory(player.getEnderChest().getContents()),
                player.getHealth(),
                Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue(),
                player.getHealthScale(),
                player.getFoodLevel(),
                player.getSaturation(),
                player.getExhaustion(),
                player.getInventory().getHeldItemSlot(),
                PlayerSerializer.serializePotionEffects(getPlayerPotionEffects(player)),
                player.getTotalExperience(),
                player.getLevel(),
                player.getExp(),
                player.getGameMode().toString(),
                PlayerSerializer.getSerializedStatisticData(player),
                player.isFlying(),
                PlayerSerializer.getSerializedAdvancements(player),
                PlayerSerializer.getSerializedLocation(player)));
    }

    /**
     * Returns a {@link Player}'s active potion effects in a {@link PotionEffect} array
     *
     * @param player The {@link Player} to get the effects of
     * @return The {@link PotionEffect} array
     */
    private static PotionEffect[] getPlayerPotionEffects(Player player) {
        PotionEffect[] potionEffects = new PotionEffect[player.getActivePotionEffects().size()];
        int arrayIndex = 0;
        for (PotionEffect effect : player.getActivePotionEffects()) {
            potionEffects[arrayIndex] = effect;
            arrayIndex++;
        }
        return potionEffects;
    }

    /**
     * Update a {@link Player}'s data, sending it to the proxy
     *
     * @param player {@link Player} to send data to proxy
     */
    public static void updatePlayerData(Player player) {
        // Send a redis message with the player's last updated PlayerData version UUID and their new PlayerData
        try {
            final String serializedPlayerData = getNewSerializedPlayerData(player);
            new RedisMessage(RedisMessage.MessageType.PLAYER_DATA_UPDATE,
                    new RedisMessage.MessageTarget(Settings.ServerType.BUNGEECORD, null),
                    serializedPlayerData).send();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to send a PlayerData update to the proxy", e);
        }

        // Clear player inventory and ender chest
        player.getInventory().clear();
        player.getEnderChest().clear();
    }

    /**
     * Request a {@link Player}'s data from the proxy
     *
     * @param playerUUID The {@link UUID} of the {@link Player} to fetch PlayerData from
     * @throws IOException If the request Redis message data fails to serialize
     */
    public static void requestPlayerData(UUID playerUUID) throws IOException {
        new RedisMessage(RedisMessage.MessageType.PLAYER_DATA_REQUEST,
                new RedisMessage.MessageTarget(Settings.ServerType.BUNGEECORD, null),
                playerUUID.toString()).send();
    }

    /**
     * Set a player from their PlayerData, based on settings
     *
     * @param player    The {@link Player} to set
     * @param dataToSet The {@link PlayerData} to assign to the player
     */
    public static void setPlayerFrom(Player player, PlayerData dataToSet) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            // Handle the SyncEvent
            SyncEvent syncEvent = new SyncEvent(player, dataToSet);
            Bukkit.getPluginManager().callEvent(syncEvent);
            final PlayerData data = syncEvent.getData();
            if (syncEvent.isCancelled()) {
                return;
            }

            // If the data is flagged as being default data, skip setting
            if (data.isUseDefaultData()) {
                HuskSyncBukkit.bukkitCache.removeAwaitingDataFetch(player.getUniqueId());
                return;
            }

            // Clear player
            player.getInventory().clear();
            player.getEnderChest().clear();
            player.setExp(0);
            player.setLevel(0);

            HuskSyncBukkit.bukkitCache.removeAwaitingDataFetch(player.getUniqueId());

            // Set the player's data from the PlayerData
            try {
                if (Settings.syncAdvancements) {
                    setPlayerAdvancements(player, PlayerSerializer.deserializeAdvancementData(data.getSerializedAdvancements()), data);
                }
                if (Settings.syncInventories) {
                    setPlayerInventory(player, PlayerSerializer.deserializeInventory(data.getSerializedInventory()));
                    player.getInventory().setHeldItemSlot(data.getSelectedSlot());
                }
                if (Settings.syncEnderChests) {
                    setPlayerEnderChest(player, PlayerSerializer.deserializeInventory(data.getSerializedEnderChest()));
                }
                if (Settings.syncHealth) {
                    player.setHealthScale(data.getHealthScale() <= 0 ? data.getHealthScale() : 20D);
                    Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(data.getMaxHealth());
                    player.setHealth(data.getHealth());
                }
                if (Settings.syncHunger) {
                    player.setFoodLevel(data.getHunger());
                    player.setSaturation(data.getSaturation());
                    player.setExhaustion(data.getSaturationExhaustion());
                }
                if (Settings.syncExperience) {
                    // This is also handled when syncing advancements to ensure its correct
                    setPlayerExperience(player, data);
                }
                if (Settings.syncPotionEffects) {
                    setPlayerPotionEffects(player, PlayerSerializer.deserializePotionEffects(data.getSerializedEffectData()));
                }
                if (Settings.syncStatistics) {
                    setPlayerStatistics(player, PlayerSerializer.deserializeStatisticData(data.getSerializedStatistics()));
                }
                if (Settings.syncGameMode) {
                    player.setGameMode(GameMode.valueOf(data.getGameMode()));
                }
                if (Settings.syncLocation) {
                    player.setFlying(player.getAllowFlight() && data.isFlying());
                    setPlayerLocation(player, PlayerSerializer.deserializePlayerLocationData(data.getSerializedLocation()));
                }

                // Handle the SyncCompleteEvent
                Bukkit.getPluginManager().callEvent(new SyncCompleteEvent(player, data));
            } catch (IOException | ClassNotFoundException e) {
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
        setInventory(player.getEnderChest(), items);
    }

    /**
     * Sets a player's inventory from a set of {@link ItemStack}s
     *
     * @param player The player to set the inventory of
     * @param items  The array of {@link ItemStack}s to set
     */
    private static void setPlayerInventory(Player player, ItemStack[] items) {
        setInventory(player.getInventory(), items);
    }

    /**
     * Sets an inventory's contents from an array of {@link ItemStack}s
     *
     * @param inventory The inventory to set
     * @param items     The {@link ItemStack}s to fill it with
     */
    public static void setInventory(Inventory inventory, ItemStack[] items) {
        inventory.clear();
        int index = 0;
        for (ItemStack item : items) {
            if (item != null) {
                inventory.setItem(index, item);
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
     * @param advancementData The ArrayList of {@link PlayerSerializer.AdvancementRecord}s to set
     */
    private static void setPlayerAdvancements(Player player, ArrayList<PlayerSerializer.AdvancementRecord> advancementData, PlayerData data) {
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
            while (serverAdvancements.hasNext()) { // Iterate through all advancements
                boolean correctExperienceCheck = false; // Determines whether the experience might have changed warranting an update
                Advancement advancement = serverAdvancements.next();
                AdvancementProgress playerProgress = player.getAdvancementProgress(advancement);
                for (PlayerSerializer.AdvancementRecord record : advancementData) {
                    // If the advancement is one on the data
                    if (record.advancementKey().equals(advancement.getKey().getNamespace() + ":" + advancement.getKey().getKey())) {

                        // Award all criteria that the player does not have that they do on the cache
                        ArrayList<String> currentlyAwardedCriteria = new ArrayList<>(playerProgress.getAwardedCriteria());
                        for (String awardCriteria : record.awardedAdvancementCriteria()) {
                            if (!playerProgress.getAwardedCriteria().contains(awardCriteria)) {
                                Bukkit.getScheduler().runTask(plugin, () -> player.getAdvancementProgress(advancement).awardCriteria(awardCriteria));
                                correctExperienceCheck = true;
                            }
                            currentlyAwardedCriteria.remove(awardCriteria);
                        }

                        // Revoke all criteria that the player does have but should not
                        for (String awardCriteria : currentlyAwardedCriteria) {
                            Bukkit.getScheduler().runTask(plugin, () -> player.getAdvancementProgress(advancement).revokeCriteria(awardCriteria));
                        }
                        break;
                    }
                }

                // Update the player's experience in case the advancement changed that
                if (correctExperienceCheck) {
                    if (Settings.syncExperience) {
                        setPlayerExperience(player, data);
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
     * @param statisticData The {@link PlayerSerializer.StatisticData} to set
     */
    private static void setPlayerStatistics(Player player, PlayerSerializer.StatisticData statisticData) {
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
     * Set a player's exp level, exp points & score
     *
     * @param player The {@link Player} to set
     * @param data   The {@link PlayerData} to set them
     */
    private static void setPlayerExperience(Player player, PlayerData data) {
        player.setTotalExperience(data.getTotalExperience());
        player.setLevel(data.getExpLevel());
        player.setExp(data.getExpProgress());
    }

    /**
     * Set a player's location from {@link PlayerSerializer.PlayerLocation} data
     *
     * @param player   The {@link Player} to teleport
     * @param location The {@link PlayerSerializer.PlayerLocation}
     */
    private static void setPlayerLocation(Player player, PlayerSerializer.PlayerLocation location) {
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
