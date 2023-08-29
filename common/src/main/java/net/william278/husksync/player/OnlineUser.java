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

package net.william278.husksync.player;

import de.themoep.minedown.adventure.MineDown;
import de.themoep.minedown.adventure.MineDownParser;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.william278.desertwell.util.Version;
import net.william278.husksync.HuskSync;
import net.william278.husksync.config.Settings;
import net.william278.husksync.data.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Represents a logged-in {@link User}
 */
public abstract class OnlineUser extends User implements CommandUser {

    public OnlineUser(@NotNull UUID uuid, @NotNull String username) {
        super(uuid, username);
    }

    /**
     * Get the player's {@link StatusData}
     *
     * @return the player's {@link StatusData}
     */
    @NotNull
    public abstract StatusData getStatus();

    /**
     * Set the player's {@link StatusData}
     *
     * @param statusData the player's {@link StatusData}
     * @param settings   settings, containing information about which features should be synced
     */
    public abstract void setStatus(@NotNull StatusData statusData, @NotNull Settings settings);

    /**
     * Get the player's inventory {@link ItemData} contents
     *
     * @return The player's inventory {@link ItemData} contents
     */
    @NotNull
    public abstract ItemData getInventory();

    /**
     * Set the player's {@link ItemData}
     *
     * @param itemData The player's {@link ItemData}
     */
    public abstract void setInventory(@NotNull ItemData itemData);

    /**
     * Get the player's ender chest {@link ItemData} contents
     *
     * @return The player's ender chest {@link ItemData} contents
     */
    @NotNull
    public abstract ItemData getEnderChest();

    /**
     * Set the player's {@link ItemData}
     *
     * @param enderChestData The player's {@link ItemData}
     */
    public abstract void setEnderChest(@NotNull ItemData enderChestData);


    /**
     * Get the player's {@link PotionEffectData}
     *
     * @return The player's {@link PotionEffectData}
     */
    @NotNull
    public abstract PotionEffectData getPotionEffects();

    /**
     * Set the player's {@link PotionEffectData}
     *
     * @param potionEffectData The player's {@link PotionEffectData}
     */
    public abstract void setPotionEffects(@NotNull PotionEffectData potionEffectData);

    /**
     * Get the player's set of {@link AdvancementData}
     *
     * @return the player's set of {@link AdvancementData}
     */
    @NotNull
    public abstract List<AdvancementData> getAdvancements();

    /**
     * Set the player's {@link AdvancementData}
     *
     * @param advancementData List of the player's {@link AdvancementData}
     */
    public abstract void setAdvancements(@NotNull List<AdvancementData> advancementData);

    /**
     * Get the player's {@link StatisticsData}
     *
     * @return The player's {@link StatisticsData}
     */
    @NotNull
    public abstract StatisticsData getStatistics();

    /**
     * Set the player's {@link StatisticsData}
     *
     * @param statisticsData The player's {@link StatisticsData}
     */
    public abstract void setStatistics(@NotNull StatisticsData statisticsData);

    /**
     * Get the player's {@link LocationData}
     *
     * @return the player's {@link LocationData}
     */
    public abstract LocationData getLocation();

    /**
     * Set the player's {@link LocationData}
     *
     * @param locationData the player's {@link LocationData}
     */
    public abstract void setLocation(@NotNull LocationData locationData);

    /**
     * Get the player's {@link PersistentDataContainerData}
     *
     * @return The player's {@link PersistentDataContainerData} when fetched
     */
    @NotNull
    public abstract PersistentDataContainerData getPersistentDataContainer();

    /**
     * Set the player's {@link PersistentDataContainerData}
     *
     * @param persistentDataContainerData The player's {@link PersistentDataContainerData} to set
     */
    public abstract void setPersistentDataContainer(@NotNull PersistentDataContainerData persistentDataContainerData);

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
     * Get the player's adventure {@link Audience}
     *
     * @return the player's {@link Audience}
     */
    @NotNull
    public abstract Audience getAudience();

    /**
     * Send a message to this player
     *
     * @param component the {@link Component} message to send
     */
    public void sendMessage(@NotNull Component component) {
        getAudience().sendMessage(component);
    }

    /**
     * Dispatch a MineDown-formatted message to this player
     *
     * @param mineDown the parsed {@link MineDown} to send
     */
    public void sendMessage(@NotNull MineDown mineDown) {
        sendMessage(mineDown
                .disable(MineDownParser.Option.SIMPLE_FORMATTING)
                .replace().toComponent());
    }

    /**
     * Dispatch a MineDown-formatted action bar message to this player
     *
     * @param mineDown the parsed {@link MineDown} to send
     */
    public void sendActionBar(@NotNull MineDown mineDown) {
        getAudience().sendActionBar(mineDown
                .disable(MineDownParser.Option.SIMPLE_FORMATTING)
                .replace().toComponent());
    }

