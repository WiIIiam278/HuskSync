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
import com.google.common.collect.Sets;
import com.google.gson.annotations.SerializedName;
import lombok.*;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.StatType;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.william278.desertwell.util.ThrowingConsumer;
import net.william278.husksync.FabricHuskSync;
import net.william278.husksync.HuskSync;
import net.william278.husksync.adapter.Adaptable;
import net.william278.husksync.user.FabricUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

import static net.william278.husksync.util.FabricKeyedAdapter.*;

public abstract class FabricData implements Data {

    @Override
    public void apply(@NotNull UserDataHolder user, @NotNull HuskSync plugin) {
        this.apply((FabricUser) user, (FabricHuskSync) plugin);
    }

    protected abstract void apply(@NotNull FabricUser user, @NotNull FabricHuskSync plugin);

    @Getter
    public static abstract class Items extends FabricData implements Data.Items {

        private final @Nullable ItemStack @NotNull [] contents;

        private Items(@Nullable ItemStack @NotNull [] contents) {
            this.contents = Arrays.stream(contents.clone())
                    .map(i -> i == null || i.isEmpty() ? null : i)
                    .toArray(ItemStack[]::new);
        }

        @Nullable
        @Override
        public Stack @NotNull [] getStack() {
            return Arrays.stream(contents)
                    .map(stack -> stack != null ? new Stack(
                            stack.getItem().toString(),
                            stack.getCount(),
                            stack.getName().getString(),
                            Optional.ofNullable(stack.getSubNbt(ItemStack.DISPLAY_KEY))
                                    .flatMap(display -> Optional.ofNullable(display.get(ItemStack.LORE_KEY))
                                            .map(lore -> ((List<String>) lore).stream().toList())) //todo check this is ok
                                    .orElse(null),
                            stack.getEnchantments().stream()
                                    .map(element -> EnchantmentHelper.getIdFromNbt((NbtCompound) element))
                                    .filter(Objects::nonNull).map(Identifier::toString)
                                    .toList()
                    ) : null)
                    .toArray(Stack[]::new);
        }

        @Override
        public void clear() {
            Arrays.fill(contents, null);
        }

        @Override
        public void setContents(@NotNull Data.Items contents) {
            this.setContents(((FabricData.Items) contents).getContents());
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
            if (obj instanceof FabricData.Items items) {
                return Arrays.equals(contents, items.getContents());
            }
            return false;
        }

        @Setter
        @Getter
        public static class Inventory extends FabricData.Items implements Data.Items.Inventory {

            @Range(from = 0, to = 8)
            private int heldItemSlot;

            public Inventory(@Nullable ItemStack @NotNull [] contents, int heldItemSlot) {
                super(contents);
                this.heldItemSlot = heldItemSlot;
            }

            @NotNull
            public static FabricData.Items.Inventory from(@Nullable ItemStack @NotNull [] contents, int heldItemSlot) {
                return new FabricData.Items.Inventory(contents, heldItemSlot);
            }

            @NotNull
            public static FabricData.Items.Inventory from(@NotNull Collection<ItemStack> contents, int heldItemSlot) {
                return from(contents.toArray(ItemStack[]::new), heldItemSlot);
            }

            @NotNull
            public static FabricData.Items.Inventory empty() {
                return new FabricData.Items.Inventory(new ItemStack[INVENTORY_SLOT_COUNT], 0);
            }

            @Override
            public int getSlotCount() {
                return getContents().length;
            }

            @Override
            public void apply(@NotNull FabricUser user, @NotNull FabricHuskSync plugin) throws IllegalStateException {
                final ServerPlayerEntity player = user.getPlayer();
                player.playerScreenHandler.clearCraftingSlots();
                player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
                final ItemStack[] items = getContents();
                for (int slot = 0; slot < player.getInventory().size(); slot++) {
                    player.getInventory().setStack(slot, items[slot] == null ? ItemStack.EMPTY : items[slot]);
                }
                player.getInventory().selectedSlot = heldItemSlot;
                player.playerScreenHandler.sendContentUpdates();
                player.getInventory().updateItems();
            }

        }

        public static class EnderChest extends FabricData.Items implements Data.Items.EnderChest {

