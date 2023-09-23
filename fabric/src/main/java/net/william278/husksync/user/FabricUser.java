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
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.kyori.adventure.audience.Audience;
import net.minecraft.server.network.ServerPlayerEntity;
import net.william278.husksync.FabricHuskSync;
import net.william278.husksync.HuskSync;
import net.william278.husksync.data.Data;
import net.william278.husksync.data.FabricUserDataHolder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class FabricUser extends OnlineUser implements FabricUserDataHolder {
    private final HuskSync plugin;
    private final ServerPlayerEntity player;

    private FabricUser(@NotNull ServerPlayerEntity player, @NotNull HuskSync plugin) {
        super(player.getUuid(), player.getName().getString());
        this.player = player;
        this.plugin = plugin;
    }

    @NotNull
    @ApiStatus.Internal
    public static FabricUser adapt(@NotNull ServerPlayerEntity player, @NotNull HuskSync plugin) {
        return new FabricUser(player, plugin);
    }

    @Override
    public boolean isOffline() {
        return player == null || player.isDisconnected();
    }

    @NotNull
    @Override
    public Audience getAudience() {
        return ((FabricHuskSync) plugin).getAudiences().player(player.getUuid());
    }

    @Override
    public void sendToast(@NotNull MineDown title, @NotNull MineDown description, @NotNull String iconMaterial,
                          @NotNull String backgroundType) {
        // TODO
    }

    @Override
    public void showGui(@NotNull Data.Items.Items items, @NotNull MineDown title, boolean editable, int size,
                        @NotNull Consumer<Data.Items> onClose) {
        // TODO
    }


    @Override
    public boolean hasPermission(@NotNull String node) {
        final boolean requiresOp = Boolean.TRUE.equals(
                ((FabricHuskSync) plugin).getPermissions().getOrDefault(node, true)
        );
        return Permissions.check(player, node, !requiresOp || player.hasPermissionLevel(3));
    }

    @Override
    public boolean isDead() {
        return player.getHealth() <= 0.0f;
    }

    @Override
    public boolean isLocked() {
        return plugin.getLockedPlayers().contains(player.getUuid());
    }

    @Override
    public boolean isNpc() {
        return false;
    }

    @Override
    @NotNull
    public ServerPlayerEntity getPlayer() {
        return player;
    }

    @NotNull
    @Override
    @ApiStatus.Internal
    public HuskSync getPlugin() {
        return plugin;
    }
}