    /**
     * Dispatch a toast message to this player
     *
     * @param title          the title of the toast
     * @param description    the description of the toast
     * @param iconMaterial   the namespace-keyed material to use as an icon of the toast
     * @param backgroundType the background ("ToastType") of the toast
     */
    public abstract void sendToast(@NotNull MineDown title, @NotNull MineDown description,
                                   @NotNull String iconMaterial, @NotNull String backgroundType);

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
     * @param plugin The plugin instance
     */
    public final void setData(@NotNull UserData data, @NotNull HuskSync plugin) {
        // Prevent synchronizing user data from newer versions of Minecraft
        if (Version.fromString(data.getMinecraftVersion()).compareTo(plugin.getMinecraftVersion()) > 0) {
            plugin.log(Level.SEVERE, "Cannot set data for " + username +
                    " because the Minecraft version of their user data (" + data.getMinecraftVersion() +
                    ") is newer than the server's Minecraft version (" + plugin.getMinecraftVersion() + ").");
            plugin.getLocales().getLocale("data_update_failed")
                    .ifPresent(this::sendMessage);
            return;
        }
        // Prevent synchronizing user data from newer versions of the plugin
        if (data.getFormatVersion() > UserData.CURRENT_FORMAT_VERSION) {
            plugin.log(Level.SEVERE, "Cannot set data for " + username +
                    " because the format version of their user data (v" + data.getFormatVersion() +
                    ") is newer than the current format version (v" + UserData.CURRENT_FORMAT_VERSION + ").");
            plugin.getLocales().getLocale("data_update_failed")
                    .ifPresent(this::sendMessage);
            return;
        }

        // Fire the PreSyncEvent
        plugin.fireEvent(plugin.getPreSyncEvent(this, data), (event) -> {
            if (isOffline() || event.isCancelled()) {
                return;
            }

            // Set the user
            final UserData finalData = event.getUserData();
            plugin.runSync(() -> {
                try {
                    // TODO: Separate deserialization of data with setting of data
                    final Settings settings = plugin.getSettings();
                    if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.INVENTORIES)) {
                        finalData.getInventory().ifPresent(this::setInventory);
                    }
                    if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.ENDER_CHESTS)) {
                        finalData.getEnderChest().ifPresent(this::setEnderChest);
                    }
                    finalData.getStatus().ifPresent(statusData -> setStatus(statusData, settings));
                    if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.POTION_EFFECTS)) {
                        finalData.getPotionEffects().ifPresent(this::setPotionEffects);
                    }
                    if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.ADVANCEMENTS)) {
                        finalData.getAdvancements().ifPresent(this::setAdvancements);
                    }
                    if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.STATISTICS)) {
                        finalData.getStatistics().ifPresent(this::setStatistics);
                    }
                    if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.LOCATION)) {
                        finalData.getLocation().ifPresent(this::setLocation);
                    }
                    if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.PERSISTENT_DATA_CONTAINER)) {
                        finalData.getPersistentDataContainer().ifPresent(this::setPersistentDataContainer);
                    }
                    completeSynchronization(true, plugin);
                } catch (Throwable e) {
                    plugin.log(Level.SEVERE, String.format("An error occurred while setting data for %s", username), e);
                    completeSynchronization(false, plugin);
                }
            });
        });
    }

    /**
     * Get the player's current {@link UserData} in an {@link Optional}.
     * <p>
     * Since v2.1, this method will respect the data synchronization settings; user data will only be as big as the
     * enabled synchronization values set in the config file
     * <p>
     * Also note that if the {@code SYNCHRONIZATION_SAVE_DEAD_PLAYER_INVENTORIES} ConfigOption has been set,
     * the user's inventory will only be returned if the player is alive.
     * <p>
     * If the user data could not be returned due to an exception, the optional will return empty
     *
     * @param plugin The plugin instance
     */
    public final Optional<UserData> getUserData(@NotNull HuskSync plugin) {
        final UserDataBuilder builder = UserData.builder(getMinecraftVersion());
        if (isOffline()) {
            return Optional.empty();
        }

        try {
            final Settings settings = plugin.getSettings();
            if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.INVENTORIES)) {
                if (isDead() && settings.isSynchroniseDeadPlayersChangingServer()) {
                    plugin.debug("Player " + username + " is dead, so their inventory will be set to empty.");
                    builder.setInventory(ItemData.empty());
                } else {
                    builder.setInventory(getInventory());
                }
            }
            if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.ENDER_CHESTS)) {
                builder.setEnderChest(getEnderChest());
            }
            builder.setStatus(getStatus());
            if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.POTION_EFFECTS)) {
                builder.setPotionEffects(getPotionEffects());
            }
            if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.ADVANCEMENTS)) {
                builder.setAdvancements(getAdvancements());
            }
            if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.STATISTICS)) {
                builder.setStatistics(getStatistics());
            }
            if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.LOCATION)) {
                builder.setLocation(getLocation());
            }
            if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.PERSISTENT_DATA_CONTAINER)) {
                builder.setPersistentDataContainer(getPersistentDataContainer());
            }
            return Optional.of(builder.build());
        } catch (Throwable e) {
            plugin.log(Level.SEVERE, "An error occurred while getting data for " + username + ": " + e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Handle a player's synchronization completion
     *
     * @param succeeded Whether the synchronization succeeded
     * @param plugin    The plugin instance
     */
    public void completeSynchronization(boolean succeeded, @NotNull HuskSync plugin) {
        if (succeeded) {
            switch (plugin.getSettings().getNotificationDisplaySlot()) {
                case CHAT -> plugin.getLocales().getLocale("synchronisation_complete")
                        .ifPresent(this::sendMessage);
                case ACTION_BAR -> plugin.getLocales().getLocale("synchronisation_complete")
                        .ifPresent(this::sendActionBar);
                case TOAST -> plugin.getLocales().getLocale("synchronisation_complete")
                        .ifPresent(locale -> this.sendToast(locale, new MineDown(""),
                                "minecraft:bell", "TASK"));
            }
            plugin.fireEvent(plugin.getSyncCompleteEvent(this), (event) -> {
                plugin.getLockedPlayers().remove(uuid);
            });
        } else {
            plugin.getLocales().getLocale("synchronisation_failed")
                    .ifPresent(this::sendMessage);
        }
        plugin.getDatabase().ensureUser(this);
    }

    /**
     * Get if the player is locked
     *
     * @return the player's locked status
     */
    public abstract boolean isLocked();

    /**
     * Get if the player is a NPC
     *
     * @return if the player is a NPC with metadata
     */
    public abstract boolean isNpc();
}
