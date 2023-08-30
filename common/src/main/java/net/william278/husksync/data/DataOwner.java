package net.william278.husksync.data;

import net.william278.desertwell.util.ThrowingConsumer;
import net.william278.husksync.HuskSync;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * A holder of data in the form of {@link DataContainer}s, which can be synced
 */
public interface DataOwner extends MutableDataStore {

    /**
     * Get the data that is enabled for syncing in the config
     *
     * @return the data that is enabled for syncing
     * @since 3.0
     */
    @Override
    @NotNull
    default Map<DataContainer.Type, DataContainer> getData() {
        return Arrays.stream(DataContainer.Type.values())
                .filter(type -> getPlugin().getSettings().getSynchronizationFeature(type))
                .map(type -> Map.entry(type, switch (type) {
                    case INVENTORY -> getInventory();
                    case ENDER_CHEST -> getEnderChest();
                    case POTION_EFFECTS -> getPotionEffects();
                    case ADVANCEMENTS -> getAdvancements();
                    case LOCATION -> getLocation();
                    case STATISTICS -> getStatistics();
                    case HEALTH -> getHealth();
                    case FOOD -> getFood();
                    case EXPERIENCE -> getExperience();
                    case GAME_MODE -> getGameMode();
                    case PERSISTENT_DATA -> getPersistentData();
                }))
                .filter(entry -> entry.getValue().isPresent())
                .collect(HashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue().get()), HashMap::putAll);
    }

    /**
     * Create a serialized data snapshot of this data owner
     *
     * @param saveCause the cause of the snapshot
     * @return the snapshot
     * @since 3.0
     */
    @NotNull
    default DataSnapshot.Packed createSnapshot(@NotNull DataSnapshot.SaveCause saveCause) {
        return DataSnapshot.create(getPlugin(), this, saveCause);
    }

    /**
     * Deserialize and apply a data snapshot to this data owner
     * <p>
     * This method will deserialize the data on the current thread, then synchronously apply it on
     * the main server thread.
     * </p>
     * The {@code runAfter} callback function will be run after the snapshot has been applied.
     *
     * @param snapshot the snapshot to apply
     * @param runAfter the function to run asynchronously after the snapshot has been applied
     * @since 3.0
     */
    default void applySnapshot(@NotNull DataSnapshot.Packed snapshot, @NotNull ThrowingConsumer<DataOwner> runAfter) {
        final HuskSync plugin = getPlugin();
        final DataSnapshot.Unpacked unpacked = snapshot.unpack(plugin);
        plugin.runSync(() -> {
            try {
                unpacked.getData().forEach((type, data) -> {
                    if (plugin.getSettings().getSynchronizationFeature(type)) {
                        data.apply(this);
                    }
                });
            } catch (Throwable e) {
                plugin.log(Level.SEVERE, "An exception occurred applying data to a user", e);
                return;
            }

            plugin.runAsync(() -> runAfter.accept(this));
        });
    }

    @Override
    default void setInventory(@NotNull DataContainer.Items.Inventory inventory) {
        inventory.apply(this);
    }

    @Override
    default void setEnderChest(@NotNull DataContainer.Items.EnderChest enderChest) {
        enderChest.apply(this);
    }

    @Override
    default void setPotionEffects(@NotNull DataContainer.PotionEffects potionEffects) {
        potionEffects.apply(this);
    }

    @Override
    default void setAdvancements(@NotNull DataContainer.Advancements advancements) {
        advancements.apply(this);
    }

    @Override
    default void setLocation(@NotNull DataContainer.Location location) {
        location.apply(this);
    }

    @Override
    default void setStatistics(@NotNull DataContainer.Statistics statistics) {
        statistics.apply(this);
    }

    @Override
    default void setHealth(@NotNull DataContainer.Health health) {
        health.apply(this);
    }

    @Override
    default void setFood(@NotNull DataContainer.Food food) {
        food.apply(this);
    }

    @Override
    default void setExperience(@NotNull DataContainer.Experience experience) {
        experience.apply(this);
    }

    @Override
    default void setGameMode(@NotNull DataContainer.GameMode gameMode) {
        gameMode.apply(this);
    }

    @Override
    default void setPersistentData(@NotNull DataContainer.PersistentData persistentData) {
        persistentData.apply(this);
    }

    @NotNull
    @ApiStatus.Internal
    HuskSync getPlugin();

}
