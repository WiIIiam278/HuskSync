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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.annotations.SerializedName;
import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTPersistentDataContainer;
import lombok.*;
import net.william278.desertwell.util.ThrowingConsumer;
import net.william278.husksync.BukkitHuskSync;
import net.william278.husksync.HuskSync;
import net.william278.husksync.adapter.Adaptable;
import net.william278.husksync.config.Settings.SynchronizationSettings.AttributeSettings;
import net.william278.husksync.user.BukkitUser;
import org.bukkit.*;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static net.william278.husksync.util.BukkitKeyedAdapter.*;

public abstract class BukkitData implements Data {

    @Override
    public final void apply(@NotNull UserDataHolder dataHolder, @NotNull HuskSync plugin) throws IllegalStateException {
        this.apply((BukkitUser) dataHolder, (BukkitHuskSync) plugin);
    }

    public abstract void apply(@NotNull BukkitUser user, @NotNull BukkitHuskSync plugin) throws IllegalStateException;

    @Getter
    public static abstract class Items extends BukkitData implements Data.Items {

        private final @Nullable ItemStack @NotNull [] contents;

        private Items(@Nullable ItemStack @NotNull [] contents) {
            this.contents = Arrays.stream(contents.clone())
                    .map(i -> i == null || i.getType() == Material.AIR ? null : i)
                    .toArray(ItemStack[]::new);
        }