            private EnderChest(@Nullable ItemStack @NotNull [] contents) {
                super(contents);
            }

            @NotNull
            public static FabricData.Items.EnderChest adapt(@Nullable ItemStack @NotNull [] contents) {
                return new FabricData.Items.EnderChest(contents);
            }

            @NotNull
            public static FabricData.Items.EnderChest adapt(@NotNull Collection<ItemStack> items) {
                return adapt(items.toArray(ItemStack[]::new));
            }

            @NotNull
            public static FabricData.Items.EnderChest empty() {
                return new FabricData.Items.EnderChest(new ItemStack[ENDER_CHEST_SLOT_COUNT]);
            }

            @Override
            public void apply(@NotNull FabricUser user, @NotNull FabricHuskSync plugin) throws IllegalStateException {
                final ItemStack[] items = getContents();
                for (int slot = 0; slot < user.getPlayer().getEnderChestInventory().size(); slot++) {
                    user.getPlayer().getEnderChestInventory().setStack(
                            slot, items[slot] == null ? ItemStack.EMPTY : items[slot]
                    );
                }
            }

        }

        public static class ItemArray extends FabricData.Items implements Data.Items {

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
            public void apply(@NotNull FabricUser user, @NotNull FabricHuskSync plugin) throws IllegalStateException {
                throw new UnsupportedOperationException("A generic item array cannot be applied to a player");
            }

        }

    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class PotionEffects extends FabricData implements Data.PotionEffects {

        private final Collection<StatusEffectInstance> effects;

        @NotNull
        public static FabricData.PotionEffects from(@NotNull Collection<StatusEffectInstance> sei) {
            return new FabricData.PotionEffects(Lists.newArrayList(sei.stream().filter(e -> !e.isAmbient()).toList()));
        }

        @NotNull
        public static FabricData.PotionEffects adapt(@NotNull Collection<Effect> effects) {
            return from(effects.stream()
                    .map(effect -> {
                        final StatusEffect type = matchEffectType(effect.type());
                        return type != null ? new StatusEffectInstance(
                                type,
                                effect.duration(),
                                effect.amplifier(),
                                effect.isAmbient(),
                                effect.showParticles(),
                                effect.hasIcon()
                        ) : null;
                    })
                    .filter(Objects::nonNull)
                    .toList()
            );
        }

        @NotNull
        @SuppressWarnings("unused")
        public static FabricData.PotionEffects empty() {
            return new FabricData.PotionEffects(Lists.newArrayList());
        }

        @Override
        public void apply(@NotNull FabricUser user, @NotNull FabricHuskSync plugin) throws IllegalStateException {
            final ServerPlayerEntity player = user.getPlayer();
            final List<StatusEffect> effectsToRemove = player.getActiveStatusEffects().entrySet().stream()
                    .filter(e -> !e.getValue().isAmbient()).map(Map.Entry::getKey).toList();
            effectsToRemove.forEach(player::removeStatusEffect);
            getEffects().forEach(player::addStatusEffect);
        }

        @NotNull
        @Override
        @Unmodifiable
        public List<Effect> getActiveEffects() {
            return effects.stream()
                    .map(potionEffect -> {
                        final String key = getEffectId(potionEffect.getEffectType());
                        return key != null ? new Effect(
                                key,
                                potionEffect.getAmplifier(),
                                potionEffect.getDuration(),
                                potionEffect.isAmbient(),
                                potionEffect.shouldShowParticles(),
                                potionEffect.shouldShowIcon()
                        ) : null;
                    })
                    .filter(Objects::nonNull)
                    .toList();
        }

    }

