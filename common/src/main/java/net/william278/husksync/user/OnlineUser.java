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

package net.william278.husksync.user;

import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.william278.husksync.HuskSync;
import net.william278.husksync.data.Data;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.data.Identifier;
import net.william278.husksync.data.UserDataHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Represents a logged-in {@link User}
 */
public abstract class OnlineUser extends User implements CommandUser, UserDataHolder {

    public OnlineUser(@NotNull UUID uuid, @NotNull String username) {
        super(uuid, username);
    }

    /**
     * Indicates if the player has gone offline
     *
     * @return {@code true} if the player has left the server; {@code false} otherwise
     */
    public abstract boolean isOffline();

    @NotNull
    @Override
    public Audience getAudience() {
        return getPlugin().getAudience(getUuid());
    }

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
        sendMessage(mineDown.toComponent());
    }

    /**
     * Dispatch a MineDown-formatted action bar message to this player
     *
     * @param mineDown the parsed {@link MineDown} to send
     */
    public void sendActionBar(@NotNull MineDown mineDown) {
        getAudience().sendActionBar(mineDown.toComponent());
    }

    /**
     * Dispatch a toast message to this player
     *
     * @param title          the title of the toast
     * @param description    the description of the toast
     * @param iconMaterial   the namespace-keyed material to use as an hasIcon of the toast
     * @param backgroundType the background ("ToastType") of the toast
     * @deprecated No longer supported
     */
    @Deprecated(since = "3.6.7")
    public abstract void sendToast(@NotNull MineDown title, @NotNull MineDown description,
                                   @NotNull String iconMaterial, @NotNull String backgroundType);

    /**
     * Show a GUI chest menu to the user
     *
     * @param items    the items to fill the menu with
     * @param title    the title of the menu
     * @param editable whether the menu is editable (items can be removed or added)
     * @param size     the size of the menu
     * @param onClose  the action to perform when the menu is closed
     */
    public abstract void showGui(@NotNull Data.Items.Items items, @NotNull MineDown title, boolean editable, int size,
                                 @NotNull Consumer<Data.Items.Items> onClose);

    /**
     * Returns if the player has the permission node
     *
     * @param node The permission node string
     * @return {@code true} if the player has permission node; {@code false} otherwise
     */
    public abstract boolean hasPermission(@NotNull String node);


    /**
     * Set a player's status from a {@link DataSnapshot}
     *
     * @param snapshot The {@link DataSnapshot} to set the player's status from
     * @param cause    The {@link DataSnapshot.UpdateCause} of the snapshot
     * @since 3.0
     */
    public void applySnapshot(@NotNull DataSnapshot.Packed snapshot, @NotNull DataSnapshot.UpdateCause cause) {
        getPlugin().fireEvent(getPlugin().getPreSyncEvent(this, snapshot), (event) -> {
            if (!isOffline()) {
                getPlugin().debug(String.format("Applying snapshot (%s) to %s (cause: %s)",
                        snapshot.getShortId(), getUsername(), cause.getDisplayName()
                ));
                UserDataHolder.super.applySnapshot(
                        event.getData(), (succeeded) -> completeSync(succeeded, cause, getPlugin())
                );
            }
        });
    }

    /**
     * Handle a player's synchronization completion
     *
     * @param succeeded Whether the synchronization succeeded
     * @param plugin    The plugin instance
     */
    public void completeSync(boolean succeeded, @NotNull DataSnapshot.UpdateCause cause, @NotNull HuskSync plugin) {
        if (succeeded) {
            switch (plugin.getSettings().getSynchronization().getNotificationDisplaySlot()) {
                case CHAT -> cause.getCompletedLocale(plugin).ifPresent(this::sendMessage);
                case ACTION_BAR -> cause.getCompletedLocale(plugin).ifPresent(this::sendActionBar);
            }
            plugin.fireEvent(
                    plugin.getSyncCompleteEvent(this),
                    (event) -> plugin.unlockPlayer(getUuid())
            );
        } else {
            cause.getFailedLocale(plugin).ifPresent(this::sendMessage);
        }

        // Ensure the user is in the database
        plugin.getDatabase().ensureUser(this);
    }

    @NotNull
    @Override
    public Map<Identifier, Data> getCustomDataStore() {
        return getPlugin().getPlayerCustomDataStore(this);
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
