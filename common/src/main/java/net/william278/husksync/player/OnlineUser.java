package net.william278.husksync.player;

import de.themoep.minedown.MineDown;
import net.william278.husksync.config.Settings;
import net.william278.husksync.data.*;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a logged-in {@link User}
 */
public abstract class OnlineUser extends User {

    public OnlineUser(@NotNull UUID uuid, @NotNull String username) {
        super(uuid, username);
    }

    /**
     * Get the player's {@link StatusData}
     *
     * @return the player's {@link StatusData}
     */
    public abstract CompletableFuture<StatusData> getStatus();

    /**
     * Get the player's inventory {@link InventoryData} contents
     *
     * @return The player's inventory {@link InventoryData} contents
     */
    public abstract CompletableFuture<InventoryData> getInventory();

    /**
     * Get the player's ender chest {@link InventoryData} contents
     *
     * @return The player's ender chest {@link InventoryData} contents
     */
    public abstract CompletableFuture<InventoryData> getEnderChest();

    /**
     * Get the player's {@link PotionEffectData}
     *
     * @return The player's {@link PotionEffectData}
     */
    public abstract CompletableFuture<PotionEffectData> getPotionEffects();

    /**
     * Get the player's set of {@link AdvancementData}
     *
     * @return the player's set of {@link AdvancementData}
     */
    public abstract CompletableFuture<HashSet<AdvancementData>> getAdvancements();

    /**
     * Get the player's {@link StatisticsData}
     *
     * @return The player's {@link StatisticsData}
     */
    public abstract CompletableFuture<StatisticsData> getStatistics();

    /**
     * Get the player's {@link LocationData}
     *
     * @return the player's {@link LocationData}
     */
    public abstract CompletableFuture<LocationData> getLocation();

    /**
     * Get the player's {@link PersistentDataContainerData}
     *
     * @return The player's {@link PersistentDataContainerData} when fetched
     */
    public abstract CompletableFuture<PersistentDataContainerData> getPersistentDataContainer();

    /**
     * Set {@link UserData} to a player
     *
     * @param data     The data to set
     * @param settings Plugin settings, for determining what needs setting
     * @return a future that will be completed when done
     */
    public abstract CompletableFuture<Void> setData(@NotNull UserData data, @NotNull Settings settings);

    /**
     * Dispatch a MineDown-formatted message to this player
     *
     * @param mineDown the parsed {@link MineDown} to send
     */
    public abstract void sendMessage(@NotNull MineDown mineDown);

    /**
     * Dispatch a MineDown-formatted action bar message to this player
     *
     * @param mineDown the parsed {@link MineDown} to send
     */
    public abstract void sendActionBar(@NotNull MineDown mineDown);

    /**
     * Returns if the player has the permission node
     *
     * @param node The permission node string
     * @return {@code true} if the player has permission node; {@code false} otherwise
     */
    public abstract boolean hasPermission(@NotNull String node);

    /**
     * Get the player's current {@link UserData}
     *
     * @return the player's current {@link UserData}
     */
    public final CompletableFuture<UserData> getUserData() {
        return CompletableFuture.supplyAsync(() -> new UserData(getStatus().join(), getInventory().join(),
                getEnderChest().join(), getPotionEffects().join(), getAdvancements().join(),
                getStatistics().join(), getLocation().join(), getPersistentDataContainer().join()));
    }

}
