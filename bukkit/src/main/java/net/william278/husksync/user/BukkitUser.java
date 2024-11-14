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
import dev.triumphteam.gui.builder.gui.StorageBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.StorageGui;
import net.william278.husksync.HuskSync;
import net.william278.husksync.data.BukkitData;
import net.william278.husksync.data.BukkitUserDataHolder;
import net.william278.husksync.data.Data;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Bukkit platform implementation of an {@link OnlineUser}
 */
public class BukkitUser extends OnlineUser implements BukkitUserDataHolder {

    private final HuskSync plugin;
    private final Player player;

    private BukkitUser(@NotNull Player player, @NotNull HuskSync plugin) {
        super(player.getUniqueId(), player.getName());
        this.player = player;
        this.plugin = plugin;
    }

    @NotNull
    @ApiStatus.Internal
    public static BukkitUser adapt(@NotNull Player player, @NotNull HuskSync plugin) {
        return new BukkitUser(player, plugin);
    }

    @Override
    public boolean isOffline() {
        return player == null || !player.isOnline();
    }

    @Override
    @Deprecated(since = "3.6.7")
    public void sendToast(@NotNull MineDown title, @NotNull MineDown description,
                          @NotNull String iconMaterial, @NotNull String backgroundType) {
        plugin.log(Level.WARNING, "Toast notifications are deprecated. " +
                                  "Please change your notification display slot to CHAT, ACTION_BAR or NONE.");
        this.sendActionBar(title);
    }

    @Override
    public void showGui(@NotNull Data.Items items, @NotNull MineDown title, boolean editable, int size,
                        @NotNull Consumer<Data.Items> onClose) {
        final ItemStack[] contents = ((BukkitData.Items) items).getContents();
        final StorageBuilder builder = Gui.storage().rows((int) Math.ceil(size / 9.0d));
        if (!editable) {
            builder.disableAllInteractions();
        }
        final StorageGui gui = builder
                .apply(a -> a.getInventory().setContents(contents))
                .title(title.toComponent()).create();
        gui.setCloseGuiAction((close) -> onClose.accept(BukkitData.Items.ItemArray.adapt(
                Arrays.stream(close.getInventory().getContents()).limit(size).toArray(ItemStack[]::new)
        )));
        plugin.runSync(() -> gui.open(player), this);
    }

    @Override
    public boolean hasPermission(@NotNull String node) {
        return player.hasPermission(node);
    }

    @Override
    public boolean isDead() {
        return player.getHealth() <= 0;
    }

    @Override
    public boolean isLocked() {
        return plugin.getLockedPlayers().contains(player.getUniqueId());
    }

    @Override
    public boolean isNpc() {
        return player.hasMetadata("NPC");
    }

    /**
     * Get the Bukkit {@link Player} instance of this user
     *
     * @return the {@link Player} instance
     * @since 3.6
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    @Override
    @ApiStatus.Internal
    public HuskSync getPlugin() {
        return plugin;
    }
}
