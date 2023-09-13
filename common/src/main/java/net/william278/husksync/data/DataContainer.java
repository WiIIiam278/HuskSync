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
import net.william278.husksync.HuskSync;
import net.william278.husksync.player.OnlineUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A piece of data, held by an {@link DataOwner}
 */
public interface DataContainer {

    /**
     * Apply (set) this data container to the given {@link OnlineUser}
     *
     * @param user   the user to apply this element to
     * @param plugin
     */
    void apply(@NotNull DataOwner user, @NotNull HuskSync plugin) throws IllegalStateException;

    /**
     * Enumeration of types of {@link DataContainer}s
     */
    enum Type {
        INVENTORY(true),
        ENDER_CHEST(true),
        POTION_EFFECTS(true),
        ADVANCEMENTS(true),
        LOCATION(false),
        STATISTICS(true),
        HEALTH(true),
        FOOD(true),
        EXPERIENCE(true),
        GAME_MODE(true),
        PERSISTENT_DATA(false);

        private final boolean defaultSetting;

        Type(boolean defaultSetting) {
            this.defaultSetting = defaultSetting;
        }

        @NotNull
        private Map.Entry<String, Boolean> toEntry() {
            return Map.entry(name().toLowerCase(Locale.ENGLISH), defaultSetting);
        }

        @SuppressWarnings("unchecked")
        @NotNull
        public static Map<String, Boolean> getDefaults() {
            return Map.ofEntries(Arrays.stream(values())
                    .map(Type::toEntry)
                    .toArray(Map.Entry[]::new));
        }

        public boolean getDefault() {
            return defaultSetting;
        }

    }

    /**
     * A data container holding data for:
     * <ul>
     *     <li>Inventories</li>
     *     <li>Ender Chests</li>
     * </ul>
     */
    interface Items extends DataContainer {

        @NotNull
        StackPreview[] getPreview();

        default int getSlotCount() {
            return getPreview().length;
        }

        record StackPreview(@NotNull String material, int amount, @Nullable String name,
                            @Nullable List<String> lore, @NotNull List<String> enchantments) {

        }

        default boolean isEmpty() {
            return Arrays.stream(getPreview()).allMatch(Objects::isNull) || getPreview().length == 0;
        }

        void clear();

        void setContents(@NotNull Items contents);

        /**
         * A data container holding data for inventories and selected hotbar slot
         */
        interface Inventory extends Items {

            int getHeldItemSlot();

            void setHeldItemSlot(int heldItemSlot) throws IllegalArgumentException;

            default Optional<StackPreview> getHelmet() {
                return Optional.ofNullable(getPreview()[39]);
            }

            default Optional<StackPreview> getChestplate() {
                return Optional.ofNullable(getPreview()[38]);
            }

            default Optional<StackPreview> getLeggings() {
                return Optional.ofNullable(getPreview()[37]);
            }

            default Optional<StackPreview> getBoots() {
                return Optional.ofNullable(getPreview()[36]);
            }

            default Optional<StackPreview> getOffHand() {
                return Optional.ofNullable(getPreview()[40]);
            }
        }

        /**
         * Data container holding data for ender chests
         */
        interface EnderChest extends Items {

        }

    }

    /**
     * Data container holding data for potion effects
     */
    interface PotionEffects extends DataContainer {

        @NotNull
        List<Effect> getActiveEffects();

        /**
         * Represents a potion effect
         *
         * @param type          the type of potion effect
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
    interface Advancements extends DataContainer {

        @NotNull
        List<Advancement> getCompleted();

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
    interface Location extends DataContainer {
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
    interface Statistics extends DataContainer {
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
    interface PersistentData extends DataContainer {

    }

    /**
     * A data container holding data for:
     * <ul>
     *     <li>Health</li>
     *     <li>Max Health</li>
     *     <li>Health Scale</li>
     * </ul>
     */
    interface Health extends DataContainer {
        double getHealth();

        void setHealth(double health);

        double getMaxHealth();

        void setMaxHealth(double maxHealth);

        double getHealthScale();

        void setHealthScale(double healthScale);
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
    interface Food extends DataContainer {

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
    interface Experience extends DataContainer {

        int getTotalExperience();

        void setTotalExperience(int totalExperience);

        int getExpLevel();

        void setExpLevel(int expLevel);

        float getExpProgress();

        void setExpProgress(float expProgress);
    }

    /**
     * A data container holding data for:
     * <ul>
     *     <li>Game mode</li>
     *     <li>Allow flight</li>
     *     <li>Is flying</li>
     * </ul>
     */
    interface GameMode extends DataContainer {

        @NotNull
        String getGameMode();

        void setGameMode(@NotNull String gameMode);

        boolean getAllowFlight();

        void setAllowFlight(boolean allowFlight);

        boolean getIsFlying();

        void setIsFlying(boolean isFlying);
    }


}