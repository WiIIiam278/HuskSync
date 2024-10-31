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
import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.william278.husksync.FabricHuskSync;
import net.william278.husksync.HuskSync;
import net.william278.husksync.data.Data;
import net.william278.husksync.data.FabricData;
import net.william278.husksync.data.FabricUserDataHolder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.logging.Level;

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
        return plugin.getAudiences().player(player.getUuid());
    }

    @Override
    @Deprecated(since = "3.6.7")
    public void sendToast(@NotNull MineDown title, @NotNull MineDown description, @NotNull String iconMaterial,
                          @NotNull String backgroundType) {
        plugin.log(Level.WARNING, "Toast notifications are deprecated. " +
                                  "Please change your notification display slot to CHAT, ACTION_BAR or NONE.");
        this.sendActionBar(title);
    }

    @Override
    public void showGui(@NotNull Data.Items.Items items, @NotNull MineDown title, boolean editable, int size,
                        @NotNull Consumer<Data.Items> onClose) {
        plugin.runSync(
                () -> new ItemViewerGui(size, player, title, (FabricData.Items) items, onClose, editable, plugin).open()
        );
    }

    private static class ItemViewerGui extends SimpleGui {

        private final Consumer<Data.Items> onClose;
        private final int size;
        private final boolean editable;

        public ItemViewerGui(int size, @NotNull ServerPlayerEntity player, @NotNull MineDown title,
                             @NotNull FabricData.Items items, @NotNull Consumer<Data.Items> onClose,
                             boolean editable, @NotNull HuskSync plugin) {
            super(getScreenHandler(size), player, false);
            this.onClose = onClose;
            this.size = size;
            this.editable = editable;

            // Set title, items
            this.setTitle(((MinecraftServerAudiences) plugin.getAudiences()).asNative(title.toComponent()));
            this.setLockPlayerInventory(!editable);
            for (int i = 0; i < size; i++) {
                final ItemStack item = items.getContents()[i];
                this.setSlot(i, item == null ? ItemStack.EMPTY : item);
            }
        }

        @Override
        public void onClose() {
            final ItemStack[] contents = new ItemStack[size];
            for (int i = 0; i < size; i++) {
                contents[i] = this.getSlot(i) == null ? null : this.getSlot(i).getItemStack();
            }
            onClose.accept(FabricData.Items.ItemArray.adapt(contents));
        }

        @Override
        public boolean onAnyClick(int index, @NotNull ClickType type, @NotNull SlotActionType action) {
            return editable;
        }

        @Override
        public boolean onClick(int index, @NotNull ClickType type, @NotNull SlotActionType action,
                               @NotNull GuiElementInterface element) {
            return editable;
        }

        @NotNull
        private static ScreenHandlerType<GenericContainerScreenHandler> getScreenHandler(int size) {
            return switch (size / 9 + (size % 9 == 0 ? 0 : 1)) {
                case 3 -> ScreenHandlerType.GENERIC_9X3;
                case 4 -> ScreenHandlerType.GENERIC_9X4;
                case 5 -> ScreenHandlerType.GENERIC_9X5;
                default -> ScreenHandlerType.GENERIC_9X6;
            };
        }
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
