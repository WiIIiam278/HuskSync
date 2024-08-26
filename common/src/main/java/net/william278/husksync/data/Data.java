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

import com.google.common.collect.Sets;
import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.kyori.adventure.key.Key;
import net.william278.husksync.HuskSync;
import net.william278.husksync.user.OnlineUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A piece of data, held by a {@link DataHolder}
 */
@SuppressWarnings("unused")
public interface Data {

    /**
     * Apply (set) this data container to the given {@link OnlineUser}
     *
     * @param user   the user to apply this element to
     * @param plugin the plugin instance
     */
    void apply(@NotNull UserDataHolder user, @NotNull HuskSync plugin);

    /**
     * A data container holding data for:
     * <ul>
     *     <li>Inventories</li>
     *     <li>Ender Chests</li>
     * </ul>
     */
    interface Items extends Data {

        @Nullable
        Stack @NotNull [] getStack();

        default int getSlotCount() {
            return getStack().length;
        }

        record Stack(@NotNull String material, int amount, @Nullable String name,
                     @Nullable List<String> lore, @NotNull List<String> enchantments) {

        }

        default boolean isEmpty() {
            return Arrays.stream(getStack()).allMatch(Objects::isNull) || getStack().length == 0;
        }

        void clear();

        void setContents(@NotNull Items contents);

        /**
         * A data container holding data for inventories and selected hotbar slot
         */
        interface Inventory extends Items {

            int INVENTORY_SLOT_COUNT = 41;
            String ITEMS_TAG = "items";
            String HELD_ITEM_SLOT_TAG = "held_item_slot";

            int getHeldItemSlot();

            void setHeldItemSlot(int heldItemSlot) throws IllegalArgumentException;

            default Optional<Stack> getHelmet() {
                return Optional.ofNullable(getStack()[39]);
            }

            default Optional<Stack> getChestplate() {
                return Optional.ofNullable(getStack()[38]);
            }

            default Optional<Stack> getLeggings() {
                return Optional.ofNullable(getStack()[37]);
            }

            default Optional<Stack> getBoots() {
                return Optional.ofNullable(getStack()[36]);
            }

            default Optional<Stack> getOffHand() {
                return Optional.ofNullable(getStack()[40]);
            }
        }

        /**
         * Data container holding data for ender chests
         */
        interface EnderChest extends Items {
            int ENDER_CHEST_SLOT_COUNT = 27;
        }

    }

    /**
     * Data container holding data for potion effects
     */
    interface PotionEffects extends Data {

        @NotNull
        List<Effect> getActiveEffects();

        /**
         * Represents a potion effect
         *
         * @param type          the key of potion effect
         * @param amplifier     the amplifier of the potion effect
         * @param duration      the duration of the potion effect
         * @param isAmbient     whether the potion effect is ambient
         * @param showParticles whether the potion effect shows particles
         * @param hasIcon       whether the potion effect displays a HUD icon
         */
        record Effect(@SerializedName("type") @NotNull String type,
                      @SerializedName("amplifier") int amplifier,
                      @SerializedName("duration") int duration,
                      @SerializedName("is_ambient") boolean isAmbient,
                      @SerializedName("show_particles") boolean showParticles,
                      @SerializedName("has_icon") boolean hasIcon) {

        }

    }

    /**
     * Data container holding data for advancements
     */
    interface Advancements extends Data {

        String RECIPE_ADVANCEMENT = "minecraft:recipe";

        @NotNull
        List<Advancement> getCompleted();

        @NotNull
        default List<Advancement> getCompletedExcludingRecipes() {
            return getCompleted().stream().filter(adv -> !adv.getKey().startsWith(RECIPE_ADVANCEMENT)).toList();
        }

        void setCompleted(@NotNull List<Advancement> completed);

        class Advancement {
            @SerializedName("key")
            private String key;

            @SerializedName("completed_criteria")
            private Map<String, Long> completedCriteria;

            private Advancement(@NotNull String key, @NotNull Map<String, Date> completedCriteria) {
                this.key = key;
                this.completedCriteria = adaptDateMap(completedCriteria);
            }

            @SuppressWarnings("unused")
            private Advancement() {
            }