    @Getter
    @Setter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Advancements extends FabricData implements Data.Advancements {

        private List<Advancement> completed;

        @NotNull
        public static FabricData.Advancements adapt(@NotNull ServerPlayerEntity player) {
            final MinecraftServer server = Objects.requireNonNull(player.getServer(), "Server is null");
            final List<Advancement> advancements = Lists.newArrayList();
            forEachAdvancement(server, advancement -> {
                final AdvancementProgress advancementProgress = player.getAdvancementTracker().getProgress(advancement);
                final Map<String, Date> awardedCriteria = Maps.newHashMap();

                advancementProgress.getObtainedCriteria().forEach((criteria) -> awardedCriteria.put(criteria,
                        advancementProgress.getEarliestProgressObtainDate()));

                // Only save the advancement if criteria has been completed
                if (!awardedCriteria.isEmpty()) {
                    advancements.add(Advancement.adapt(advancement.getId().toString(), awardedCriteria));
                }
            });
            return new FabricData.Advancements(advancements);
        }

        @NotNull
        public static FabricData.Advancements from(@NotNull List<Advancement> advancements) {
            return new FabricData.Advancements(advancements);
        }

        @Override
        public void apply(@NotNull FabricUser user, @NotNull FabricHuskSync plugin) throws IllegalStateException {
            final ServerPlayerEntity player = user.getPlayer();
            final MinecraftServer server = Objects.requireNonNull(player.getServer(), "Server is null");
            plugin.runAsync(() -> forEachAdvancement(server, advancement -> {
                final AdvancementProgress progress = player.getAdvancementTracker().getProgress(advancement);
                final Optional<Advancement> record = completed.stream()
                        .filter(r -> r.getKey().equals(advancement.getId().toString()))
                        .findFirst();
                if (record.isEmpty()) {
                    return;
                }

                final Map<String, Date> criteria = record.get().getCompletedCriteria();
                final List<String> awarded = Lists.newArrayList(progress.getObtainedCriteria());
                this.setAdvancement(
                        plugin, advancement, player, user,
                        criteria.keySet().stream().filter(key -> !awarded.contains(key)).toList(),
                        awarded.stream().filter(key -> !criteria.containsKey(key)).toList()
                );
            }));
        }

        private void setAdvancement(@NotNull FabricHuskSync plugin,
                                    @NotNull net.minecraft.advancement.Advancement advancement,
                                    @NotNull ServerPlayerEntity player,
                                    @NotNull FabricUser user,
                                    @NotNull List<String> toAward,
                                    @NotNull List<String> toRevoke) {
            plugin.runSync(() -> {
                // Track player exp level & progress
                final int expLevel = player.experienceLevel;
                final float expProgress = player.experienceProgress;

                // Award and revoke advancement criteria
                final PlayerAdvancementTracker progress = player.getAdvancementTracker();
                toAward.forEach(a -> progress.grantCriterion(advancement, a));
                toRevoke.forEach(r -> progress.revokeCriterion(advancement, r));

                // Restore player exp level & progress
                if (!toAward.isEmpty()
                    && (player.experienceLevel != expLevel || player.experienceProgress != expProgress)) {
                    player.setExperienceLevel(expLevel);
                    player.setExperiencePoints((int) (player.getNextLevelExperience() * expProgress));
                }
            });
        }

        // Performs a consuming function for every advancement registered on the server
        private static void forEachAdvancement(@NotNull MinecraftServer server,
                                               @NotNull ThrowingConsumer<net.minecraft.advancement.Advancement> con) {
            server.getAdvancementLoader().getAdvancements().forEach(con);
        }

    }

