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
import net.william278.desertwell.util.ThrowingConsumer;
import net.william278.husksync.BukkitHuskSync;
import net.william278.husksync.HuskSync;
import net.william278.husksync.adapter.Adaptable;
import net.william278.husksync.user.BukkitUser;
import org.apache.commons.lang.NotImplementedException;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.Statistic;
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
import java.util.stream.Collectors;

public abstract class BukkitData implements Data {

    public static abstract class Items implements Data.Items {

        private final ItemStack[] contents;

        private Items(@NotNull ItemStack[] contents) {
            this.contents = Arrays.stream(contents)
                    .map(i -> i == null || i.getType() == Material.AIR ? null : i)
                    .toArray(ItemStack[]::new);
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
                    ((BukkitData.Items) contents).getContents(),
                    0, this.contents,
                    0, this.contents.length
            );
        }

        @NotNull
        public ItemStack[] getContents() {
            return contents;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof BukkitData.Items items) {
                return Arrays.equals(contents, items.getContents());
            }
            return false;
        }

        public static class Inventory extends BukkitData.Items implements Items.Inventory {

            public static final int INVENTORY_SLOT_COUNT = 41;
            private int heldItemSlot;

            private Inventory(@NotNull ItemStack[] contents, int heldItemSlot) {
                super(contents);
                this.heldItemSlot = heldItemSlot;
            }

            @NotNull
            public static BukkitData.Items.Inventory from(@NotNull ItemStack[] contents, int heldItemSlot) {
                return new BukkitData.Items.Inventory(contents, heldItemSlot);
            }

            @NotNull
            public static BukkitData.Items.Inventory empty() {
                return new BukkitData.Items.Inventory(new ItemStack[INVENTORY_SLOT_COUNT], 0);
            }

            @Override
            public int getSlotCount() {
                return INVENTORY_SLOT_COUNT;
            }

            @Override
            public void apply(@NotNull UserDataHolder user, @NotNull HuskSync plugin) throws IllegalStateException {
                final Player player = ((BukkitUser) user).getPlayer();
                this.clearInventoryCraftingSlots(player);
                player.setItemOnCursor(null);
                player.getInventory().setContents(((BukkitHuskSync) plugin).setMapViews(getContents()));
                player.updateInventory();
                player.getInventory().setHeldItemSlot(heldItemSlot);
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

        public static class EnderChest extends BukkitData.Items implements Items.EnderChest {

            public static final int ENDER_CHEST_SLOT_COUNT = 27;

            private EnderChest(@NotNull ItemStack[] contents) {
                super(contents);
            }

            @NotNull
            public static BukkitData.Items.EnderChest adapt(@NotNull ItemStack[] items) {
                return new BukkitData.Items.EnderChest(items);
            }

            @Override
            public void apply(@NotNull UserDataHolder user, @NotNull HuskSync plugin) throws IllegalStateException {
                ((BukkitUser) user).getPlayer().getEnderChest().setContents(
                        ((BukkitHuskSync) plugin).setMapViews(getContents())
                );
            }

        }

        public static class ItemArray extends BukkitData.Items implements Items {

            private ItemArray(@NotNull ItemStack[] contents) {
                super(contents);
            }

            @NotNull
            public static ItemArray adapt(@NotNull Collection<ItemStack> drops) {
                return new ItemArray(drops.toArray(ItemStack[]::new));
            }

            @NotNull
            public static ItemArray adapt(@NotNull ItemStack[] drops) {
                return new ItemArray(drops);
            }

            @Override
            public void apply(@NotNull UserDataHolder user, @NotNull HuskSync plugin) throws IllegalStateException {
                throw new NotImplementedException("A generic item array cannot be applied to a player");
            }

        }

    }

    public static class PotionEffects implements Data.PotionEffects, Adaptable {

        private final Collection<PotionEffect> effects;

        private PotionEffects(@NotNull Collection<PotionEffect> effects) {
            this.effects = effects;
        }

        @NotNull
        public static BukkitData.PotionEffects adapt(@NotNull Collection<PotionEffect> effects) {
            return new BukkitData.PotionEffects(effects);
        }

        @Override
        public void apply(@NotNull UserDataHolder user, @NotNull HuskSync plugin) throws IllegalStateException {
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
        @SuppressWarnings("unused")
        public Collection<PotionEffect> getEffects() {
            return effects;
        }

    }

    public static class Advancements implements Data.Advancements {

        private List<Advancement> completed;

        private Advancements(@NotNull List<Advancement> advancements) {
            this.completed = advancements;
        }

        // Iterate through the server advancement set and add all advancements to the list
        @NotNull
        public static BukkitData.Advancements adapt(@NotNull Player player) {
            final List<Advancement> advancements = new ArrayList<>();
            forEachAdvancement(advancement -> {
                final AdvancementProgress advancementProgress = player.getAdvancementProgress(advancement);
                final Map<String, Date> awardedCriteria = new HashMap<>();

                advancementProgress.getAwardedCriteria().forEach(criteriaKey -> awardedCriteria.put(criteriaKey,
                        advancementProgress.getDateAwarded(criteriaKey)));

                // Only save the advancement if criteria has been completed
                if (!awardedCriteria.isEmpty()) {
                    advancements.add(Advancement.adapt(advancement.getKey().toString(), awardedCriteria));
                }
            });
            return new BukkitData.Advancements(advancements);
        }

        @NotNull
        public static BukkitData.Advancements from(@NotNull List<Advancement> advancements) {
            return new BukkitData.Advancements(advancements);
        }

        @Override
        public void apply(@NotNull UserDataHolder user, @NotNull HuskSync plugin) throws IllegalStateException {
            plugin.runAsync(() -> forEachAdvancement(advancement -> {
                final Player player = ((BukkitUser) user).getPlayer();
                final AdvancementProgress progress = player.getAdvancementProgress(advancement);
                final Optional<Advancement> record = completed.stream()
                        .filter(r -> r.getKey().equals(advancement.getKey().toString()))
                        .findFirst();
                if (record.isEmpty()) {
                    this.setAdvancement(plugin, advancement, player, List.of(), progress.getAwardedCriteria());
                    return;
                }

                final Map<String, Date> criteria = record.get().getCompletedCriteria();
                this.setAdvancement(
                        plugin, advancement, player,
                        criteria.keySet().stream().filter(key -> !progress.getAwardedCriteria().contains(key)).toList(),
                        progress.getAwardedCriteria().stream().filter(key -> !criteria.containsKey(key)).toList()
                );
            }));
        }

        private void setAdvancement(@NotNull HuskSync plugin,
                                    @NotNull org.bukkit.advancement.Advancement advancement, @NotNull Player player,
                                    @NotNull Collection<String> toAward, @NotNull Collection<String> toRevoke) {
            plugin.runSync(() -> {
                // Track player exp level & progress
                final int expLevel = player.getLevel();
                final float expProgress = player.getExp();
                boolean gameRuleUpdated = false;
                if (Boolean.TRUE.equals(player.getWorld().getGameRuleValue(GameRule.ANNOUNCE_ADVANCEMENTS))) {
                    player.getWorld().setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
                    gameRuleUpdated = true;
                }

                // Award and revoke advancement criteria
                final AdvancementProgress progress = player.getAdvancementProgress(advancement);
                toAward.forEach(progress::awardCriteria);
                toRevoke.forEach(progress::revokeCriteria);

                // Set player experience and level (prevent advancement awards applying twice), reset game rule
                if (!toAward.isEmpty() && player.getLevel() != expLevel || player.getExp() != expProgress) {
                    player.setLevel(expLevel);
                    player.setExp(expProgress);
                }
                if (gameRuleUpdated) {
                    player.getWorld().setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, true);
                }
            });
        }

        // Performs a consuming function for every advancement registered on the server
        private static void forEachAdvancement(@NotNull ThrowingConsumer<org.bukkit.advancement.Advancement> consumer) {
            Bukkit.getServer().advancementIterator().forEachRemaining(consumer);
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

    public static class Location implements Data.Location, Adaptable {
        @SerializedName("x")
        private double x;
        @SerializedName("y")
        private double y;
        @SerializedName("z")
        private double z;
        @SerializedName("yaw")
        private float yaw;
        @SerializedName("pitch")
        private float pitch;
        @SerializedName("world")
        private World world;

        private Location(double x, double y, double z, float yaw, float pitch, @NotNull World world) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.world = world;
        }

        @SuppressWarnings("unused")
        private Location() {
        }

        @NotNull
        public static BukkitData.Location from(double x, double y, double z,
                                               float yaw, float pitch, @NotNull World world) {
            return new BukkitData.Location(x, y, z, yaw, pitch, world);
        }

        @NotNull
        public static BukkitData.Location adapt(@NotNull org.bukkit.Location location) {
            return from(
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    location.getYaw(),
                    location.getPitch(),
                    new World(
                            Objects.requireNonNull(location.getWorld(), "World is null").getName(),
                            location.getWorld().getUID(),
                            location.getWorld().getEnvironment().name()
                    )
            );
        }

        @Override
        public void apply(@NotNull UserDataHolder user, @NotNull HuskSync plugin) throws IllegalStateException {
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

    public static class Statistics implements Data.Statistics {
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
        public static BukkitData.Statistics adapt(@NotNull Player player) {
            return new BukkitData.Statistics(
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
        public static BukkitData.Statistics from(@NotNull StatisticsMap stats) {
            return new BukkitData.Statistics(
                    stats.genericStats().entrySet().stream().collect(Collectors.toMap(
                            entry -> matchStatistic(entry.getKey()),
                            Map.Entry::getValue
                    )),
                    stats.blockStats().entrySet().stream().collect(Collectors.toMap(
                            entry -> matchStatistic(entry.getKey()),
                            entry -> entry.getValue().entrySet().stream().collect(Collectors.toMap(
                                    blockEntry -> Material.matchMaterial(blockEntry.getKey()),
                                    Map.Entry::getValue
                            ))
                    )),
                    stats.itemStats().entrySet().stream().collect(Collectors.toMap(
                            entry -> matchStatistic(entry.getKey()),
                            entry -> entry.getValue().entrySet().stream().collect(Collectors.toMap(
                                    itemEntry -> Material.matchMaterial(itemEntry.getKey()),
                                    Map.Entry::getValue
                            ))
                    )),
                    stats.entityStats().entrySet().stream().collect(Collectors.toMap(
                            entry -> matchStatistic(entry.getKey()),
                            entry -> entry.getValue().entrySet().stream().collect(Collectors.toMap(
                                    entityEntry -> matchEntityType(entityEntry.getKey()),
                                    Map.Entry::getValue
                            ))
                    ))
            );
        }

        @NotNull
        public static BukkitData.Statistics from(@NotNull Map<Statistic, Integer> genericStats,
                                                 @NotNull Map<Statistic, Map<Material, Integer>> blockStats,
                                                 @NotNull Map<Statistic, Map<Material, Integer>> itemStats,
                                                 @NotNull Map<Statistic, Map<EntityType, Integer>> entityStats) {
            return new BukkitData.Statistics(genericStats, blockStats, itemStats, entityStats);
        }

        public static StatisticsMap createStatisticsMap(@NotNull Map<String, Integer> genericStats,
                                                        @NotNull Map<String, Map<String, Integer>> blockStats,
                                                        @NotNull Map<String, Map<String, Integer>> itemStats,
                                                        @NotNull Map<String, Map<String, Integer>> entityStats) {
            return new StatisticsMap(genericStats, blockStats, itemStats, entityStats);
        }

        @NotNull
        private static Statistic matchStatistic(@NotNull String key) {
            return Arrays.stream(Statistic.values())
                    .filter(stat -> stat.getKey().toString().equals(key))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid statistic key: %s", key)));
        }

        @NotNull
        private static EntityType matchEntityType(@NotNull String key) {
            return Arrays.stream(EntityType.values())
                    .filter(entityType -> entityType.getKey().toString().equals(key))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Invalid entity type key: %s", key)));
        }

        @Override
        public void apply(@NotNull UserDataHolder user, @NotNull HuskSync plugin) throws IllegalStateException {
            untypedStatistics.forEach((stat, value) -> applyStat(user, stat, null, value));
            blockStatistics.forEach((stat, m) -> m.forEach((block, value) -> applyStat(user, stat, block, value)));
            itemStatistics.forEach((stat, m) -> m.forEach((item, value) -> applyStat(user, stat, item, value)));
            entityStatistics.forEach((stat, m) -> m.forEach((entity, value) -> applyStat(user, stat, entity, value)));
        }

        private void applyStat(@NotNull UserDataHolder user, @NotNull Statistic stat, @Nullable Object type, int value) {
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
                    (m, e) -> m.put(e.getKey().getKey().toString(), e.getValue()), TreeMap::putAll
            );
        }

        @NotNull
        @Override
        public Map<String, Map<String, Integer>> getBlockStatistics() {
            return blockStatistics.entrySet().stream().collect(
                    TreeMap::new,
                    (m, e) -> m.put(e.getKey().getKey().toString(), e.getValue().entrySet().stream().collect(
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
                    (m, e) -> m.put(e.getKey().getKey().toString(), e.getValue().entrySet().stream().collect(
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
                    (m, e) -> m.put(e.getKey().getKey().toString(), e.getValue().entrySet().stream().collect(
                            TreeMap::new,
                            (m2, e2) -> m2.put(e2.getKey().getKey().toString(), e2.getValue()), TreeMap::putAll
                    )), TreeMap::putAll
            );
        }

        @NotNull
        protected StatisticsMap getStatisticsSet() {
            return new StatisticsMap(
                    getGenericStatistics(),
                    getBlockStatistics(),
                    getItemStatistics(),
                    getEntityStatistics()
            );
        }

        protected record StatisticsMap(
                @SerializedName("generic") @NotNull Map<String, Integer> genericStats,
                @SerializedName("blocks") @NotNull Map<String, Map<String, Integer>> blockStats,
                @SerializedName("items") @NotNull Map<String, Map<String, Integer>> itemStats,
                @SerializedName("entities") @NotNull Map<String, Map<String, Integer>> entityStats
        ) {
        }

    }

    public static class PersistentData implements Data.PersistentData {
        private final NBTCompound persistentData;

        private PersistentData(@NotNull NBTCompound persistentData) {
            this.persistentData = persistentData;
        }

        @NotNull
        public static BukkitData.PersistentData adapt(@NotNull PersistentDataContainer persistentData) {
            return new BukkitData.PersistentData(new NBTPersistentDataContainer(persistentData));
        }

        @NotNull
        public static BukkitData.PersistentData from(@NotNull NBTCompound compound) {
            return new BukkitData.PersistentData(compound);
        }

        @Override
        public void apply(@NotNull UserDataHolder user, @NotNull HuskSync plugin) throws IllegalStateException {
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

    public static class Health implements Data.Health, Adaptable {
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

        @NotNull
        public static BukkitData.Health from(double health, double maxHealth, double healthScale) {
            return new BukkitData.Health(health, maxHealth, healthScale);
        }

        @NotNull
        public static BukkitData.Health adapt(@NotNull Player player) {
            return from(
                    player.getHealth(),
                    Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH),
                            "Missing max health attribute").getValue(),
                    player.getHealthScale()
            );
        }

        @Override
        public void apply(@NotNull UserDataHolder user, @NotNull HuskSync plugin) throws IllegalStateException {
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

    public static class Hunger implements Data.Hunger, Adaptable {

        @SerializedName("food_level")
        private int foodLevel;
        @SerializedName("saturation")
        private float saturation;
        @SerializedName("exhaustion")
        private float exhaustion;

        private Hunger(int foodLevel, float saturation, float exhaustion) {
            this.foodLevel = foodLevel;
            this.saturation = saturation;
            this.exhaustion = exhaustion;
        }

        @SuppressWarnings("unused")
        private Hunger() {
        }

        @NotNull
        public static Hunger adapt(@NotNull Player player) {
            return from(player.getFoodLevel(), player.getSaturation(), player.getExhaustion());
        }

        @NotNull
        public static Hunger from(int foodLevel, float saturation, float exhaustion) {
            return new BukkitData.Hunger(foodLevel, saturation, exhaustion);
        }

        @Override
        public void apply(@NotNull UserDataHolder user, @NotNull HuskSync plugin) throws IllegalStateException {
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

    public static class Experience implements Data.Experience, Adaptable {

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

        @NotNull
        public static BukkitData.Experience from(int totalExperience, int expLevel, float expProgress) {
            return new BukkitData.Experience(totalExperience, expLevel, expProgress);
        }

        @NotNull
        public static BukkitData.Experience adapt(@NotNull Player player) {
            return from(player.getTotalExperience(), player.getLevel(), player.getExp());
        }

        @Override
        public void apply(@NotNull UserDataHolder user, @NotNull HuskSync plugin) throws IllegalStateException {
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

    public static class GameMode implements Data.GameMode, Adaptable {

        @SerializedName("game_mode")
        private String gameMode;
        @SerializedName("allow_flight")
        private boolean allowFlight;
        @SerializedName("is_flying")
        private boolean isFlying;

        private GameMode(@NotNull String gameMode, boolean allowFlight, boolean isFlying) {
            this.gameMode = gameMode;
            this.allowFlight = allowFlight;
            this.isFlying = isFlying;
        }

        @NotNull
        public static BukkitData.GameMode from(@NotNull String gameMode, boolean allowFlight, boolean isFlying) {
            return new BukkitData.GameMode(gameMode, allowFlight, isFlying);
        }

        @NotNull
        public static BukkitData.GameMode adapt(@NotNull Player player) {
            return from(player.getGameMode().name(), player.getAllowFlight(), player.isFlying());
        }

        @Override
        public void apply(@NotNull UserDataHolder user, @NotNull HuskSync plugin) throws IllegalStateException {
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