            @NotNull
            public static Advancement adapt(@NotNull String key, @NotNull Map<String, Date> completedCriteria) {
                return new Advancement(key, completedCriteria);
            }

            @NotNull
            private static Map<String, Long> adaptDateMap(@NotNull Map<String, Date> dateMap) {
                return dateMap.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getTime()));
            }

            @NotNull
            private static Map<String, Date> adaptLongMap(@NotNull Map<String, Long> dateMap) {
                return dateMap.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> new Date(e.getValue())));
            }

            @NotNull
            public String getKey() {
                return key;
            }

            public void setKey(@NotNull String key) {
                this.key = key;
            }

            public Map<String, Date> getCompletedCriteria() {
                return adaptLongMap(completedCriteria);
            }

            public void setCompletedCriteria(Map<String, Date> completedCriteria) {
                this.completedCriteria = adaptDateMap(completedCriteria);
            }
        }

    }

    /**
     * Data container holding data for the player's location
     */
    interface Location extends Data {
        double getX();

        void setX(double x);

        double getY();

        void setY(double y);

        double getZ();

        void setZ(double z);

        float getYaw();

        void setYaw(float yaw);

        float getPitch();

        void setPitch(float pitch);

        @NotNull
        World getWorld();

        void setWorld(@NotNull World world);

        record World(
                @SerializedName("name") @NotNull String name,
                @SerializedName("uuid") @NotNull UUID uuid,
                @SerializedName("environment") @NotNull String environment
        ) {
        }
    }

    /**
     * Data container holding data for statistics
     */
    interface Statistics extends Data {
        @NotNull
        Map<String, Integer> getGenericStatistics();

        @NotNull
        Map<String, Map<String, Integer>> getBlockStatistics();

        @NotNull
        Map<String, Map<String, Integer>> getItemStatistics();

        @NotNull
        Map<String, Map<String, Integer>> getEntityStatistics();
    }

    /**
     * Data container holding data for persistent data containers
     */
    interface PersistentData extends Data {

    }

    /**
     * A data container holding data for:
     * <ul>
     *     <li>Health</li>
     *     <li>Max Health</li>
     *     <li>Health Scale</li>
     * </ul>
     */
    interface Health extends Data {
        double getHealth();

        void setHealth(double health);

        /**
         * @deprecated Use {@link Attributes#getMaxHealth()} instead
         */
        @Deprecated(forRemoval = true, since = "3.5")
        default double getMaxHealth() {
            return getHealth();
        }

        /**
         * @deprecated Use {@link Attributes#setMaxHealth(double)} instead
         */
        @Deprecated(forRemoval = true, since = "3.5")
        default void setMaxHealth(double maxHealth) {
        }

        double getHealthScale();

        void setHealthScale(double healthScale);
    }

    /**
     * A data container holding player attribute data
     */
    interface Attributes extends Data {

        Key MAX_HEALTH_KEY = Key.key("generic.max_health");

        List<Attribute> getAttributes();

        record Attribute(
                @NotNull String name,
                double baseValue,
                @NotNull Set<Modifier> modifiers
        ) {

            public double getValue() {
                double value = baseValue;
                for (Modifier modifier : modifiers) {
                    value = modifier.modify(value);
                }
                return value;
            }

        }

        @Getter
        @Accessors(fluent = true)
        @RequiredArgsConstructor
        final class Modifier {
            final static String ANY_EQUIPMENT_SLOT_GROUP = "any";

            @Getter(AccessLevel.NONE)
            @Nullable
            @SerializedName("uuid")
            private UUID uuid = null;

            // Since 1.21.1: Name, amount, operation, slotGroup
            @SerializedName("name")
            private String name;

            @SerializedName("amount")
            private double amount;

            @SerializedName("operation")
            private int operation;

            @SerializedName("equipment_slot")
            @Deprecated(since = "3.7")
            private int equipmentSlot;

            @SerializedName("equipment_slot_group")
            private String slotGroup = ANY_EQUIPMENT_SLOT_GROUP;

            public Modifier(@NotNull String name, double amount, int operation, @NotNull String slotGroup) {
                this.name = name;
                this.amount = amount;
                this.operation = operation;
                this.slotGroup = slotGroup;
            }

            @Deprecated(since = "3.7")
            public Modifier(@NotNull UUID uuid, @NotNull String name, double amount, int operation, int equipmentSlot) {
                this.name = name;
                this.amount = amount;
                this.operation = operation;
                this.equipmentSlot = equipmentSlot;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj instanceof Modifier other) {
                    if (uuid != null && other.uuid != null) {
                        return uuid.equals(other.uuid);
                    }
                    return name.equals(other.name);
                }
                return super.equals(obj);
            }

            public double modify(double value) {
                return switch (operation) {
                    case 0 -> value + amount;
                    case 1 -> value * amount;
                    case 2 -> value * (1 + amount);
                    default -> value;
                };
            }

            public boolean hasUuid() {
                return uuid != null;
            }

            @NotNull
            public UUID uuid() {
                return uuid != null ? uuid : UUID.nameUUIDFromBytes(name.getBytes());
            }

        }

        default Optional<Attribute> getAttribute(@NotNull Key key) {
            return getAttributes().stream()
                    .filter(attribute -> attribute.name().equals(key.asString()))
                    .findFirst();
        }

        default void removeAttribute(@NotNull Key key) {
            getAttributes().removeIf(attribute -> attribute.name().equals(key.asString()));
        }

        default double getMaxHealth() {
            return getAttribute(MAX_HEALTH_KEY)
                    .map(Attribute::getValue)
                    .orElse(20.0);
        }

        default void setMaxHealth(double maxHealth) {
            removeAttribute(MAX_HEALTH_KEY);
            getAttributes().add(new Attribute(MAX_HEALTH_KEY.asString(), maxHealth, Sets.newHashSet()));
        }

    }

    /**
     * A data container holding data for:
     * <ul>
     *
     *     <li>Food Level</li>
     *     <li>Saturation</li>
     *     <li>Exhaustion</li>
     * </ul>
     */
    interface Hunger extends Data {

        int getFoodLevel();

        void setFoodLevel(int foodLevel);

        float getSaturation();

        void setSaturation(float saturation);

        float getExhaustion();

        void setExhaustion(float exhaustion);

    }

    /**
     * A data container holding data for:
     * <ul>
     *     <li>Total experience</li>
     *     <li>Experience level</li>
     *     <li>Experience progress</li>
     * </ul>
     */
    interface Experience extends Data {

        int getTotalExperience();

        void setTotalExperience(int totalExperience);

        int getExpLevel();

        void setExpLevel(int expLevel);

        float getExpProgress();

        void setExpProgress(float expProgress);
    }

    /**
     * Data container holding data for the player's current game mode
     */
    interface GameMode extends Data {

        @NotNull
        String getGameMode();

        void setGameMode(@NotNull String gameMode);

        /**
         * Get if the player can fly.
         *
         * @return {@code false} since v3.5
         * @deprecated Moved to its own data type. This will always return {@code false}.
         * Use {@link FlightStatus#isAllowFlight()} instead
         */
        @Deprecated(forRemoval = true, since = "3.5")
        default boolean getAllowFlight() {
            return false;
        }

        /**
         * Set if the player can fly.
         *
         * @deprecated Moved to its own data type.
         * Use {@link FlightStatus#setAllowFlight(boolean)} instead
         */
        @Deprecated(forRemoval = true, since = "3.5")
        default void setAllowFlight(boolean allowFlight) {
        }

        /**
         * Get if the player is flying.
         *
         * @return {@code false} since v3.5
         * @deprecated Moved to its own data type. This will always return {@code false}.
         * Use {@link FlightStatus#isFlying()} instead
         */
        @Deprecated(forRemoval = true, since = "3.5")
        default boolean getIsFlying() {
            return false;
        }

        /**
         * Set if the player is flying.
         *
         * @deprecated Moved to its own data type.
         * Use {@link FlightStatus#setFlying(boolean)} instead
         */
        @Deprecated(forRemoval = true, since = "3.5")
        default void setIsFlying(boolean isFlying) {
        }

    }

    /**
     * Data container holding data for the player's flight status
     *
     * @since 3.5
     */
    interface FlightStatus extends Data {
        boolean isAllowFlight();

        void setAllowFlight(boolean allowFlight);

        boolean isFlying();

        void setFlying(boolean isFlying);
    }


}
