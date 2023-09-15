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
import net.william278.husksync.HuskSync;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.data.PlayerDataHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents a logged-in {@link User}
 */
public abstract class OnlineUser extends User implements CommandUser, PlayerDataHolder {

    public OnlineUser(@NotNull UUID uuid, @NotNull String username) {
        super(uuid, username);
    }

    /**
     * Indicates if the player has gone offline
     *
     * @return {@code true} if the player has left the server; {@code false} otherwise
     */
    public abstract boolean isOffline();

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
     * @param iconMaterial   the namespace-keyed material to use as an hasIcon of the toast
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
     * Returns true if the player is dead
     *
     * @return true if the player is dead
     */
    public abstract boolean isDead();

    /**
     * Set a player's status from a {@link DataSnapshot}
     *
     * @param snapshot The {@link DataSnapshot} to set the player's status from
     */
    public void applySnapshot(@NotNull DataSnapshot.Packed snapshot) {
        getPlugin().fireEvent(getPlugin().getPreSyncEvent(this, snapshot), (event) -> {
            if (!isOffline()) {
                PlayerDataHolder.super.applySnapshot(
                        event.getData(), (owner) -> completeSync(true, getPlugin())
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
    public void completeSync(boolean succeeded, @NotNull HuskSync plugin) {
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
            plugin.fireEvent(plugin.getSyncCompleteEvent(this), (event) -> plugin.getLockedPlayers().remove(getUuid()));
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
