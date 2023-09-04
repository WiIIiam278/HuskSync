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

package net.william278.husksync.data;

import com.google.gson.annotations.SerializedName;
import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTPersistentDataContainer;
import net.william278.husksync.HuskSync;
import net.william278.husksync.adapter.Adaptable;
import net.william278.husksync.player.BukkitUser;
import org.apache.commons.lang.NotImplementedException;
import org.bukkit.*;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public abstract class BukkitDataContainer implements DataContainer {

    public static abstract class Items implements DataContainer.Items {

        private final ItemStack[] contents;

        private Items(@NotNull ItemStack[] contents) {
            this.contents = contents;
        }

        @NotNull
        @Override
        public StackPreview[] getPreview() {
            return Arrays.stream(contents)
                    .map(stack -> stack != null ? new StackPreview(
                            stack.getType().getKey().toString(),
                            stack.getAmount(),
                            stack.hasItemMeta() ? (Objects.requireNonNull(
                                    stack.getItemMeta()).hasDisplayName() ? stack.getItemMeta().getDisplayName() : null)
                                    : null,
                            stack.hasItemMeta() ? (Objects.requireNonNull(
                                    stack.getItemMeta()).hasLore() ? stack.getItemMeta().getLore() : null)
                                    : null,
                            stack.hasItemMeta() && Objects.requireNonNull(stack.getItemMeta()).hasEnchants() ?
                                    stack.getItemMeta().getEnchants().keySet().stream()
                                            .map(enchantment -> enchantment.getKey().getKey())
                                            .toList()
                                    : List.of()
                    ) : null)
                    .toArray(StackPreview[]::new);
        }

        @Override
        public void clear() {
            Arrays.fill(contents, null);
        }

        @Override
        public void setContents(@NotNull Items contents) {
            System.arraycopy(
                    ((BukkitDataContainer.Items) contents).getContents(),
                    0, this.contents,
                    0, this.contents.length
            );
        }

        @NotNull
        public ItemStack[] getContents() {
            return contents;
        }

        public static class Inventory extends BukkitDataContainer.Items implements Items.Inventory {

            public static final int INVENTORY_SLOT_COUNT = 41;
            private int heldItemSlot;

            private Inventory(@NotNull ItemStack[] contents, int heldItemSlot) {
                super(contents);
                this.heldItemSlot = heldItemSlot;
            }

            @NotNull
            public static BukkitDataContainer.Items.Inventory adapt(@NotNull Player player) {
                return new BukkitDataContainer.Items.Inventory(
                        player.getInventory().getContents(),
                        player.getInventory().getHeldItemSlot()
                );
            }

            @NotNull
            public static BukkitDataContainer.Items.Inventory from(@NotNull ItemStack[] contents, int heldItemSlot) {
                return new BukkitDataContainer.Items.Inventory(contents, heldItemSlot);
            }

            @Override
            public void apply(@NotNull DataOwner user) throws IllegalStateException {
                final Player player = ((BukkitUser) user).getPlayer();
                this.clearInventoryCraftingSlots(player);
                player.setItemOnCursor(null);
                player.getInventory().setContents(getContents());
                player.updateInventory();
            }

            private void clearInventoryCraftingSlots(@NotNull Player player) {
                final org.bukkit.inventory.Inventory inventory = player.getOpenInventory().getTopInventory();
                if (inventory.getType() == InventoryType.CRAFTING) {
                    for (int slot = 0; slot < 5; slot++) {
                        inventory.setItem(slot, null);
                    }
                }
            }

            @Override
            public int getHeldItemSlot() {
                return heldItemSlot;
            }

            @Override
            public void setHeldItemSlot(int heldItemSlot) throws IllegalArgumentException {
                if (heldItemSlot < 0 || heldItemSlot > 8) {
                    throw new IllegalArgumentException("Held item slot must be between 0 and 8");
                }
                this.heldItemSlot = heldItemSlot;
            }

        }

        public static class EnderChest extends BukkitDataContainer.Items implements Items.EnderChest {

            private EnderChest(@NotNull ItemStack[] contents) {
                super(contents);
            }

            @NotNull
            public static BukkitDataContainer.Items.EnderChest adapt(@NotNull ItemStack[] items) {
                return new BukkitDataContainer.Items.EnderChest(items);
            }

            @Override
            public void apply(@NotNull DataOwner user) throws IllegalStateException {
                ((BukkitUser) user).getPlayer().getEnderChest().setContents(getContents());
            }

        }

        public static class DeathDrops extends BukkitDataContainer.Items implements Items {

            private DeathDrops(@NotNull ItemStack[] contents) {
                super(contents);
            }

            @NotNull
            public static DeathDrops adapt(@NotNull List<ItemStack> drops) {
                return new BukkitDataContainer.Items.DeathDrops(drops.toArray(ItemStack[]::new));
            }

            @Override
            public void apply(@NotNull DataOwner user) throws IllegalStateException {
                throw new NotImplementedException("Death drops cannot be applied to a player");
            }
        }
    }

    public static class PotionEffects implements DataContainer.PotionEffects, Adaptable {

        private final Collection<PotionEffect> effects;

        private PotionEffects(@NotNull Collection<PotionEffect> effects) {
            this.effects = effects;
        }

        @NotNull
        public static BukkitDataContainer.PotionEffects adapt(@NotNull Collection<PotionEffect> effects) {
            return new BukkitDataContainer.PotionEffects(effects);
        }

        @Override
        public void apply(@NotNull DataOwner user) throws IllegalStateException {
            final Player player = ((BukkitUser) user).getPlayer();
            player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
            player.addPotionEffects(effects);
        }

        @NotNull
        @Override
        public List<Effect> getActiveEffects() {
            return effects.stream()
                    .map(potionEffect -> new Effect(
                            potionEffect.getType().getName().toLowerCase(Locale.ENGLISH),
                            potionEffect.getDuration(),
                            potionEffect.getAmplifier(),
                            potionEffect.isAmbient(),
                            potionEffect.hasParticles(),
                            potionEffect.hasIcon()
                    ))
                    .toList();
        }

        @NotNull
        public Collection<PotionEffect> getEffects() {
            return effects;
        }

    }

    public static class Advancements implements DataContainer.Advancements {

        private final HuskSync plugin;
        private List<Advancement> completed;

        private Advancements(@NotNull Player player, @NotNull HuskSync plugin) {
            final ArrayList<Advancement> advancementData = new ArrayList<>();
            this.plugin = plugin;

            // Iterate through the server advancement set and add all advancements to the list
            Bukkit.getServer().advancementIterator().forEachRemaining(advancement -> {
                final AdvancementProgress advancementProgress = player.getAdvancementProgress(advancement);
                final Map<String, Date> awardedCriteria = new HashMap<>();

                advancementProgress.getAwardedCriteria().forEach(criteriaKey -> awardedCriteria.put(criteriaKey,
                        advancementProgress.getDateAwarded(criteriaKey)));

                // Only save the advancement if criteria has been completed
                if (!awardedCriteria.isEmpty()) {
                    advancementData.add(Advancement.adapt(advancement.getKey().toString(), awardedCriteria));
                }
            });
            this.completed = advancementData;
        }

        private Advancements(@NotNull List<Advancement> advancements, @NotNull HuskSync plugin) {
            this.completed = advancements;
            this.plugin = plugin;
        }

        @NotNull
        public static BukkitDataContainer.Advancements adapt(@NotNull Player player, @NotNull HuskSync plugin) {
            return new BukkitDataContainer.Advancements(player, plugin);
        }

        @NotNull
        public static BukkitDataContainer.Advancements from(@NotNull List<Advancement> advancements,
                                                            @NotNull HuskSync plugin) {
            return new BukkitDataContainer.Advancements(advancements, plugin);
        }

        @Override
        public void apply(@NotNull DataOwner user) throws IllegalStateException {
            // Temporarily disable advancement announcing if needed
            final Player player = ((BukkitUser) user).getPlayer();
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
            plugin.runAsync(() -> {

                // Apply the advancements to the player
                final Iterator<org.bukkit.advancement.Advancement> serverAdvancements = Bukkit.getServer().advancementIterator();
                while (serverAdvancements.hasNext()) {
                    // Iterate through all advancements
                    final org.bukkit.advancement.Advancement advancement = serverAdvancements.next();
                    final AdvancementProgress playerProgress = player.getAdvancementProgress(advancement);

                    completed.stream().filter(record -> record.getKey().equals(advancement.getKey().toString())).findFirst().ifPresentOrElse(
                            // Award all criteria that the player does not have that they do on the cache
                            record -> {
                                record.getCompletedCriteria().keySet().stream()
                                        .filter(criterion -> !playerProgress.getAwardedCriteria().contains(criterion))
                                        .forEach(criterion -> {
                                            plugin.runAsync(() -> player.getAdvancementProgress(advancement).awardCriteria(criterion));
                                            correctExperience.set(true);
                                        });

                                // Revoke all criteria that the player does have but should not
                                new ArrayList<>(playerProgress.getAwardedCriteria()).stream()
                                        .filter(criterion -> !record.getCompletedCriteria().containsKey(criterion))
                                        .forEach(criterion -> plugin.runSync(
                                                () -> player.getAdvancementProgress(advancement).revokeCriteria(criterion)));

                            },
                            // Revoke the criteria as the player shouldn't have any
                            () -> new ArrayList<>(playerProgress.getAwardedCriteria()).forEach(criterion ->
                                    plugin.runAsync(() -> player.getAdvancementProgress(advancement).revokeCriteria(criterion))));

                    // Update the player's experience in case the advancement changed that
                    if (correctExperience.get()) {
                        player.setLevel(experienceLevel);
                        player.setExp(expProgress);
                        correctExperience.set(false);
                    }
                }

                // Re-enable announcing advancements (back on the main thread again)
                plugin.runSync(() -> {
                    if (finalAnnounceAdvancementUpdate) {
                        player.getWorld().setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, true);
                    }
                });
            });
        }

        @NotNull
        @Override
        public List<Advancement> getCompleted() {
            return completed;
        }

        @Override
        public void setCompleted(@NotNull List<Advancement> completed) {
            this.completed = completed;
        }

    }

    public static class Location implements DataContainer.Location, Adaptable {
        private double x;
        private double y;
        private double z;
        private float yaw;
        private float pitch;
        private World world;

        private Location(@NotNull org.bukkit.Location location) {
            this.x = location.getX();
            this.y = location.getY();
            this.z = location.getZ();
            this.yaw = location.getYaw();
            this.pitch = location.getPitch();
            this.world = new World(
                    Objects.requireNonNull(location.getWorld(), "World is null").getName(),
                    location.getWorld().getUID()
            );
        }

        @SuppressWarnings("unused")
        private Location() {
        }

        @NotNull
        public static BukkitDataContainer.Location adapt(@NotNull org.bukkit.Location location) {
            return new BukkitDataContainer.Location(location);
        }

        @Override
        public void apply(@NotNull DataOwner user) throws IllegalStateException {
            try {
                final org.bukkit.Location location = new org.bukkit.Location(
                        Bukkit.getWorld(world.name()), x, y, z, yaw, pitch
                );
                ((BukkitUser) user).getPlayer().teleport(location);
            } catch (Throwable e) {
                throw new IllegalStateException("Failed to apply location", e);
            }
        }

        @Override
        public double getX() {
            return x;
        }

        @Override
        public void setX(double x) {
            this.x = x;
        }

        @Override
        public double getY() {
            return y;
        }

        @Override
        public void setY(double y) {
            this.y = y;
        }

        @Override
        public double getZ() {
            return z;
        }

        @Override
        public void setZ(double z) {
            this.z = z;
        }

        @Override
        public float getYaw() {
            return yaw;
        }

        @Override
        public void setYaw(float yaw) {
            this.yaw = yaw;
        }

        @Override
        public float getPitch() {
            return pitch;
        }

        @Override
        public void setPitch(float pitch) {
            this.pitch = pitch;
        }

        @NotNull
        @Override
        public World getWorld() {
            return world;
        }

        @Override
        public void setWorld(@NotNull World world) {
            this.world = world;
        }

    }

    public static class Statistics implements DataContainer.Statistics {
        private Map<Statistic, Integer> untypedStatistics;
        private Map<Statistic, Map<Material, Integer>> blockStatistics;
        private Map<Statistic, Map<Material, Integer>> itemStatistics;
        private Map<Statistic, Map<EntityType, Integer>> entityStatistics;

        private Statistics(@NotNull Map<Statistic, Integer> genericStatistics,
                           @NotNull Map<Statistic, Map<Material, Integer>> blockStatistics,
                           @NotNull Map<Statistic, Map<Material, Integer>> itemStatistics,
                           @NotNull Map<Statistic, Map<EntityType, Integer>> entityStatistics) {
            this.untypedStatistics = genericStatistics;
            this.blockStatistics = blockStatistics;
            this.itemStatistics = itemStatistics;
            this.entityStatistics = entityStatistics;
        }

        @SuppressWarnings("unused")
        private Statistics() {
        }

        @NotNull
        public static BukkitDataContainer.Statistics adapt(@NotNull Player player) {
            return new BukkitDataContainer.Statistics(
                    // Generic (untyped) stats
                    Arrays.stream(Statistic.values())
                            .filter(stat -> stat.getType() == Statistic.Type.UNTYPED)
                            .filter(stat -> player.getStatistic(stat) != 0)
                            .map(stat -> Map.entry(stat, player.getStatistic(stat)))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),

                    // Block stats
                    Arrays.stream(Statistic.values())
                            .filter(stat -> stat.getType() == Statistic.Type.BLOCK)
                            .map(stat -> Map.entry(stat, Arrays.stream(Material.values())
                                    .filter(Material::isBlock)
                                    .filter(material -> player.getStatistic(stat, material) != 0)
                                    .map(material -> Map.entry(material, player.getStatistic(stat, material)))
                                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))))
                            .filter(entry -> !entry.getValue().isEmpty())
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),

                    // Item stats
                    Arrays.stream(Statistic.values())
                            .filter(stat -> stat.getType() == Statistic.Type.ITEM)
                            .map(stat -> Map.entry(stat, Arrays.stream(Material.values())
                                    .filter(Material::isItem)
                                    .filter(material -> player.getStatistic(stat, material) != 0)
                                    .map(material -> Map.entry(material, player.getStatistic(stat, material)))
                                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))))
                            .filter(entry -> !entry.getValue().isEmpty())
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),

                    // Entity stats
                    Arrays.stream(Statistic.values())
                            .filter(stat -> stat.getType() == Statistic.Type.ENTITY)
                            .map(stat -> Map.entry(stat, Arrays.stream(EntityType.values())
                                    .filter(EntityType::isAlive)
                                    .filter(entityType -> player.getStatistic(stat, entityType) != 0)
                                    .map(entityType -> Map.entry(entityType, player.getStatistic(stat, entityType)))
                                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))))
                            .filter(entry -> !entry.getValue().isEmpty())
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
            );
        }

        @NotNull
        public static BukkitDataContainer.Statistics from(@NotNull StatisticsSet stats) {
            return new BukkitDataContainer.Statistics(
                    stats.genericStats().entrySet().stream().collect(Collectors.toMap(
                            entry -> Statistic.valueOf(entry.getKey()),
                            Map.Entry::getValue
                    )),
                    stats.blockStats().entrySet().stream().collect(Collectors.toMap(
                            entry -> Statistic.valueOf(entry.getKey()),
                            entry -> entry.getValue().entrySet().stream().collect(Collectors.toMap(
                                    blockEntry -> Material.valueOf(blockEntry.getKey()),
                                    Map.Entry::getValue
                            ))
                    )),
                    stats.itemStats().entrySet().stream().collect(Collectors.toMap(
                            entry -> Statistic.valueOf(entry.getKey()),
                            entry -> entry.getValue().entrySet().stream().collect(Collectors.toMap(
                                    itemEntry -> Material.valueOf(itemEntry.getKey()),
                                    Map.Entry::getValue
                            ))
                    )),
                    stats.entityStats().entrySet().stream().collect(Collectors.toMap(
                            entry -> Statistic.valueOf(entry.getKey()),
                            entry -> entry.getValue().entrySet().stream().collect(Collectors.toMap(
                                    entityEntry -> EntityType.valueOf(entityEntry.getKey()),
                                    Map.Entry::getValue
                            ))
                    ))
            );
        }

        @Override
        public void apply(@NotNull DataOwner user) throws IllegalStateException {
            untypedStatistics.forEach((stat, value) -> applyStat(user, stat, null, value));
            blockStatistics.forEach((stat, m) -> m.forEach((block, value) -> applyStat(user, stat, block, value)));
            itemStatistics.forEach((stat, m) -> m.forEach((item, value) -> applyStat(user, stat, item, value)));
            entityStatistics.forEach((stat, m) -> m.forEach((entity, value) -> applyStat(user, stat, entity, value)));
        }

        private void applyStat(@NotNull DataOwner user, @NotNull Statistic stat, @Nullable Object type, int value) {
            try {
                final Player player = ((BukkitUser) user).getPlayer();
                if (type == null) {
                    player.setStatistic(stat, value);
                } else if (type instanceof Material) {
                    player.setStatistic(stat, (Material) type, value);
                } else if (type instanceof EntityType) {
                    player.setStatistic(stat, (EntityType) type, value);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        @NotNull
        @Override
        public Map<String, Integer> getGenericStatistics() {
            return untypedStatistics.entrySet().stream().collect(
                    TreeMap::new,
                    (m, e) -> m.put(e.getKey().toString(), e.getValue()), TreeMap::putAll
            );
        }

        @NotNull
        @Override
        public Map<String, Map<String, Integer>> getBlockStatistics() {
            return blockStatistics.entrySet().stream().collect(
                    TreeMap::new,
                    (m, e) -> m.put(e.getKey().toString(), e.getValue().entrySet().stream().collect(
                            TreeMap::new,
                            (m2, e2) -> m2.put(e2.getKey().getKey().toString(), e2.getValue()), TreeMap::putAll
                    )), TreeMap::putAll
            );
        }

        @NotNull
        @Override
        public Map<String, Map<String, Integer>> getItemStatistics() {
            return itemStatistics.entrySet().stream().collect(
                    TreeMap::new,
                    (m, e) -> m.put(e.getKey().toString(), e.getValue().entrySet().stream().collect(
                            TreeMap::new,
                            (m2, e2) -> m2.put(e2.getKey().getKey().toString(), e2.getValue()), TreeMap::putAll
                    )), TreeMap::putAll
            );
        }

        @NotNull
        @Override
        public Map<String, Map<String, Integer>> getEntityStatistics() {
            return entityStatistics.entrySet().stream().collect(
                    TreeMap::new,
                    (m, e) -> m.put(e.getKey().toString(), e.getValue().entrySet().stream().collect(
                            TreeMap::new,
                            (m2, e2) -> m2.put(e2.getKey().getKey().toString(), e2.getValue()), TreeMap::putAll
                    )), TreeMap::putAll
            );
        }

        @NotNull
        protected StatisticsSet getStatisticsSet() {
            return new StatisticsSet(
                    getGenericStatistics(),
                    getBlockStatistics(),
                    getItemStatistics(),
                    getEntityStatistics()
            );
        }

        protected record StatisticsSet(
                @SerializedName("generic") @NotNull Map<String, Integer> genericStats,
                @SerializedName("blocks") @NotNull Map<String, Map<String, Integer>> blockStats,
                @SerializedName("items") @NotNull Map<String, Map<String, Integer>> itemStats,
                @SerializedName("entities") @NotNull Map<String, Map<String, Integer>> entityStats) {
        }

    }

    public static class PersistentData implements DataContainer.PersistentData {
        private final NBTCompound persistentData;

        private PersistentData(@NotNull NBTCompound persistentData) {
            this.persistentData = persistentData;
        }

        @NotNull
        public static BukkitDataContainer.PersistentData adapt(@NotNull PersistentDataContainer persistentData) {
            return new BukkitDataContainer.PersistentData(new NBTPersistentDataContainer(persistentData));
        }

        @NotNull
        public static BukkitDataContainer.PersistentData from(@NotNull NBTCompound compound) {
            return new BukkitDataContainer.PersistentData(compound);
        }

        @Override
        public void apply(@NotNull DataOwner user) throws IllegalStateException {
            final NBTPersistentDataContainer container = new NBTPersistentDataContainer(
                    ((BukkitUser) user).getPlayer().getPersistentDataContainer()
            );
            container.clearNBT();
            container.mergeCompound(persistentData);
        }

        @NotNull
        public NBTCompound getPersistentData() {
            return persistentData;
        }

    }

    public static class Health implements DataContainer.Health, Adaptable {
        @SerializedName("health")
        private double health;
        @SerializedName("max_health")
        private double maxHealth;
        @SerializedName("health_scale")
        private double healthScale;

        private Health(double health, double maxHealth, double healthScale) {
            this.health = health;
            this.maxHealth = maxHealth;
            this.healthScale = healthScale;
        }

        @SuppressWarnings("unused")
        private Health() {
        }

        public static BukkitDataContainer.Health adapt(@NotNull Player player) {
            return new BukkitDataContainer.Health(
                    player.getHealth(),
                    Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH),
                            "Missing max health attribute").getValue(),
                    player.getHealthScale()
            );
        }

        @Override
        public void apply(@NotNull DataOwner user) throws IllegalStateException {
            final Player player = ((BukkitUser) user).getPlayer();

            // Set base max health
            final AttributeInstance maxHealthAttribute = Objects.requireNonNull(
                    player.getAttribute(Attribute.GENERIC_MAX_HEALTH), "Missing max health attribute");
            double currentMaxHealth = maxHealthAttribute.getBaseValue();
            if (maxHealth != 0d) {
                maxHealthAttribute.setBaseValue(maxHealth);
                currentMaxHealth = maxHealth;
            }

            // Set health
            final double currentHealth = player.getHealth();
            if (health != currentHealth) {
                final double healthToSet = currentHealth > currentMaxHealth ? currentMaxHealth : health;
                try {
                    player.setHealth(Math.min(healthToSet, currentMaxHealth));
                } catch (IllegalArgumentException e) {
//                    plugin.log(Level.WARNING, "Failed to set player health", e);
                }
            }

            // Set health scale
            try {
                if (healthScale != 0d) {
                    player.setHealthScale(healthScale);
                } else {
                    player.setHealthScale(maxHealth);
                }
                player.setHealthScaled(healthScale != 0D);
            } catch (IllegalArgumentException e) {
//                plugin.log(Level.WARNING, "Failed to set player health scale", e);
            }
        }

        @Override
        public double getHealth() {
            return health;
        }

        @Override
        public void setHealth(double health) {
            this.health = health;
        }

        @Override
        public double getMaxHealth() {
            return maxHealth;
        }

        @Override
        public void setMaxHealth(double maxHealth) {
            this.maxHealth = maxHealth;
        }

        @Override
        public double getHealthScale() {
            return healthScale;
        }

        @Override
        public void setHealthScale(double healthScale) {
            this.healthScale = healthScale;
        }

    }

    public static class Food implements DataContainer.Food, Adaptable {

        @SerializedName("food_level")
        private int foodLevel;
        @SerializedName("saturation")
        private float saturation;
        @SerializedName("exhaustion")
        private float exhaustion;

        private Food(int foodLevel, float saturation, float exhaustion) {
            this.foodLevel = foodLevel;
            this.saturation = saturation;
            this.exhaustion = exhaustion;
        }

        @SuppressWarnings("unused")
        private Food() {
        }

        public static BukkitDataContainer.Food adapt(@NotNull Player player) {
            return new BukkitDataContainer.Food(
                    player.getFoodLevel(),
                    player.getSaturation(),
                    player.getExhaustion()
            );
        }

        @Override
        public void apply(@NotNull DataOwner user) throws IllegalStateException {
            final Player player = ((BukkitUser) user).getPlayer();
            player.setFoodLevel(foodLevel);
            player.setSaturation(saturation);
            player.setExhaustion(exhaustion);
        }

        @Override
        public int getFoodLevel() {
            return foodLevel;
        }

        @Override
        public void setFoodLevel(int foodLevel) {
            this.foodLevel = foodLevel;
        }

        @Override
        public float getSaturation() {
            return saturation;
        }

        @Override
        public void setSaturation(float saturation) {
            this.saturation = saturation;
        }

        @Override
        public float getExhaustion() {
            return exhaustion;
        }

        @Override
        public void setExhaustion(float exhaustion) {
            this.exhaustion = exhaustion;
        }
    }

    public static class Experience implements DataContainer.Experience, Adaptable {

        @SerializedName("total_experience")
        private int totalExperience;

        @SerializedName("exp_level")
        private int expLevel;

        @SerializedName("exp_progress")
        private float expProgress;

        private Experience(int totalExperience, int expLevel, float expProgress) {
            this.totalExperience = totalExperience;
            this.expLevel = expLevel;
            this.expProgress = expProgress;
        }

        @SuppressWarnings("unused")
        private Experience() {
        }

        public static BukkitDataContainer.Experience adapt(@NotNull Player player) {
            return new BukkitDataContainer.Experience(
                    player.getTotalExperience(),
                    player.getLevel(),
                    player.getExp()
            );
        }

        @Override
        public void apply(@NotNull DataOwner user) throws IllegalStateException {
            final Player player = ((BukkitUser) user).getPlayer();
            player.setTotalExperience(totalExperience);
            player.setLevel(expLevel);
            player.setExp(expProgress);
        }

        @Override
        public int getTotalExperience() {
            return totalExperience;
        }

        @Override
        public void setTotalExperience(int totalExperience) {
            this.totalExperience = totalExperience;
        }

        @Override
        public int getExpLevel() {
            return expLevel;
        }

        @Override
        public void setExpLevel(int expLevel) {
            this.expLevel = expLevel;
        }

        @Override
        public float getExpProgress() {
            return expProgress;
        }

        @Override
        public void setExpProgress(float expProgress) {
            this.expProgress = expProgress;
        }

    }

    public static class GameMode implements DataContainer.GameMode, Adaptable {

        @SerializedName("game_mode")
        private String gameMode;
        @SerializedName("allow_flight")
        private boolean allowFlight;
        @SerializedName("is_flying")
        private boolean isFlying;

        private GameMode(@NotNull org.bukkit.GameMode gameMode, boolean allowFlight, boolean isFlying) {
            this.gameMode = gameMode.name();
            this.allowFlight = allowFlight;
            this.isFlying = isFlying;
        }

        public static BukkitDataContainer.GameMode adapt(@NotNull Player player) {
            return new BukkitDataContainer.GameMode(
                    player.getGameMode(),
                    player.getAllowFlight(),
                    player.isFlying()
            );
        }

        @Override
        public void apply(@NotNull DataOwner user) throws IllegalStateException {
            final Player player = ((BukkitUser) user).getPlayer();
            player.setGameMode(org.bukkit.GameMode.valueOf(gameMode));
            player.setAllowFlight(allowFlight);
            player.setFlying(isFlying);
        }

        @NotNull
        @Override
        public String getGameMode() {
            return gameMode;
        }

        @Override
        public void setGameMode(@NotNull String gameMode) {
            this.gameMode = gameMode;
        }

        @Override
        public boolean getAllowFlight() {
            return allowFlight;
        }

        @Override
        public void setAllowFlight(boolean allowFlight) {
            this.allowFlight = allowFlight;
        }

        @Override
        public boolean getIsFlying() {
            return isFlying;
        }

        @Override
        public void setIsFlying(boolean isFlying) {
            this.isFlying = isFlying;
        }

    }

}
