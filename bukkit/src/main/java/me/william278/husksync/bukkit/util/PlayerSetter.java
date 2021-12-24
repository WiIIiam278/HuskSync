package me.william278.husksync.bukkit.util;

import me.william278.husksync.HuskSyncBukkit;
import me.william278.husksync.PlayerData;
import me.william278.husksync.Settings;
import me.william278.husksync.api.events.SyncCompleteEvent;
import me.william278.husksync.api.events.SyncEvent;
import me.william278.husksync.bukkit.data.DataSerializer;
import me.william278.husksync.bukkit.util.nms.AdvancementUtils;
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
import org.bukkit.potion.PotionEffectType;

import java.io.IOException;
import java.time.Instant;
import java.time.Period;
import java.util.*;
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
        final double maxHealth = getMaxHealth(player); // Get the player's max health (used to determine health as well)
        return RedisMessage.serialize(new PlayerData(player.getUniqueId(),
                DataSerializer.serializeInventory(player.getInventory().getContents()),
                DataSerializer.serializeInventory(player.getEnderChest().getContents()),
                Math.min(player.getHealth(), maxHealth),
                maxHealth,
                player.isHealthScaled() ? player.getHealthScale() : 0D,
                player.getFoodLevel(),
                player.getSaturation(),
                player.getExhaustion(),
                player.getInventory().getHeldItemSlot(),
                DataSerializer.serializePotionEffects(getPlayerPotionEffects(player)),
                player.getTotalExperience(),
                player.getLevel(),
                player.getExp(),
                player.getGameMode().toString(),
                DataSerializer.getSerializedStatisticData(player),
                player.isFlying(),
                DataSerializer.getSerializedAdvancements(player),
                DataSerializer.getSerializedLocation(player)));
    }

    /**
     * Returns a {@link Player}'s maximum health, minus any health boost effects
     *
     * @param player The {@link Player} to get the maximum health of
     * @return The {@link Player}'s max health
     */
    private static double getMaxHealth(Player player) {
        double maxHealth = Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue();

        // If the player has additional health bonuses from synchronised potion effects, subtract these from this number as they are synchronised seperately
        if (player.hasPotionEffect(PotionEffectType.HEALTH_BOOST) && maxHealth > 20D) {
            PotionEffect healthBoostEffect = player.getPotionEffect(PotionEffectType.HEALTH_BOOST);
            assert healthBoostEffect != null;
            double healthBoostBonus = 4 * (healthBoostEffect.getAmplifier() + 1);
            maxHealth -= healthBoostBonus;
        }
        return maxHealth;
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
                    new RedisMessage.MessageTarget(Settings.ServerType.PROXY, null, Settings.cluster),
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
                new RedisMessage.MessageTarget(Settings.ServerType.PROXY, null, Settings.cluster),
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
                    ArrayList<DataSerializer.AdvancementRecord> advancementRecords
                            = DataSerializer.deserializeAdvancementData(data.getSerializedAdvancements());

                    if (Settings.useNativeImplementation) {
                        try {
                            nativeSyncPlayerAdvancements(player, advancementRecords);
                        } catch (Exception e) {
                            plugin.getLogger().log(Level.WARNING,
                                    "Your server does not support a native implementation of achievements synchronization");
                            plugin.getLogger().log(Level.WARNING,
                                    "Your server version {0}. Please disable using native implementation!", Bukkit.getVersion());

                            Settings.useNativeImplementation = false;
                            setPlayerAdvancements(player, advancementRecords, data);
                            plugin.getLogger().log(Level.SEVERE, e.getMessage(), e);
                        }
                    } else {
                        setPlayerAdvancements(player, advancementRecords, data);
                    }
                }
                if (Settings.syncInventories) {
                    setPlayerInventory(player, DataSerializer.deserializeInventory(data.getSerializedInventory()));
                    player.getInventory().setHeldItemSlot(data.getSelectedSlot());
                }
                if (Settings.syncEnderChests) {
                    setPlayerEnderChest(player, DataSerializer.deserializeInventory(data.getSerializedEnderChest()));
                }
                if (Settings.syncHealth) {
                    setPlayerHealth(player, data.getHealth(), data.getMaxHealth(), data.getHealthScale());
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
                    setPlayerPotionEffects(player, DataSerializer.deserializePotionEffects(data.getSerializedEffectData()));
                }
                if (Settings.syncStatistics) {
                    setPlayerStatistics(player, DataSerializer.deserializeStatisticData(data.getSerializedStatistics()));
                }
                if (Settings.syncGameMode) {
                    player.setGameMode(GameMode.valueOf(data.getGameMode()));
                }
                if (Settings.syncLocation) {
                    setPlayerLocation(player, DataSerializer.deserializePlayerLocationData(data.getSerializedLocation()));
                }
                if (Settings.syncFlight) {
                    if (data.isFlying()) {
                        player.setAllowFlight(true);
                    }
                    player.setFlying(player.getAllowFlight() && data.isFlying());
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

    private static void nativeSyncPlayerAdvancements(final Player player, final List<DataSerializer.AdvancementRecord> advancementRecords) {
        final Object playerAdvancements = AdvancementUtils.getPlayerAdvancements(player);

        // Clear
        AdvancementUtils.clearPlayerAdvancementsMap(playerAdvancements);

        advancementRecords.forEach(advancementRecord -> {
            NamespacedKey namespacedKey = Objects.requireNonNull(
                    NamespacedKey.fromString(advancementRecord.advancementKey()),
                    "Invalid Namespaced key of " + advancementRecord.advancementKey()
            );

            Advancement bukkitAdvancement = Bukkit.getAdvancement(namespacedKey);
            if (bukkitAdvancement == null) {
                plugin.getLogger().log(Level.WARNING, "Ignored advancement '{0}' - it doesn't exist anymore?", namespacedKey);
                return;
            }

            // todo: sync date of get advancement
            Date date = Date.from(Instant.now().minus(Period.ofWeeks(1)));

            Object advancement = AdvancementUtils.getHandle(bukkitAdvancement);
            List<String> criteriaList = advancementRecord.awardedAdvancementCriteria();
            {
                Map<String, Object> nativeCriteriaMap = new HashMap<>();
                criteriaList.forEach(criteria ->
                        nativeCriteriaMap.put(criteria, AdvancementUtils.newCriterionProgress(date))
                );
                Object nativeAdvancementProgress = AdvancementUtils.newAdvancementProgress(nativeCriteriaMap);

                AdvancementUtils.startProgress(playerAdvancements, advancement, nativeAdvancementProgress);

            }
        });

        AdvancementUtils.markPlayerAdvancementsFirst(playerAdvancements);
        AdvancementUtils.ensureAllVisible(playerAdvancements);
    }

    /**
     * Update a player's advancements and progress to match the advancementData
     *
     * @param player          The player to set the advancements of
     * @param advancementData The ArrayList of {@link DataSerializer.AdvancementRecord}s to set
     */
    private static void setPlayerAdvancements(Player player, ArrayList<DataSerializer.AdvancementRecord> advancementData, PlayerData data) {
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
                for (DataSerializer.AdvancementRecord record : advancementData) {
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

    /**
     * Correctly set a {@link Player}'s health data
     *
     * @param player      The {@link Player} to set
     * @param health      Health to set to the player
     * @param maxHealth   Max health to set to the player
     * @param healthScale Health scaling to apply to the player
     */
    private static void setPlayerHealth(Player player, double health, double maxHealth, double healthScale) {
        // Set max health
        if (maxHealth != 0.0D) {
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(maxHealth);
        }

        // Set health
        player.setHealth(player.getHealth() > maxHealth ? maxHealth : health);

        // Set health scaling if needed
        if (healthScale != 0D) {
            player.setHealthScale(healthScale);
        } else {
            player.setHealthScale(maxHealth);
        }
        player.setHealthScaled(healthScale != 0D);
    }
}
