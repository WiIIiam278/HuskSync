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
import net.kyori.adventure.audience.Audience;
import net.roxeez.advancement.display.FrameType;
import net.william278.andjam.Toast;
import net.william278.husksync.BukkitHuskSync;
import net.william278.husksync.HuskSync;
import net.william278.husksync.data.BukkitDataOwner;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Bukkit platform implementation of an {@link OnlineUser}
 */
public class BukkitUser extends OnlineUser implements BukkitDataOwner {

    private final HuskSync plugin;
    private final Player player;

    private BukkitUser(@NotNull Player player, @NotNull HuskSync plugin) {
        super(player.getUniqueId(), player.getName());
        this.plugin = plugin;
        this.player = player;
    }

    @NotNull
    public static BukkitUser adapt(@NotNull Player player, @NotNull HuskSync plugin) {
        return new BukkitUser(player, plugin);
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public boolean isOffline() {
        return player == null || !player.isOnline();
    }

    @NotNull
    @Override
    public Audience getAudience() {
        return ((BukkitHuskSync) plugin).getAudiences().player(player);
    }

    @Override
    public void sendToast(@NotNull MineDown title, @NotNull MineDown description,
                          @NotNull String iconMaterial, @NotNull String backgroundType) {
        try {
            final Material material = Material.matchMaterial(iconMaterial);
            Toast.builder((BukkitHuskSync) plugin)
                    .setTitle(title.toComponent())
                    .setDescription(description.toComponent())
                    .setIcon(material != null ? material : Material.BARRIER)
                    .setFrameType(FrameType.valueOf(backgroundType))
                    .build()
                    .show(player);
        } catch (Throwable e) {
            plugin.log(Level.WARNING, "Failed to send toast to player " + player.getName(), e);
        }
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

    @NotNull
    @Override
    public Player getBukkitPlayer() {
        return player;
    }

    @NotNull
    @Override
    public HuskSync getPlugin() {
        return plugin;
    }
}