    @Getter
    @Setter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Location extends FabricData implements Data.Location, Adaptable {
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
        public static FabricData.Location from(double x, double y, double z,
                                               float yaw, float pitch, @NotNull World world) {
            return new FabricData.Location(x, y, z, yaw, pitch, world);
        }

        @NotNull
        public static FabricData.Location adapt(@NotNull ServerPlayerEntity player) {
            return from(
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    player.getYaw(),
                    player.getPitch(),
                    new World(
                            Objects.requireNonNull(
                                    player.getWorld(), "World is null"
                            ).getRegistryKey().getValue().toString(),
                            UUID.nameUUIDFromBytes(
                                    player.getWorld().getDimensionKey().getValue().toString().getBytes()
                            ),
                            player.getWorld().getDimensionKey().getValue().toString()
                    )
            );
        }

        @Override
        public void apply(@NotNull FabricUser user, @NotNull FabricHuskSync plugin) throws IllegalStateException {
            final ServerPlayerEntity player = user.getPlayer();
            final MinecraftServer server = plugin.getMinecraftServer();
            try {
                player.dismountVehicle();
                FabricDimensions.teleport(
                        player,
                        server.getWorld(server.getWorldRegistryKeys().stream()
                                .filter(key -> key.getValue().equals(Identifier.tryParse(world.name())))
                                .findFirst().orElseThrow(
                                        () -> new IllegalStateException("Invalid world")
                                )),
                        new TeleportTarget(
                                new Vec3d(x, y, z),
                                Vec3d.ZERO,
                                yaw,
                                pitch
                        )
                );
            } catch (Throwable e) {
                throw new IllegalStateException("Failed to apply location", e);
            }
        }

    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Statistics extends FabricData implements Data.Statistics, Adaptable {

        private static final String BLOCK_STAT_TYPE = "block";
        private static final String ITEM_STAT_TYPE = "item";
        private static final String ENTITY_STAT_TYPE = "entity_type";

        @SerializedName("generic")
        private Map<String, Integer> genericStatistics;
        @SerializedName("blocks")
        private Map<String, Map<String, Integer>> blockStatistics;
        @SerializedName("items")
        private Map<String, Map<String, Integer>> itemStatistics;
        @SerializedName("entities")
        private Map<String, Map<String, Integer>> entityStatistics;

        @NotNull
        public static FabricData.Statistics adapt(@NotNull ServerPlayerEntity player) throws IllegalStateException {
            // Adapt typed stats
            final Map<String, Map<String, Integer>> blocks = Maps.newHashMap(),
                    items = Maps.newHashMap(), entities = Maps.newHashMap();
            Registries.STAT_TYPE.getEntrySet().forEach(stat -> {
                final Registry<?> registry = stat.getValue().getRegistry();

                final String registryId = registry.getKey().getValue().getPath();
                if (registryId.equals("custom_stat")) {
                    return;
                }
                final Map<String, Integer> map = (switch (registryId) {
                    case BLOCK_STAT_TYPE -> blocks;
                    case ITEM_STAT_TYPE -> items;
                    case ENTITY_STAT_TYPE -> entities;
                    default -> throw new IllegalStateException("Unexpected value: %s".formatted(registryId));
                }).compute(stat.getKey().getValue().toString(), (k, v) -> v == null ? Maps.newHashMap() : v);

                registry.getEntrySet().forEach(entry -> {
                    @SuppressWarnings({"unchecked", "rawtypes"}) final int value = player.getStatHandler()
                            .getStat((StatType) stat.getValue(), entry.getValue());
                    if (value != 0) {
                        map.put(entry.getKey().getValue().toString(), value);
                    }
                });
            });

            // Add generic stats
            final Map<String, Integer> generic = Maps.newHashMap();
            Registries.CUSTOM_STAT.getEntrySet().forEach(stat -> {
                final int value = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(stat.getValue()));
                if (value != 0) {
                    generic.put(stat.getKey().getValue().toString(), value);
                }
            });

            return new FabricData.Statistics(generic, blocks, items, entities);
        }

        @NotNull
        public static FabricData.Statistics from(@NotNull Map<String, Integer> generic,
                                                 @NotNull Map<String, Map<String, Integer>> blocks,
                                                 @NotNull Map<String, Map<String, Integer>> items,
                                                 @NotNull Map<String, Map<String, Integer>> entities) {
            return new FabricData.Statistics(generic, blocks, items, entities);
        }

        @Override
        public void apply(@NotNull FabricUser user, @NotNull FabricHuskSync plugin) {
            final ServerPlayerEntity player = user.getPlayer();
            genericStatistics.forEach((id, v) -> applyStat(player, id, null, v));
            blockStatistics.forEach((id, m) -> m.forEach((b, v) -> applyStat(player, id, BLOCK_STAT_TYPE, v, b)));
            itemStatistics.forEach((id, m) -> m.forEach((i, v) -> applyStat(player, id, ITEM_STAT_TYPE, v, i)));
            entityStatistics.forEach((id, m) -> m.forEach((e, v) -> applyStat(player, id, ENTITY_STAT_TYPE, v, e)));
            player.getStatHandler().updateStatSet();
            player.getStatHandler().sendStats(player);
        }

        @SuppressWarnings("unchecked")
        private <T> void applyStat(@NotNull ServerPlayerEntity player, @NotNull String id,
                                   @Nullable String type, int value, @NotNull String... key) {
            final Identifier statId = Identifier.tryParse(id);
            if (statId == null) {
                return;
            }
            if (type == null) {
                player.getStatHandler().setStat(
                        player,
                        Stats.CUSTOM.getOrCreateStat(Registries.CUSTOM_STAT.get(statId)),
                        value
                );
                return;
            }
            final Identifier typeId = Identifier.tryParse(type);
            final StatType<T> statType = (StatType<T>) Registries.STAT_TYPE.get(typeId);
            if (statType == null) {
                return;
            }

            final Registry<T> typeReg = statType.getRegistry();
            final T typeInstance = typeReg.get(Identifier.tryParse(key[0]));
            if (typeInstance == null) {
                return;
            }

            player.getStatHandler().setStat(player, statType.getOrCreateStat(typeInstance), value);
        }

    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Attributes extends FabricData implements Data.Attributes, Adaptable {

        private List<Attribute> attributes;

        @NotNull
        public static FabricData.Attributes adapt(@NotNull ServerPlayerEntity player, @NotNull HuskSync plugin) {
            final List<Attribute> attributes = Lists.newArrayList();
            Registries.ATTRIBUTE.forEach(id -> {
                final EntityAttributeInstance instance = player.getAttributeInstance(id);
                final Identifier key = Registries.ATTRIBUTE.getId(id);
                if (instance == null || key == null) {
                    return;
                }
                final Set<Modifier> modifiers = Sets.newHashSet();
                instance.getModifiers().forEach(modifier -> modifiers.add(new Modifier(
                        modifier.getId(),
                        modifier.getName(),
                        modifier.getValue(),
                        modifier.getOperation().getId(),
                        -1
                )));
                attributes.add(new Attribute(
                        key.toString(),
                        instance.getBaseValue(),
                        modifiers
                ));
            });
            return new FabricData.Attributes(attributes);
        }

        public Optional<Attribute> getAttribute(@NotNull EntityAttribute id) {
            return Optional.ofNullable(Registries.ATTRIBUTE.getId(id)).map(Identifier::toString)
                    .flatMap(key -> attributes.stream().filter(attribute -> attribute.name().equals(key)).findFirst());
        }

        @SuppressWarnings("unused")
        public Optional<Attribute> getAttribute(@NotNull String key) {
            final EntityAttribute attribute = matchAttribute(key);
            if (attribute == null) {
                return Optional.empty();
            }
            return getAttribute(attribute);
        }

        @Override
        protected void apply(@NotNull FabricUser user, @NotNull FabricHuskSync plugin) {
            Registries.ATTRIBUTE.forEach(id -> applyAttribute(
                    user.getPlayer().getAttributeInstance(id),
                    getAttribute(id).orElse(null)
            ));

        }

        private static void applyAttribute(@Nullable EntityAttributeInstance instance,
                                           @Nullable Attribute attribute) {
            if (instance == null) {
                return;
            }
            instance.setBaseValue(attribute == null ? instance.getAttribute().getDefaultValue() : attribute.baseValue());
            instance.getModifiers().forEach(instance::removeModifier);
            if (attribute != null) {
                attribute.modifiers().forEach(modifier -> instance.addPersistentModifier(new EntityAttributeModifier(
                        modifier.uuid(),
                        modifier.name(),
                        modifier.amount(),
                        EntityAttributeModifier.Operation.fromId(modifier.operationType())
                )));
            }
        }

    }

    @Getter
    @Setter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Health extends FabricData implements Data.Health, Adaptable {
        @SerializedName("health")
        private double health;
        @SerializedName("health_scale")
        private double healthScale;
        @SerializedName("is_health_scaled")
        private boolean isHealthScaled;


        @NotNull
        public static FabricData.Health from(double health, double scale, boolean isScaled) {
            return new FabricData.Health(health, scale, isScaled);
        }

        @NotNull
        public static FabricData.Health adapt(@NotNull ServerPlayerEntity player) {
            return from(
                    player.getHealth(),
                    20.0f, false // Health scale is a Bukkit API feature, not used in Fabric
            );
        }

        @Override
        public void apply(@NotNull FabricUser user, @NotNull FabricHuskSync plugin) throws IllegalStateException {
            final ServerPlayerEntity player = user.getPlayer();
            player.setHealth((float) health);
        }

    }


    @Getter
    @Setter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Hunger extends FabricData implements Data.Hunger, Adaptable {

        @SerializedName("food_level")
        private int foodLevel;
        @SerializedName("saturation")
        private float saturation;
        @SerializedName("exhaustion")
        private float exhaustion;

        @NotNull
        public static FabricData.Hunger adapt(@NotNull ServerPlayerEntity player) {
            final HungerManager hunger = player.getHungerManager();
            return from(hunger.getFoodLevel(), hunger.getSaturationLevel(), hunger.getExhaustion());
        }

        @NotNull
        public static FabricData.Hunger from(int foodLevel, float saturation, float exhaustion) {
            return new FabricData.Hunger(foodLevel, saturation, exhaustion);
        }

        @Override
        public void apply(@NotNull FabricUser user, @NotNull FabricHuskSync plugin) throws IllegalStateException {
            final ServerPlayerEntity player = user.getPlayer();
            final HungerManager hunger = player.getHungerManager();
            hunger.setFoodLevel(foodLevel);
            hunger.setSaturationLevel(saturation);
            hunger.setExhaustion(exhaustion);
        }

    }

    @Getter
    @Setter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Experience extends FabricData implements Data.Experience, Adaptable {

        @SerializedName("total_experience")
        private int totalExperience;

        @SerializedName("exp_level")
        private int expLevel;

        @SerializedName("exp_progress")
        private float expProgress;

        @NotNull
        public static FabricData.Experience from(int totalExperience, int expLevel, float expProgress) {
            return new FabricData.Experience(totalExperience, expLevel, expProgress);
        }

        @NotNull
        public static FabricData.Experience adapt(@NotNull ServerPlayerEntity player) {
            return from(player.totalExperience, player.experienceLevel, player.experienceProgress);
        }

        @Override
        public void apply(@NotNull FabricUser user, @NotNull FabricHuskSync plugin) throws IllegalStateException {
            final ServerPlayerEntity player = user.getPlayer();
            player.totalExperience = totalExperience;
            player.setExperienceLevel(expLevel);
            player.setExperiencePoints((int) (player.getNextLevelExperience() * expProgress));
        }

    }

    @Getter
    @Setter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class GameMode extends FabricData implements Data.GameMode, Adaptable {

        @SerializedName("game_mode")
        private String gameMode;

        @NotNull
        public static FabricData.GameMode from(@NotNull String gameMode) {
            return new FabricData.GameMode(gameMode);
        }

        @NotNull
        public static FabricData.GameMode adapt(@NotNull ServerPlayerEntity player) {
            return from(player.interactionManager.getGameMode().asString());
        }

        @Override
        public void apply(@NotNull FabricUser user, @NotNull FabricHuskSync plugin) throws IllegalStateException {
            user.getPlayer().changeGameMode(net.minecraft.world.GameMode.byName(gameMode));
        }

    }

    @Getter
    @Setter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class FlightStatus extends FabricData implements Data.FlightStatus, Adaptable {

        @SerializedName("allow_flight")
        private boolean allowFlight;
        @SerializedName("is_flying")
        private boolean flying;

        @NotNull
        public static FabricData.FlightStatus from(boolean allowFlight, boolean flying) {
            return new FabricData.FlightStatus(allowFlight, allowFlight && flying);
        }

        @NotNull
        public static FabricData.FlightStatus adapt(@NotNull ServerPlayerEntity player) {
            return from(player.getAbilities().allowFlying, player.getAbilities().flying);
        }

        @Override
        public void apply(@NotNull FabricUser user, @NotNull FabricHuskSync plugin) throws IllegalStateException {
            final ServerPlayerEntity player = user.getPlayer();
            player.getAbilities().allowFlying = allowFlight;
            player.getAbilities().flying = allowFlight && flying;
            player.sendAbilitiesUpdate();
        }

    }

}