        @Nullable
        @Override
        public Stack @NotNull [] getStack() {
            return Arrays.stream(contents)
                    .map(stack -> stack != null ? new Stack(
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
                    .toArray(Stack[]::new);
        }

        @Override
        public void clear() {
            Arrays.fill(contents, null);
        }

        @Override
        public void setContents(@NotNull Data.Items contents) {
            this.setContents(((BukkitData.Items) contents).getContents());
        }

        public void setContents(@Nullable ItemStack @NotNull [] contents) {
            // Ensure the array is the correct length for the inventory
            if (contents.length != this.contents.length) {
                contents = Arrays.copyOf(contents, this.contents.length);
            }
            System.arraycopy(contents, 0, this.contents, 0, this.contents.length);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof BukkitData.Items items) {
                return Arrays.equals(contents, items.getContents());
            }
            return false;
        }

        @Setter
        @Getter
        public static class Inventory extends BukkitData.Items implements Data.Items.Inventory {

            @Range(from = 0, to = 8)
            private int heldItemSlot;

            private Inventory(@Nullable ItemStack @NotNull [] contents, int heldItemSlot) {
                super(contents);
                this.heldItemSlot = heldItemSlot;
            }

            @NotNull
            public static BukkitData.Items.Inventory from(@Nullable ItemStack @NotNull [] contents, int heldItemSlot) {
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
            public void apply(@NotNull BukkitUser user, @NotNull BukkitHuskSync plugin) throws IllegalStateException {
                final Player player = user.getPlayer();
                this.clearInventoryCraftingSlots(player);
                player.setItemOnCursor(null);
                player.getInventory().setContents(plugin.setMapViews(getContents()));
                player.getInventory().setHeldItemSlot(heldItemSlot);
                //noinspection UnstableApiUsage
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

        }

        public static class EnderChest extends BukkitData.Items implements Data.Items.EnderChest {

            private EnderChest(@Nullable ItemStack @NotNull [] contents) {
                super(contents);
            }

            @NotNull
            public static BukkitData.Items.EnderChest adapt(@Nullable ItemStack @NotNull [] contents) {
                return new BukkitData.Items.EnderChest(contents);
            }

            @NotNull
            public static BukkitData.Items.EnderChest adapt(@NotNull Collection<ItemStack> items) {
                return adapt(items.toArray(ItemStack[]::new));
            }

            @NotNull
            public static BukkitData.Items.EnderChest empty() {
                return new BukkitData.Items.EnderChest(new ItemStack[ENDER_CHEST_SLOT_COUNT]);
            }

            @Override
            public void apply(@NotNull BukkitUser user, @NotNull BukkitHuskSync plugin) throws IllegalStateException {
                user.getPlayer().getEnderChest().setContents(plugin.setMapViews(getContents()));
            }

        }

        public static class ItemArray extends BukkitData.Items implements Data.Items {

            private ItemArray(@Nullable ItemStack @NotNull [] contents) {
                super(contents);
            }

            @NotNull
            public static ItemArray adapt(@NotNull Collection<ItemStack> drops) {
                return new ItemArray(drops.toArray(ItemStack[]::new));
            }

            @NotNull
            public static ItemArray adapt(@Nullable ItemStack @NotNull [] drops) {
                return new ItemArray(drops);
            }

            @Override
            public void apply(@NotNull BukkitUser user, @NotNull BukkitHuskSync plugin) throws IllegalStateException {
                throw new UnsupportedOperationException("A generic item array cannot be applied to a player");
            }

        }

    }

    @Getter
    @Setter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class PotionEffects extends BukkitData implements Data.PotionEffects {

        private final Collection<PotionEffect> effects;

        @NotNull
        public static BukkitData.PotionEffects from(@NotNull Collection<PotionEffect> sei) {
            return new BukkitData.PotionEffects(Lists.newArrayList(sei.stream().filter(e -> !e.isAmbient()).toList()));

        }

        @NotNull
        public static BukkitData.PotionEffects adapt(@NotNull Collection<Effect> effects) {
            return from(effects.stream()
                    .map(effect -> {
                        final PotionEffectType type = matchEffectType(effect.type());
                        return type != null ? new PotionEffect(
                                type,
                                effect.duration(),
                                effect.amplifier(),
                                effect.isAmbient(),
                                effect.showParticles(),
                                effect.hasIcon()
                        ) : null;
                    })
                    .filter(Objects::nonNull)
                    .toList());
        }

        @NotNull
        @SuppressWarnings("unused")
        public static BukkitData.PotionEffects empty() {
            return new BukkitData.PotionEffects(Lists.newArrayList());
        }

        @Override
        public void apply(@NotNull BukkitUser user, @NotNull BukkitHuskSync plugin) throws IllegalStateException {
            final Player player = user.getPlayer();
            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
            for (PotionEffect effect : this.getEffects()) {
                player.addPotionEffect(effect);
            }
        }

        @NotNull
        @Override
        @Unmodifiable
        public List<Effect> getActiveEffects() {
            return effects.stream()
                    .map(potionEffect -> new Effect(
                            potionEffect.getType().getKey().toString(),
                            potionEffect.getAmplifier(),
                            potionEffect.getDuration(),
                            potionEffect.isAmbient(),
                            potionEffect.hasParticles(),
                            potionEffect.hasIcon()
                    ))
                    .toList();
        }

    }

    @Getter
    @Setter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Advancements extends BukkitData implements Data.Advancements {

        private List<Advancement> completed;

        // Iterate through the server advancement set and add all advancements to the list
        @NotNull
        public static BukkitData.Advancements adapt(@NotNull Player player) {
            final List<Advancement> advancements = Lists.newArrayList();
            forEachAdvancement(advancement -> {
                final AdvancementProgress advancementProgress = player.getAdvancementProgress(advancement);
                final Map<String, Date> awardedCriteria = Maps.newHashMap();

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
        public void apply(@NotNull BukkitUser user, @NotNull BukkitHuskSync plugin) throws IllegalStateException {
            plugin.runAsync(() -> forEachAdvancement(advancement -> {
                final Player player = user.getPlayer();
                final AdvancementProgress progress = player.getAdvancementProgress(advancement);
                final Optional<Advancement> record = completed.stream()
                        .filter(r -> r.getKey().equals(advancement.getKey().toString()))
                        .findFirst();
                if (record.isEmpty()) {
                    this.setAdvancement(plugin, advancement, player, user, List.of(), progress.getAwardedCriteria());
                    return;
                }

                final Map<String, Date> criteria = record.get().getCompletedCriteria();
                this.setAdvancement(
                        plugin, advancement, player, user,
                        criteria.keySet().stream().filter(key -> !progress.getAwardedCriteria().contains(key)).toList(),
                        progress.getAwardedCriteria().stream().filter(key -> !criteria.containsKey(key)).toList()
                );
            }));
        }

        private void setAdvancement(@NotNull HuskSync plugin,
                                    @NotNull org.bukkit.advancement.Advancement advancement,
                                    @NotNull Player player,
                                    @NotNull BukkitUser user,
                                    @NotNull Collection<String> toAward,
                                    @NotNull Collection<String> toRevoke) {
            plugin.runSync(() -> {
                // Track player exp level & progress
                final int expLevel = player.getLevel();
                final float expProgress = player.getExp();

                // Award and revoke advancement criteria
                final AdvancementProgress progress = player.getAdvancementProgress(advancement);
                toAward.forEach(progress::awardCriteria);
                toRevoke.forEach(progress::revokeCriteria);

                // Set player experience and level (prevent advancement awards applying twice), reset game rule
                if (!toAward.isEmpty()
                    && (player.getLevel() != expLevel || player.getExp() != expProgress)) {
                    player.setLevel(expLevel);
                    player.setExp(expProgress);
                }
            }, user);
        }

        // Performs a consuming function for every advancement registered on the server
        private static void forEachAdvancement(@NotNull ThrowingConsumer<org.bukkit.advancement.Advancement> consumer) {
            Bukkit.getServer().advancementIterator().forEachRemaining(consumer);
        }

    }

    @Getter
    @Setter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Location extends BukkitData implements Data.Location, Adaptable {

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
        public void apply(@NotNull BukkitUser user, @NotNull BukkitHuskSync plugin) throws IllegalStateException {
            try {
                final org.bukkit.Location location = new org.bukkit.Location(
                        Bukkit.getWorld(world.name()), x, y, z, yaw, pitch
                );
                user.getPlayer().teleport(location);
            } catch (Throwable e) {
                throw new IllegalStateException("Failed to apply location", e);
            }
        }

    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Statistics extends BukkitData implements Data.Statistics, Adaptable {

        @SerializedName("generic")
        private Map<String, Integer> genericStatistics;
        @SerializedName("blocks")
        private Map<String, Map<String, Integer>> blockStatistics;
        @SerializedName("items")
        private Map<String, Map<String, Integer>> itemStatistics;
        @SerializedName("entities")
        private Map<String, Map<String, Integer>> entityStatistics;

        @NotNull
        public static BukkitData.Statistics adapt(@NotNull Player player) {
            final Map<String, Integer> generic = Maps.newHashMap();
            final Map<String, Map<String, Integer>> blocks = Maps.newHashMap(),
                    items = Maps.newHashMap(), entities = Maps.newHashMap();
            Registry.STATISTIC.forEach(id -> {
                switch (id.getType()) {
                    case UNTYPED -> addStatistic(player, id, generic);
                    // Todo - Future - Use BLOCK and ITEM registries when API stabilizes
                    case BLOCK -> addStatistic(player, id, Registry.MATERIAL, blocks);
                    case ITEM -> addStatistic(player, id, Registry.MATERIAL, items);
                    case ENTITY -> addStatistic(player, id, Registry.ENTITY_TYPE, entities);
                }
            });
            return new BukkitData.Statistics(generic, blocks, items, entities);
        }

        @NotNull
        public static BukkitData.Statistics from(@NotNull Map<String, Integer> generic,
                                                 @NotNull Map<String, Map<String, Integer>> blocks,
                                                 @NotNull Map<String, Map<String, Integer>> items,
                                                 @NotNull Map<String, Map<String, Integer>> entities) {
            return new BukkitData.Statistics(generic, blocks, items, entities);
        }

        private static void addStatistic(@NotNull Player p, @NotNull Statistic id, @NotNull Map<String, Integer> map) {
            final int stat = p.getStatistic(id);
            if (stat != 0) {
                map.put(id.getKey().getKey(), stat);
            }
        }

        private static <R extends Keyed> void addStatistic(@NotNull Player p, @NotNull Statistic id,
                                                           @NotNull Registry<R> registry,
                                                           @NotNull Map<String, Map<String, Integer>> map) {
            registry.forEach(i -> {
                try {
                    final int stat = i instanceof Material m ? p.getStatistic(id, m) :
                            (i instanceof EntityType e ? p.getStatistic(id, e) : -1);
                    if (stat != 0) {
                        map.compute(id.getKey().getKey(), (k, v) -> v == null ? Maps.newHashMap() : v)
                                .put(i.getKey().getKey(), stat);
                    }
                } catch (IllegalStateException ignored) {
                }
            });
        }

        @Override
        public void apply(@NotNull BukkitUser user, @NotNull BukkitHuskSync p) {
            genericStatistics.forEach((k, v) -> applyStat(p, user, k, Statistic.Type.UNTYPED, v));
            blockStatistics.forEach((k, m) -> m.forEach((b, v) -> applyStat(p, user, k, Statistic.Type.BLOCK, v, b)));
            itemStatistics.forEach((k, m) -> m.forEach((i, v) -> applyStat(p, user, k, Statistic.Type.ITEM, v, i)));
            entityStatistics.forEach((k, m) -> m.forEach((e, v) -> applyStat(p, user, k, Statistic.Type.ENTITY, v, e)));
        }

        private void applyStat(@NotNull HuskSync plugin, @NotNull UserDataHolder user, @NotNull String id,
                               @NotNull Statistic.Type type, int value, @NotNull String... key) {
            final Player player = ((BukkitUser) user).getPlayer();
            final Statistic stat = matchStatistic(id);
            if (stat == null) {
                return;
            }

            try {
                switch (type) {
                    case UNTYPED -> player.setStatistic(stat, value);
                    case BLOCK, ITEM -> player.setStatistic(stat, Objects.requireNonNull(matchMaterial(key[0])), value);
                    case ENTITY -> player.setStatistic(stat, Objects.requireNonNull(matchEntityType(key[0])), value);
                }
            } catch (Throwable a) {
                plugin.log(Level.WARNING, "Failed to apply statistic " + id, a);
            }
        }

    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class PersistentData extends BukkitData implements Data.PersistentData {
        private final NBTCompound persistentData;

        @NotNull
        public static BukkitData.PersistentData adapt(@NotNull PersistentDataContainer persistentData) {
            return new BukkitData.PersistentData(new NBTPersistentDataContainer(persistentData));
        }

        @NotNull
        public static BukkitData.PersistentData from(@NotNull NBTCompound compound) {
            return new BukkitData.PersistentData(compound);
        }

        @Override
        public void apply(@NotNull BukkitUser user, @NotNull BukkitHuskSync plugin) throws IllegalStateException {
            final NBTPersistentDataContainer container = new NBTPersistentDataContainer(
                    user.getPlayer().getPersistentDataContainer()
            );
            container.clearNBT();
            container.mergeCompound(persistentData);
        }

    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @SuppressWarnings("UnstableApiUsage")
    public static class Attributes extends BukkitData implements Data.Attributes, Adaptable {

        private List<Attribute> attributes;

        @NotNull
        public static BukkitData.Attributes adapt(@NotNull Player player, @NotNull HuskSync plugin) {
            final List<Attribute> attributes = Lists.newArrayList();
            final AttributeSettings settings = plugin.getSettings().getSynchronization().getAttributes();
            Registry.ATTRIBUTE.forEach(id -> {
                final AttributeInstance instance = player.getAttribute(id);
                if (settings.isIgnoredAttribute(id.getKey().toString()) || instance == null) {
                    return; // We don't sync attributes not marked as to be synced
                }
                attributes.add(adapt(instance, settings));
            });
            return new BukkitData.Attributes(attributes);
        }

        public Optional<Attribute> getAttribute(@NotNull org.bukkit.attribute.Attribute id) {
            return attributes.stream().filter(attribute -> attribute.name().equals(id.getKey().toString())).findFirst();
        }

        @SuppressWarnings("unused")
        public Optional<Attribute> getAttribute(@NotNull String key) {
            final org.bukkit.attribute.Attribute attribute = matchAttribute(key);
            if (attribute == null) {
                return Optional.empty();
            }
            return getAttribute(attribute);
        }

        @NotNull
        private static Attribute adapt(@NotNull AttributeInstance instance, @NotNull AttributeSettings settings) {
            return new Attribute(
                    instance.getAttribute().getKey().toString(),
                    instance.getBaseValue(),
                    instance.getModifiers().stream()
                            .filter(modifier -> !settings.isIgnoredModifier(modifier.getName()))
                            .filter(modifier -> modifier.getSlotGroup() != EquipmentSlotGroup.ANY)
                            .map(BukkitData.Attributes::adapt).collect(Collectors.toSet())
            );
        }

        @NotNull
        private static Modifier adapt(@NotNull AttributeModifier modifier) {
            return new Modifier(
                    modifier.getKey().toString(),
                    modifier.getAmount(),
                    modifier.getOperation().ordinal(),
                    modifier.getSlotGroup().toString()
            );
        }

        private static void applyAttribute(@Nullable AttributeInstance instance, @Nullable Attribute attribute) {
            if (instance == null) {
                return;
            }
            instance.getModifiers().forEach(instance::removeModifier);
            instance.setBaseValue(attribute == null ? instance.getValue() : attribute.baseValue());
            if (attribute != null) {
                attribute.modifiers().stream()
                        .filter(mod -> instance.getModifiers().stream().map(AttributeModifier::getName)
                                .noneMatch(n -> n.equals(mod.name())))
                        .distinct().filter(mod -> !mod.hasUuid())
                        .forEach(mod -> instance.addModifier(adapt(mod)));
            }
        }

        @NotNull
        private static AttributeModifier adapt(@NotNull Modifier modifier) {
            return new AttributeModifier(
                    Objects.requireNonNull(NamespacedKey.fromString(modifier.name())),
                    modifier.amount(),
                    AttributeModifier.Operation.values()[modifier.operation()],
                    Optional.ofNullable(EquipmentSlotGroup.getByName(modifier.slotGroup())).orElse(EquipmentSlotGroup.ANY)
            );
        }

        @Override
        public void apply(@NotNull BukkitUser user, @NotNull BukkitHuskSync plugin) throws IllegalStateException {
            final AttributeSettings settings = plugin.getSettings().getSynchronization().getAttributes();
            Registry.ATTRIBUTE.forEach(id -> {
                if (settings.isIgnoredAttribute(id.getKey().toString())) {
                    return;
                }
                applyAttribute(user.getPlayer().getAttribute(id), getAttribute(id).orElse(null));
            });
        }

    }

    @Getter
    @Setter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Health extends BukkitData implements Data.Health, Adaptable {
        @SerializedName("health")
        private double health;
        @SerializedName("health_scale")
        private double healthScale;
        @SerializedName("is_health_scaled")
        private boolean isHealthScaled;

        @NotNull
        public static BukkitData.Health from(double health, double scale, boolean isScaled) {
            return new BukkitData.Health(health, scale, isScaled);
        }

        /**
         * @deprecated Use {@link #from(double, double, boolean)} instead
         */
        @NotNull
        @Deprecated(since = "3.5.4")
        public static BukkitData.Health from(double health, double scale) {
            return from(health, scale, false);
        }

        /**
         * @deprecated Use {@link #from(double, double, boolean)} instead
         */
        @NotNull
        @Deprecated(forRemoval = true, since = "3.5")
        public static BukkitData.Health from(double health, @SuppressWarnings("unused") double max, double scale) {
            return from(health, scale, false);
        }

        @NotNull
        public static BukkitData.Health adapt(@NotNull Player player) {
            return from(
                    player.getHealth(),
                    player.getHealthScale(),
                    player.isHealthScaled()
            );
        }

        @Override
        @SuppressWarnings("deprecation")
        public void apply(@NotNull BukkitUser user, @NotNull BukkitHuskSync plugin) throws IllegalStateException {
            final Player player = user.getPlayer();

            // Set health
            try {
                player.setHealth(Math.min(health, player.getMaxHealth()));
            } catch (Throwable e) {
                plugin.log(Level.WARNING, "Error setting %s's health to %s".formatted(player.getName(), health), e);
            }

            // Set health scale
            double scale = healthScale <= 0 ? player.getMaxHealth() : healthScale;
            try {
                player.setHealthScale(scale);
                player.setHealthScaled(isHealthScaled);
            } catch (Throwable e) {
                plugin.log(Level.WARNING, "Error setting %s's health scale to %s".formatted(player.getName(), scale), e);
            }
        }

    }

    @Getter
    @Setter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Hunger extends BukkitData implements Data.Hunger, Adaptable {

        @SerializedName("food_level")
        private int foodLevel;
        @SerializedName("saturation")
        private float saturation;
        @SerializedName("exhaustion")
        private float exhaustion;

        @NotNull
        public static BukkitData.Hunger adapt(@NotNull Player player) {
            return from(player.getFoodLevel(), player.getSaturation(), player.getExhaustion());
        }

        @NotNull
        public static BukkitData.Hunger from(int foodLevel, float saturation, float exhaustion) {
            return new BukkitData.Hunger(foodLevel, saturation, exhaustion);
        }

        @Override
        public void apply(@NotNull BukkitUser user, @NotNull BukkitHuskSync plugin) throws IllegalStateException {
            final Player player = user.getPlayer();
            player.setFoodLevel(foodLevel);
            player.setSaturation(saturation);
            player.setExhaustion(exhaustion);
        }

    }

    @Getter
    @Setter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Experience extends BukkitData implements Data.Experience, Adaptable {

        @SerializedName("total_experience")
        private int totalExperience;

        @SerializedName("exp_level")
        private int expLevel;

        @SerializedName("exp_progress")
        private float expProgress;

        @NotNull
        public static BukkitData.Experience from(int totalExperience, int expLevel, float expProgress) {
            return new BukkitData.Experience(totalExperience, expLevel, expProgress);
        }

        @NotNull
        public static BukkitData.Experience adapt(@NotNull Player player) {
            return from(player.getTotalExperience(), player.getLevel(), player.getExp());
        }

        @Override
        public void apply(@NotNull BukkitUser user, @NotNull BukkitHuskSync plugin) throws IllegalStateException {
            final Player player = user.getPlayer();
            player.setTotalExperience(totalExperience);
            player.setLevel(expLevel);
            player.setExp(expProgress);
        }

    }

    @Getter
    @Setter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class GameMode extends BukkitData implements Data.GameMode, Adaptable {

        @SerializedName("game_mode")
        private String gameMode;

        @NotNull
        public static BukkitData.GameMode from(@NotNull String gameMode) {
            return new BukkitData.GameMode(gameMode);
        }

        @NotNull
        @Deprecated(forRemoval = true, since = "3.5")
        @SuppressWarnings("unused")
        public static BukkitData.GameMode from(@NotNull String gameMode, boolean allowFlight, boolean isFlying) {
            return new BukkitData.GameMode(gameMode);
        }

        @NotNull
        public static BukkitData.GameMode adapt(@NotNull Player player) {
            return from(player.getGameMode().name());
        }

        @Override
        public void apply(@NotNull BukkitUser user, @NotNull BukkitHuskSync plugin) throws IllegalStateException {
            user.getPlayer().setGameMode(org.bukkit.GameMode.valueOf(gameMode));
        }

    }

    @Getter
    @Setter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class FlightStatus extends BukkitData implements Data.FlightStatus, Adaptable {

        @SerializedName("allow_flight")
        private boolean allowFlight;
        @SerializedName("is_flying")
        private boolean flying;

        @NotNull
        public static BukkitData.FlightStatus from(boolean allowFlight, boolean flying) {
            return new BukkitData.FlightStatus(allowFlight, allowFlight && flying);
        }

        @NotNull
        public static BukkitData.FlightStatus adapt(@NotNull Player player) {
            return from(player.getAllowFlight(), player.isFlying());
        }

        @Override
        public void apply(@NotNull BukkitUser user, @NotNull BukkitHuskSync plugin) throws IllegalStateException {
            final Player player = user.getPlayer();
            player.setAllowFlight(allowFlight);
            player.setFlying(allowFlight && flying);
        }

    }

}
