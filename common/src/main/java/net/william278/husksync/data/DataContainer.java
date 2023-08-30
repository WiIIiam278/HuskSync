package net.william278.husksync.data;

import com.google.gson.annotations.SerializedName;
import net.william278.husksync.player.OnlineUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * A piece of data, held by an {@link DataOwner}
 */
public interface DataContainer {

    /**
     * Apply (set) this data container to the given {@link OnlineUser}
     *
     * @param user the user to apply this element to
     */
    void apply(@NotNull DataOwner user) throws IllegalStateException;

    /**
     * Enumeration of types of {@link DataContainer}s
     */
    enum Type {
        INVENTORY(true),
        ENDER_CHEST(true),
        POTION_EFFECTS(true),
        ADVANCEMENTS(true),
        LOCATION(true),
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
        List<EffectPreview> getPreview();


        record EffectPreview(@NotNull String type, int amplifier, int duration, boolean ambient, boolean particles,
                             boolean icon) {

        }

    }

    /**
     * Data container holding data for advancements
     */
    interface Advancements extends DataContainer {

        @NotNull
        List<CompletedAdvancement> getCompleted();

        void setCompleted(@NotNull List<CompletedAdvancement> completed);

        class CompletedAdvancement {
            @SerializedName("key")
            private String key;

            @SerializedName("completed_criteria")
            private Map<String, Date> completedCriteria;

            private CompletedAdvancement(@NotNull String key, @NotNull Map<String, Date> completedCriteria) {
                this.key = key;
                this.completedCriteria = completedCriteria;
            }

            @SuppressWarnings("unused")
            private CompletedAdvancement() {
            }

            @NotNull
            public static CompletedAdvancement adapt(@NotNull String key, @NotNull Map<String, Date> completedCriteria) {
                return new CompletedAdvancement(key, completedCriteria);
            }

            public String getKey() {
                return key;
            }

            public void setKey(String key) {
                this.key = key;
            }

            public Map<String, Date> getCompletedCriteria() {
                return completedCriteria;
            }

            public void setCompletedCriteria(Map<String, Date> completedCriteria) {
                this.completedCriteria = completedCriteria;
            }
        }

    }

    /**
     * Data container holding data for the player's location
     */
    interface Location extends DataContainer {
        double getX();

        double getY();

        double getZ();

        float getYaw();

        float getPitch();

        @NotNull
        World getWorld();


        record World(@NotNull String name, @NotNull UUID uuid) {
        }
    }

    /**
     * Data container holding data for statistics
     */
    interface Statistics extends DataContainer {
        @NotNull
        Map<String, Integer> getUntypedStatistics();

        @NotNull
        Map<String, Map<String, Integer>> getBlockStatistics();

        @NotNull
        Map<String, Map<String, Integer>> getItemStatistics();
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
