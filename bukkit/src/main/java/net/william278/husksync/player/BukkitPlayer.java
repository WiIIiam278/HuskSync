/*
 * This file is part of HuskSync, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.husksync.player;

import de.themoep.minedown.adventure.MineDown;
import dev.triumphteam.gui.builder.gui.StorageBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.StorageGui;
import net.kyori.adventure.audience.Audience;
import net.roxeez.advancement.display.FrameType;
import net.william278.andjam.Toast;
import net.william278.desertwell.util.Version;
import net.william278.husksync.BukkitHuskSync;
import net.william278.husksync.config.Settings;
import net.william278.husksync.data.*;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * Bukkit implementation of an {@link OnlineUser}
 */
public class BukkitPlayer extends OnlineUser {

    private final BukkitHuskSync plugin;
    private final Player player;

    private BukkitPlayer(@NotNull Player player) {
        super(player.getUniqueId(), player.getName());
        this.plugin = BukkitHuskSync.getInstance();
        this.player = player;
    }

    @NotNull
    public static BukkitPlayer adapt(@NotNull Player player) {
        return new BukkitPlayer(player);
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public CompletableFuture<StatusData> getStatus() {
        return CompletableFuture.supplyAsync(() -> {
            final double maxHealth = getMaxHealth(player);
            return new StatusData(Math.min(player.getHealth(), maxHealth),
                    maxHealth,
                    player.isHealthScaled() ? player.getHealthScale() : 0d,
                    player.getFoodLevel(),
                    player.getSaturation(),
                    player.getExhaustion(),
                    player.getInventory().getHeldItemSlot(),
                    player.getTotalExperience(),
                    player.getLevel(),
                    player.getExp(),
                    player.getGameMode().name(),
                    player.getAllowFlight() && player.isFlying());
        });
    }

    @Override
    public CompletableFuture<Void> setStatus(@NotNull StatusData statusData, @NotNull Settings settings) {
        return CompletableFuture.runAsync(() -> {
            // Set max health
            double currentMaxHealth = Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue();
            if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.MAX_HEALTH)) {
                if (statusData.maxHealth != 0d) {
                    Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH))
                            .setBaseValue(statusData.maxHealth);
                    currentMaxHealth = statusData.maxHealth;
                }
            }
            if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.HEALTH)) {
                // Set health
                final double currentHealth = player.getHealth();
                if (statusData.health != currentHealth) {
                    final double healthToSet = currentHealth > currentMaxHealth ? currentMaxHealth : statusData.health;
                    final double maxHealth = currentMaxHealth;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            player.setHealth(Math.min(healthToSet, maxHealth));
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().log(Level.WARNING,
                                    "Failed to set health of player " + player.getName() + " to " + healthToSet);
                        }
                    });
                }

                // Set health scale
                try {
                    if (statusData.healthScale != 0d) {
                        player.setHealthScale(statusData.healthScale);
                    } else {
                        player.setHealthScale(statusData.maxHealth);
                    }
                    player.setHealthScaled(statusData.healthScale != 0D);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().log(Level.WARNING,
                            "Failed to set health scale of player " + player.getName() + " to " + statusData.healthScale);
                }
            }
            if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.HUNGER)) {
                player.setFoodLevel(statusData.hunger);
                player.setSaturation(statusData.saturation);
                player.setExhaustion(statusData.saturationExhaustion);
            }
            if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.INVENTORIES)) {
                player.getInventory().setHeldItemSlot(statusData.selectedItemSlot);
            }
            if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.EXPERIENCE)) {
                player.setTotalExperience(statusData.totalExperience);
                player.setLevel(statusData.expLevel);
                player.setExp(statusData.expProgress);
            }
            if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.GAME_MODE)) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.setGameMode(GameMode.valueOf(statusData.gameMode)));
            }
            if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.LOCATION)) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (statusData.isFlying) {
                        player.setAllowFlight(true);
                        player.setFlying(true);
                    }
                    player.setFlying(false);
                });
            }
        });
    }

    @Override
    public CompletableFuture<ItemData> getInventory() {
        final PlayerInventory inventory = player.getInventory();
        if (inventory.isEmpty()) {
            return CompletableFuture.completedFuture(ItemData.empty());
        }
        return BukkitSerializer.serializeItemStackArray(inventory.getContents())
                .thenApply(ItemData::new);
    }

    @Override
    public CompletableFuture<Void> setInventory(@NotNull ItemData itemData) {
        return BukkitSerializer.deserializeInventory(itemData.serializedItems).thenApplyAsync(contents -> {
            final CompletableFuture<Void> inventorySetFuture = new CompletableFuture<>();
            Bukkit.getScheduler().runTask(plugin, () -> {
                this.clearInventoryCraftingSlots();
                player.setItemOnCursor(null);
                player.getInventory().setContents(contents.getContents());
                player.updateInventory();
                inventorySetFuture.complete(null);
            });
            return inventorySetFuture.join();
        });
    }

    // Clears any items the player may have in the crafting slots of their inventory
    private void clearInventoryCraftingSlots() {
        final Inventory inventory = player.getOpenInventory().getTopInventory();
        if (inventory.getType() == InventoryType.CRAFTING) {
            for (int slot = 0; slot < 5; slot++) {
                inventory.setItem(slot, null);
            }
        }
    }

    @Override
    public CompletableFuture<ItemData> getEnderChest() {
        final Inventory enderChest = player.getEnderChest();
        if (enderChest.isEmpty()) {
            return CompletableFuture.completedFuture(ItemData.empty());
        }
        return BukkitSerializer.serializeItemStackArray(enderChest.getContents())
                .thenApply(ItemData::new);
    }

    @Override
    public CompletableFuture<Void> setEnderChest(@NotNull ItemData enderChestData) {
        return BukkitSerializer.deserializeItemStackArray(enderChestData.serializedItems).thenApplyAsync(contents -> {
            final CompletableFuture<Void> enderChestSetFuture = new CompletableFuture<>();
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.getEnderChest().setContents(contents);
                enderChestSetFuture.complete(null);
            });
            return enderChestSetFuture.join();
        });
    }

    @Override
    public CompletableFuture<PotionEffectData> getPotionEffects() {
        return BukkitSerializer.serializePotionEffectArray(player.getActivePotionEffects()
                .toArray(new PotionEffect[0])).thenApply(PotionEffectData::new);
    }

    @Override
    public CompletableFuture<Void> setPotionEffects(@NotNull PotionEffectData potionEffectData) {
        return BukkitSerializer.deserializePotionEffectArray(potionEffectData.serializedPotionEffects)
                .thenApplyAsync(effects -> {
                    final CompletableFuture<Void> potionEffectsSetFuture = new CompletableFuture<>();
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        for (PotionEffect effect : player.getActivePotionEffects()) {
                            player.removePotionEffect(effect.getType());
                        }
                        for (PotionEffect effect : effects) {
                            player.addPotionEffect(effect);
                        }
                        potionEffectsSetFuture.complete(null);
                    });
                    return potionEffectsSetFuture.join();
                });
    }

    @Override
    public CompletableFuture<List<AdvancementData>> getAdvancements() {
        return CompletableFuture.supplyAsync(() -> {
            final Iterator<Advancement> serverAdvancements = Bukkit.getServer().advancementIterator();
            final ArrayList<AdvancementData> advancementData = new ArrayList<>();

            // Iterate through the server advancement set and add all advancements to the list
            serverAdvancements.forEachRemaining(advancement -> {
                final AdvancementProgress advancementProgress = player.getAdvancementProgress(advancement);
                final Map<String, Date> awardedCriteria = new HashMap<>();

                advancementProgress.getAwardedCriteria().forEach(criteriaKey -> awardedCriteria.put(criteriaKey,
                        advancementProgress.getDateAwarded(criteriaKey)));

                // Only save the advancement if criteria has been completed
                if (!awardedCriteria.isEmpty()) {
                    advancementData.add(new AdvancementData(advancement.getKey().toString(), awardedCriteria));
                }
            });
            return advancementData;
        });
    }

    @Override
    public CompletableFuture<Void> setAdvancements(@NotNull List<AdvancementData> advancementData) {
        return CompletableFuture.runAsync(() -> Bukkit.getScheduler().runTask(plugin, () -> {

            // Temporarily disable advancement announcing if needed
            boolean announceAdvancementUpdate = false;
            if (Boolean.TRUE.equals(player.getWorld().getGameRuleValue(GameRule.ANNOUNCE_ADVANCEMENTS))) {
                player.getWorld().setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
                announceAdvancementUpdate = true;
            }
            final boolean finalAnnounceAdvancementUpdate = announceAdvancementUpdate;

            // Save current experience and level
            final int experienceLevel = player.getLevel();
            final float expProgress = player.getExp();

            // Determines whether the experience might have changed warranting an update
            final AtomicBoolean correctExperience = new AtomicBoolean(false);

            // Run asynchronously as advancement setting is expensive
            CompletableFuture.runAsync(() -> {
                // Apply the advancements to the player
                final Iterator<Advancement> serverAdvancements = Bukkit.getServer().advancementIterator();
                while (serverAdvancements.hasNext()) {
                    // Iterate through all advancements
                    final Advancement advancement = serverAdvancements.next();
                    final AdvancementProgress playerProgress = player.getAdvancementProgress(advancement);

                    advancementData.stream().filter(record -> record.key.equals(advancement.getKey().toString())).findFirst().ifPresentOrElse(
                            // Award all criteria that the player does not have that they do on the cache
                            record -> {
                                record.completedCriteria.keySet().stream()
                                        .filter(criterion -> !playerProgress.getAwardedCriteria().contains(criterion))
                                        .forEach(criterion -> {
                                            Bukkit.getScheduler().runTask(plugin,
                                                    () -> player.getAdvancementProgress(advancement).awardCriteria(criterion));
                                            correctExperience.set(true);
                                        });

                                // Revoke all criteria that the player does have but should not
                                new ArrayList<>(playerProgress.getAwardedCriteria()).stream().filter(criterion -> !record.completedCriteria.containsKey(criterion))
                                        .forEach(criterion -> Bukkit.getScheduler().runTask(plugin,
                                                () -> player.getAdvancementProgress(advancement).revokeCriteria(criterion)));

                            },
                            // Revoke the criteria as the player shouldn't have any
                            () -> new ArrayList<>(playerProgress.getAwardedCriteria()).forEach(criterion ->
                                    Bukkit.getScheduler().runTask(plugin,
                                            () -> player.getAdvancementProgress(advancement).revokeCriteria(criterion))));

                    // Update the player's experience in case the advancement changed that
                    if (correctExperience.get()) {
                        player.setLevel(experienceLevel);
                        player.setExp(expProgress);
                        correctExperience.set(false);
                    }
                }

                // Re-enable announcing advancements (back on main thread again)
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (finalAnnounceAdvancementUpdate) {
                        player.getWorld().setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, true);
                    }
                });
            });
        }));
    }

    @Override
    public CompletableFuture<StatisticsData> getStatistics() {
        return CompletableFuture.supplyAsync(() -> {
            final Map<String, Integer> untypedStatisticValues = new HashMap<>();
            final Map<String, Map<String, Integer>> blockStatisticValues = new HashMap<>();
            final Map<String, Map<String, Integer>> itemStatisticValues = new HashMap<>();
            final Map<String, Map<String, Integer>> entityStatisticValues = new HashMap<>();

            for (Statistic statistic : Statistic.values()) {
                switch (statistic.getType()) {
                    case ITEM -> {
                        final Map<String, Integer> itemValues = new HashMap<>();
                        Arrays.stream(Material.values()).filter(Material::isItem)
                                .filter(itemMaterial -> (player.getStatistic(statistic, itemMaterial)) != 0)
                                .forEach(itemMaterial -> itemValues.put(itemMaterial.name(),
                                        player.getStatistic(statistic, itemMaterial)));
                        if (!itemValues.isEmpty()) {
                            itemStatisticValues.put(statistic.name(), itemValues);
                        }
                    }
                    case BLOCK -> {
                        final Map<String, Integer> blockValues = new HashMap<>();
                        Arrays.stream(Material.values()).filter(Material::isBlock)
                                .filter(blockMaterial -> (player.getStatistic(statistic, blockMaterial)) != 0)
                                .forEach(blockMaterial -> blockValues.put(blockMaterial.name(),
                                        player.getStatistic(statistic, blockMaterial)));
                        if (!blockValues.isEmpty()) {
                            blockStatisticValues.put(statistic.name(), blockValues);
                        }
                    }
                    case ENTITY -> {
                        final Map<String, Integer> entityValues = new HashMap<>();
                        Arrays.stream(EntityType.values()).filter(EntityType::isAlive)
                                .filter(entityType -> (player.getStatistic(statistic, entityType)) != 0)
                                .forEach(entityType -> entityValues.put(entityType.name(),
                                        player.getStatistic(statistic, entityType)));
                        if (!entityValues.isEmpty()) {
                            entityStatisticValues.put(statistic.name(), entityValues);
                        }
                    }
                    case UNTYPED -> {
                        if (player.getStatistic(statistic) != 0) {
                            untypedStatisticValues.put(statistic.name(), player.getStatistic(statistic));
                        }
                    }
                }
            }

            return new StatisticsData(untypedStatisticValues, blockStatisticValues,
                    itemStatisticValues, entityStatisticValues);
        });
    }

    @Override
    public CompletableFuture<Void> setStatistics(@NotNull StatisticsData statisticsData) {
        return CompletableFuture.runAsync(() -> {
            // Set generic statistics
            for (String statistic : statisticsData.untypedStatistics.keySet()) {
                try {
                    player.setStatistic(Statistic.valueOf(statistic), statisticsData.untypedStatistics.get(statistic));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().log(Level.WARNING,
                            "Failed to set generic statistic " + statistic + " for " + username);
                }
            }

            // Set block statistics
            for (String statistic : statisticsData.blockStatistics.keySet()) {
                for (String blockMaterial : statisticsData.blockStatistics.get(statistic).keySet()) {
                    try {
                        player.setStatistic(Statistic.valueOf(statistic), Material.valueOf(blockMaterial),
                                statisticsData.blockStatistics.get(statistic).get(blockMaterial));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().log(Level.WARNING,
                                "Failed to set " + blockMaterial + " statistic " + statistic + " for " + username);
                    }
                }
            }

            // Set item statistics
            for (String statistic : statisticsData.itemStatistics.keySet()) {
                for (String itemMaterial : statisticsData.itemStatistics.get(statistic).keySet()) {
                    try {
                        player.setStatistic(Statistic.valueOf(statistic), Material.valueOf(itemMaterial),
                                statisticsData.itemStatistics.get(statistic).get(itemMaterial));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().log(Level.WARNING,
                                "Failed to set " + itemMaterial + " statistic " + statistic + " for " + username);
                    }
                }
            }

            // Set entity statistics
            for (String statistic : statisticsData.entityStatistics.keySet()) {
                for (String entityType : statisticsData.entityStatistics.get(statistic).keySet()) {
                    try {
                        player.setStatistic(Statistic.valueOf(statistic), EntityType.valueOf(entityType),
                                statisticsData.entityStatistics.get(statistic).get(entityType));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().log(Level.WARNING,
                                "Failed to set " + entityType + " statistic " + statistic + " for " + username);
                    }
                }
            }
        });
    }

    @Override
    public CompletableFuture<LocationData> getLocation() {
        return CompletableFuture.supplyAsync(() ->
                new LocationData(player.getWorld().getName(), player.getWorld().getUID(), player.getWorld().getEnvironment().name(),
                        player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(),
                        player.getLocation().getYaw(), player.getLocation().getPitch()));
    }

    @Override
    public CompletableFuture<Void> setLocation(@NotNull LocationData locationData) {
        final CompletableFuture<Void> teleportFuture = new CompletableFuture<>();
        AtomicReference<World> bukkitWorld = new AtomicReference<>(Bukkit.getWorld(locationData.worldName));
        if (bukkitWorld.get() == null) {
            bukkitWorld.set(Bukkit.getWorld(locationData.worldUuid));
        }
        if (bukkitWorld.get() == null) {
            Bukkit.getWorlds().stream().filter(world -> world.getEnvironment() == World.Environment
                    .valueOf(locationData.worldEnvironment)).findFirst().ifPresent(bukkitWorld::set);
        }
        if (bukkitWorld.get() != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.teleport(new Location(bukkitWorld.get(),
                        locationData.x, locationData.y, locationData.z,
                        locationData.yaw, locationData.pitch), PlayerTeleportEvent.TeleportCause.PLUGIN);
                teleportFuture.complete(null);
            });
        }
        return teleportFuture;
    }

    @Override
    public CompletableFuture<PersistentDataContainerData> getPersistentDataContainer() {
        final Map<String, PersistentDataTag<?>> persistentDataMap = new HashMap<>();
        final PersistentDataContainer container = player.getPersistentDataContainer();
        return CompletableFuture.supplyAsync(() -> {
            container.getKeys().forEach(key -> {
                BukkitPersistentTypeMapping<?, ?> type = null;
                for (BukkitPersistentTypeMapping<?, ?> dataType : BukkitPersistentTypeMapping.PRIMITIVE_TYPE_MAPPINGS) {
                    if (container.has(key, dataType.bukkitType())) {
                        type = dataType;
                        break;
                    }
                }
                if (type != null) {
                    persistentDataMap.put(key.toString(), type.getContainerValue(container, key));
                }
            });
            return new PersistentDataContainerData(persistentDataMap);
        }).exceptionally(throwable -> {
            plugin.log(Level.WARNING,
                    "Could not read " + player.getName() + "'s persistent data map, skipping!");
            throwable.printStackTrace();
            return new PersistentDataContainerData(new HashMap<>());
        });
    }

    @Override
    public CompletableFuture<Void> setPersistentDataContainer(@NotNull PersistentDataContainerData container) {
        return CompletableFuture.runAsync(() -> {
            player.getPersistentDataContainer().getKeys().forEach(namespacedKey ->
                    player.getPersistentDataContainer().remove(namespacedKey));
            container.getTags().forEach(keyString -> {
                final NamespacedKey key = NamespacedKey.fromString(keyString);
                if (key != null) {
                    container.getTagType(keyString)
                            .flatMap(BukkitPersistentTypeMapping::getMapping)
                            .ifPresentOrElse(mapping -> mapping.setContainerValue(container, player, key),
                                    () -> plugin.log(Level.WARNING,
                                            "Could not set " + player.getName() + "'s persistent data key " + keyString +
                                                    " as it has an invalid type. Skipping!"));
                }
            });
        }).exceptionally(throwable -> {
            plugin.log(Level.WARNING,
                    "Could not write " + player.getName() + "'s persistent data map, skipping!");
            throwable.printStackTrace();
            return null;
        });
    }


    @Override
    @NotNull
    public Audience getAudience() {
        return plugin.getAudiences().player(player);
    }

    @Override
    public boolean isOffline() {
        try {
            return player == null;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @NotNull
    @Override
    public Version getMinecraftVersion() {
        return Version.fromString(Bukkit.getBukkitVersion());
    }

    @Override
    public boolean hasPermission(@NotNull String node) {
        return player.hasPermission(node);
    }

    @Override
    public CompletableFuture<Optional<ItemData>> showMenu(@NotNull ItemData itemData, boolean editable,
                                                          int minimumRows, @NotNull MineDown title) {
        final CompletableFuture<Optional<ItemData>> updatedData = new CompletableFuture<>();

        // Deserialize the item data to be shown and show it in a triumph GUI
        BukkitSerializer.deserializeItemStackArray(itemData.serializedItems).thenAccept(items -> {
            // Build the GUI and populate with items
            final int itemCount = items.length;
            final StorageBuilder guiBuilder = Gui.storage()
                    .title(title.toComponent())
                    .rows(Math.max(minimumRows, (int) Math.ceil(itemCount / 9.0)))
                    .disableAllInteractions()
                    .enableOtherActions();
            final StorageGui gui = editable ? guiBuilder.enableAllInteractions().create() : guiBuilder.create();
            for (int i = 0; i < itemCount; i++) {
                if (items[i] != null) {
                    gui.getInventory().setItem(i, items[i]);
                }
            }

            // Complete the future with updated data (if editable) when the GUI is closed
            gui.setCloseGuiAction(event -> {
                if (!editable) {
                    updatedData.complete(Optional.empty());
                    return;
                }

                // Get and save the updated items
                final ItemStack[] updatedItems = Arrays.copyOf(event.getPlayer().getOpenInventory()
                        .getTopInventory().getContents().clone(), itemCount);
                BukkitSerializer.serializeItemStackArray(updatedItems).thenAccept(serializedItems -> {
                    if (serializedItems.equals(itemData.serializedItems)) {
                        updatedData.complete(Optional.empty());
                        return;
                    }
                    updatedData.complete(Optional.of(new ItemData(serializedItems)));
                });
            });

            // Display the GUI (synchronously; on the main server thread)
            Bukkit.getScheduler().runTask(plugin, () -> gui.open(player));
        }).exceptionally(throwable -> {
            // Handle exceptions
            updatedData.completeExceptionally(throwable);
            return null;
        });
        return updatedData;
    }

    @Override
    public boolean isDead() {
        return player.getHealth() <= 0;
    }

    @Override
    public void sendToast(@NotNull MineDown title, @NotNull MineDown description,
                          @NotNull String iconMaterial, @NotNull String backgroundType) {
        try {
            final Material material = Material.matchMaterial(iconMaterial);
            Toast.builder(plugin)
                    .setTitle(title.toComponent())
                    .setDescription(description.toComponent())
                    .setIcon(material != null ? material : Material.BARRIER)
                    .setFrameType(FrameType.valueOf(backgroundType))
                    .build()
                    .show(player);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns a {@link Player}'s maximum health, minus any health boost effects
     *
     * @param player The {@link Player} to get the maximum health of
     * @return The {@link Player}'s max health
     */
    private static double getMaxHealth(@NotNull Player player) {
        double maxHealth = Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue();

        // If the player has additional health bonuses from synchronised potion effects, subtract these from this number as they are synchronised separately
        if (player.hasPotionEffect(PotionEffectType.HEALTH_BOOST) && maxHealth > 20D) {
            PotionEffect healthBoostEffect = player.getPotionEffect(PotionEffectType.HEALTH_BOOST);
            assert healthBoostEffect != null;
            double healthBoostBonus = 4 * (healthBoostEffect.getAmplifier() + 1);
            maxHealth -= healthBoostBonus;
        }
        return maxHealth;
    }

    @Override
    public boolean isLocked() {
        return plugin.getLockedPlayers().contains(player.getUniqueId());
    }

    @Override
    public boolean isNpc() {
        return player.hasMetadata("NPC");
    }

}
