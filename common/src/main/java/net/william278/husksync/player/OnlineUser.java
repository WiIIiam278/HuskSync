package net.william278.husksync.player;

import de.themoep.minedown.adventure.MineDown;
import net.william278.desertwell.Version;
import net.william278.husksync.config.Settings;
import net.william278.husksync.data.*;
import net.william278.husksync.event.EventCannon;
import net.william278.husksync.event.PreSyncEvent;
import net.william278.husksync.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

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
     * Set the player's {@link StatusData}
     *
     * @param statusData      the player's {@link StatusData}
     * @param statusDataFlags the flags to use for setting the status data
     * @return a future returning void when complete
     * @deprecated Use {@link #setStatus(StatusData, Settings)} instead
     */
    @Deprecated(since = "2.1")
    public final CompletableFuture<Void> setStatus(@NotNull StatusData statusData,
                                                   @NotNull List<StatusDataFlag> statusDataFlags) {
        final Settings settings = new Settings();
        settings.synchronizationFeatures.put(Settings.SynchronizationFeature.HEALTH.name().toLowerCase(), statusDataFlags.contains(StatusDataFlag.SET_HEALTH));
        settings.synchronizationFeatures.put(Settings.SynchronizationFeature.MAX_HEALTH.name().toLowerCase(), statusDataFlags.contains(StatusDataFlag.SET_MAX_HEALTH));
        settings.synchronizationFeatures.put(Settings.SynchronizationFeature.HUNGER.name().toLowerCase(), statusDataFlags.contains(StatusDataFlag.SET_HUNGER));
        settings.synchronizationFeatures.put(Settings.SynchronizationFeature.EXPERIENCE.name().toLowerCase(), statusDataFlags.contains(StatusDataFlag.SET_EXPERIENCE));
        settings.synchronizationFeatures.put(Settings.SynchronizationFeature.INVENTORIES.name().toLowerCase(), statusDataFlags.contains(StatusDataFlag.SET_SELECTED_ITEM_SLOT));
        settings.synchronizationFeatures.put(Settings.SynchronizationFeature.LOCATION.name().toLowerCase(), statusDataFlags.contains(StatusDataFlag.SET_GAME_MODE) || statusDataFlags.contains(StatusDataFlag.SET_FLYING));
        return setStatus(statusData, settings);
    }

    /**
     * Set the player's {@link StatusData}
     *
     * @param statusData the player's {@link StatusData}
     * @param settings   settings, containing information about which features should be synced
     * @return a future returning void when complete
     */
    public abstract CompletableFuture<Void> setStatus(@NotNull StatusData statusData,
                                                      @NotNull Settings settings);

    /**
     * Get the player's inventory {@link ItemData} contents
     *
     * @return The player's inventory {@link ItemData} contents
     */
    public abstract CompletableFuture<ItemData> getInventory();

    /**
     * Set the player's {@link ItemData}
     *
     * @param itemData The player's {@link ItemData}
     * @return a future returning void when complete
     */
    public abstract CompletableFuture<Void> setInventory(@NotNull ItemData itemData);

    /**
     * Get the player's ender chest {@link ItemData} contents
     *
     * @return The player's ender chest {@link ItemData} contents
     */
    public abstract CompletableFuture<ItemData> getEnderChest();

    /**
     * Set the player's {@link ItemData}
     *
     * @param enderChestData The player's {@link ItemData}
     * @return a future returning void when complete
     */
    public abstract CompletableFuture<Void> setEnderChest(@NotNull ItemData enderChestData);


    /**
     * Get the player's {@link PotionEffectData}
     *
     * @return The player's {@link PotionEffectData}
     */
    public abstract CompletableFuture<PotionEffectData> getPotionEffects();

    /**
     * Set the player's {@link PotionEffectData}
     *
     * @param potionEffectData The player's {@link PotionEffectData}
     * @return a future returning void when complete
     */
    public abstract CompletableFuture<Void> setPotionEffects(@NotNull PotionEffectData potionEffectData);

    /**
     * Get the player's set of {@link AdvancementData}
     *
     * @return the player's set of {@link AdvancementData}
     */
    public abstract CompletableFuture<List<AdvancementData>> getAdvancements();

    /**
     * Set the player's {@link AdvancementData}
     *
     * @param advancementData List of the player's {@link AdvancementData}
     * @return a future returning void when complete
     */
    public abstract CompletableFuture<Void> setAdvancements(@NotNull List<AdvancementData> advancementData);

    /**
     * Get the player's {@link StatisticsData}
     *
     * @return The player's {@link StatisticsData}
     */
    public abstract CompletableFuture<StatisticsData> getStatistics();

    /**
     * Set the player's {@link StatisticsData}
     *
     * @param statisticsData The player's {@link StatisticsData}
     * @return a future returning void when complete
     */
    public abstract CompletableFuture<Void> setStatistics(@NotNull StatisticsData statisticsData);

    /**
     * Get the player's {@link LocationData}
     *
     * @return the player's {@link LocationData}
     */
    public abstract CompletableFuture<LocationData> getLocation();

    /**
     * Set the player's {@link LocationData}
     *
     * @param locationData the player's {@link LocationData}
     * @return a future returning void when complete
     */
    public abstract CompletableFuture<Void> setLocation(@NotNull LocationData locationData);

    /**
     * Get the player's {@link PersistentDataContainerData}
     *
     * @return The player's {@link PersistentDataContainerData} when fetched
     */
    public abstract CompletableFuture<PersistentDataContainerData> getPersistentDataContainer();

    /**
     * Set the player's {@link PersistentDataContainerData}
     *
     * @param persistentDataContainerData The player's {@link PersistentDataContainerData} to set
     * @return A future returning void when complete
     */
    public abstract CompletableFuture<Void> setPersistentDataContainer(@NotNull PersistentDataContainerData persistentDataContainerData);

    /**
     * Indicates if the player has gone offline
     *
     * @return {@code true} if the player has left the server; {@code false} otherwise
     */
    public abstract boolean isOffline();

    /**
     * Returns the implementing Minecraft server version
     *
     * @return The Minecraft server version
     */
    @NotNull
    public abstract Version getMinecraftVersion();

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
     * Show a GUI chest menu to the player, containing the given {@link ItemData}
     *
     * @param itemData    Item data to be shown in the GUI
     * @param editable    If the player should be able to remove, replace and move around the items
     * @param minimumRows The minimum number of rows to show in the chest menu
     * @param title       The title of the chest menu, as a {@link MineDown} locale
     * @return A future returning the {@link ItemData} in the chest menu when the player closes it
     * @since 2.1
     */
    public abstract CompletableFuture<Optional<ItemData>> showMenu(@NotNull ItemData itemData, boolean editable,
                                                                   int minimumRows, @NotNull MineDown title);

    /**
     * Returns true if the player is dead
     *
     * @return true if the player is dead
     */
    public abstract boolean isDead();

    /**
     * Apply {@link UserData} to a player, updating their inventory, status, statistics, etc. as per the config.
     * <p>
     * This will only set data that is enabled as per the enabled settings in the config file.
     * Data present in the {@link UserData} object, but not enabled to be set in the config, will be ignored.
     *
     * @param data                   The {@link UserData} to set to the player
     * @param settings               The plugin {@link Settings} to determine which data to set
     * @param eventCannon            The {@link EventCannon} to fire the synchronisation events
     * @param logger                 The {@link Logger} for debug and error logging
     * @param serverMinecraftVersion The server's Minecraft version, for validating the format of the {@link UserData}
     * @return a future returning a boolean when complete; if the sync was successful, the future will return {@code true}.
     */
    public final CompletableFuture<Boolean> setData(@NotNull UserData data, @NotNull Settings settings,
                                                    @NotNull EventCannon eventCannon, @NotNull Logger logger,
                                                    @NotNull Version serverMinecraftVersion) {
        return CompletableFuture.supplyAsync(() -> {
            // Prevent synchronising user data from newer versions of Minecraft
            if (Version.fromMinecraftVersionString(data.getMinecraftVersion()).compareTo(serverMinecraftVersion) > 0) {
                logger.log(Level.SEVERE, "Cannot set data for " + username +
                                         " because the Minecraft version of their user data (" + data.getMinecraftVersion() +
                                         ") is newer than the server's Minecraft version (" + serverMinecraftVersion + ").");
                return false;
            }
            // Prevent synchronising user data from newer versions of the plugin
            if (data.getFormatVersion() > UserData.CURRENT_FORMAT_VERSION) {
                logger.log(Level.SEVERE, "Cannot set data for " + username +
                                         " because the format version of their user data (v" + data.getFormatVersion() +
                                         ") is newer than the current format version (v" + UserData.CURRENT_FORMAT_VERSION + ").");
                return false;
            }

            // Fire the PreSyncEvent
            final PreSyncEvent preSyncEvent = (PreSyncEvent) eventCannon.firePreSyncEvent(this, data).join();
            final UserData finalData = preSyncEvent.getUserData();
            final List<CompletableFuture<Void>> dataSetOperations = new ArrayList<>() {{
                if (!isOffline() && !preSyncEvent.isCancelled()) {
                    if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.INVENTORIES)) {
                        finalData.getInventory().ifPresent(itemData -> add(setInventory(itemData)));
                    }
                    if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.ENDER_CHESTS)) {
                        finalData.getEnderChest().ifPresent(itemData -> add(setEnderChest(itemData)));
                    }
                    finalData.getStatus().ifPresent(statusData -> add(setStatus(statusData, settings)));
                    if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.POTION_EFFECTS)) {
                        finalData.getPotionEffects().ifPresent(potionEffectData -> add(setPotionEffects(potionEffectData)));
                    }
                    if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.ADVANCEMENTS)) {
                        finalData.getAdvancements().ifPresent(advancementData -> add(setAdvancements(advancementData)));
                    }
                    if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.STATISTICS)) {
                        finalData.getStatistics().ifPresent(statisticData -> add(setStatistics(statisticData)));
                    }
                    if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.LOCATION)) {
                        finalData.getLocation().ifPresent(locationData -> add(setLocation(locationData)));
                    }
                    if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.PERSISTENT_DATA_CONTAINER)) {
                        finalData.getPersistentDataContainer().ifPresent(persistentDataContainerData ->
                                add(setPersistentDataContainer(persistentDataContainerData)));
                    }
                }
            }};
            // Apply operations in parallel, join when complete
            return CompletableFuture.allOf(dataSetOperations.toArray(new CompletableFuture[0])).thenApply(unused -> true)
                    .exceptionally(exception -> {
                        // Handle synchronisation exceptions
                        logger.log(Level.SEVERE, "Failed to set data for player " + username + " (" + exception.getMessage() + ")");
                        exception.printStackTrace();
                        return false;
                    }).join();
        });

    }

    /**
     * Get the player's current {@link UserData} in an {@link Optional}.
     * <p>
     * Since v2.1, this method will respect the data synchronisation settings; user data will only be as big as the
     * enabled synchronisation values set in the config file
     * <p>
     * Also note that if the {@code SYNCHRONIZATION_SAVE_DEAD_PLAYER_INVENTORIES} ConfigOption has been set,
     * the user's inventory will only be returned if the player is alive.
     * <p>
     * If the user data could not be returned due to an exception, the optional will return empty
     *
     * @param logger The logger to use for handling exceptions
     * @return the player's current {@link UserData} in an optional; empty if an exception occurs
     */
    public final CompletableFuture<Optional<UserData>> getUserData(@NotNull Logger logger, @NotNull Settings settings) {
        return CompletableFuture.supplyAsync(() -> {
                    final UserDataBuilder builder = UserData.builder(getMinecraftVersion());
                    final List<CompletableFuture<Void>> dataGetOperations = new ArrayList<>() {{
                        if (!isOffline()) {
                            if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.INVENTORIES)) {
                                if (isDead() && settings.saveDeadPlayerInventories) {
                                    add(CompletableFuture.runAsync(() -> builder.setInventory(ItemData.empty())));
                                } else {
                                    add(getInventory().thenAccept(builder::setInventory));
                                }
                            }
                            if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.ENDER_CHESTS)) {
                                add(getEnderChest().thenAccept(builder::setEnderChest));
                            }
                            add(getStatus().thenAccept(builder::setStatus));
                            if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.POTION_EFFECTS)) {
                                add(getPotionEffects().thenAccept(builder::setPotionEffects));
                            }
                            if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.ADVANCEMENTS)) {
                                add(getAdvancements().thenAccept(builder::setAdvancements));
                            }
                            if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.STATISTICS)) {
                                add(getStatistics().thenAccept(builder::setStatistics));
                            }
                            if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.LOCATION)) {
                                add(getLocation().thenAccept(builder::setLocation));
                            }
                            if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.PERSISTENT_DATA_CONTAINER)) {
                                add(getPersistentDataContainer().thenAccept(builder::setPersistentDataContainer));
                            }
                        }
                    }};

                    // Apply operations in parallel, join when complete
                    CompletableFuture.allOf(dataGetOperations.toArray(new CompletableFuture[0])).join();
                    return Optional.of(builder.build());
                })
                .exceptionally(exception -> {
                    logger.log(Level.SEVERE, "Failed to get user data from online player " + username + " (" + exception.getMessage() + ")");
                    exception.printStackTrace();
                    return Optional.empty();
                });
    }

}
