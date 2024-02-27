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

package net.william278.husksync.api;

import net.william278.desertwell.util.ThrowingConsumer;
import net.william278.husksync.BukkitHuskSync;
import net.william278.husksync.data.BukkitData;
import net.william278.husksync.data.DataHolder;
import net.william278.husksync.user.BukkitUser;
import net.william278.husksync.user.OnlineUser;
import net.william278.husksync.user.User;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * The HuskSync API implementation for the Bukkit platform
 * </p>
 * Retrieve an instance of the API class via {@link #getInstance()}.
 */
@SuppressWarnings("unused")
public class BukkitHuskSyncAPI extends HuskSyncAPI {

    /**
     * <b>(Internal use only)</b> - Constructor, instantiating the API.
     */
    @ApiStatus.Internal
    private BukkitHuskSyncAPI(@NotNull BukkitHuskSync plugin) {
        super(plugin);
    }

    /**
     * Entrypoint to the HuskSync API on the bukkit platform - returns an instance of the API
     *
     * @return instance of the HuskSync API
     * @since 3.0
     */
    @NotNull
    public static BukkitHuskSyncAPI getInstance() {
        if (instance == null) {
            throw new NotRegisteredException();
        }
        return (BukkitHuskSyncAPI) instance;
    }

    /**
     * <b>(Internal use only)</b> - Register the API for this platform.
     *
     * @param plugin the plugin instance
     * @since 3.0
     */
    @ApiStatus.Internal
    public static void register(@NotNull BukkitHuskSync plugin) {
        instance = new BukkitHuskSyncAPI(plugin);
    }

    /**
     * Returns a {@link OnlineUser} instance for the given bukkit {@link Player}.
     *
     * @param player the bukkit player to get the {@link OnlineUser} instance for
     * @return the {@link OnlineUser} instance for the given bukkit {@link Player}
     * @since 2.0
     */
    @NotNull
    public BukkitUser getUser(@NotNull Player player) {
        return BukkitUser.adapt(player, plugin);
    }

    /**
     * Get the current {@link BukkitData.Items.Inventory} of the given {@link User}
     *
     * @param user the user to get the inventory of
     * @return the {@link BukkitData.Items.Inventory} of the given {@link User}
     * @since 3.0
     */
    public CompletableFuture<Optional<BukkitData.Items.Inventory>> getCurrentInventory(@NotNull User user) {
        return getCurrentData(user).thenApply(data -> data.flatMap(DataHolder::getInventory)
                .map(BukkitData.Items.Inventory.class::cast));
    }

    /**
     * Get the current {@link BukkitData.Items.Inventory} of the given {@link Player}
     *
     * @param user the user to get the inventory of
     * @return the {@link BukkitData.Items.Inventory} of the given {@link Player}
     * @since 3.0
     */
    public CompletableFuture<Optional<ItemStack[]>> getCurrentInventoryContents(@NotNull User user) {
        return getCurrentInventory(user)
                .thenApply(inventory -> inventory.map(BukkitData.Items.Inventory::getContents));
    }

    /**
     * Set the current {@link BukkitData.Items.Inventory} of the given {@link User}
     *
     * @param user     the user to set the inventory of
     * @param contents the contents to set the inventory to
     * @since 3.0
     */
    public void setCurrentInventory(@NotNull User user, @NotNull BukkitData.Items.Inventory contents) {
        editCurrentData(user, dataHolder -> dataHolder.setInventory(contents));
    }

    /**
     * Set the current {@link BukkitData.Items.Inventory} of the given {@link User}
     *
     * @param user     the user to set the inventory of
     * @param contents the contents to set the inventory to
     * @since 3.0
     */
    public void setCurrentInventoryContents(@NotNull User user, @NotNull ItemStack[] contents) {
        editCurrentData(
                user,
                dataHolder -> dataHolder.getInventory().ifPresent(
                        inv -> inv.setContents(adaptItems(contents))
                )
        );
    }

    /**
     * Edit the current {@link BukkitData.Items.Inventory} of the given {@link User}
     *
     * @param user   the user to edit the inventory of
     * @param editor the editor to apply to the inventory
     * @since 3.0
     */
    public void editCurrentInventory(@NotNull User user, ThrowingConsumer<BukkitData.Items.Inventory> editor) {
        editCurrentData(user, dataHolder -> dataHolder.getInventory()
                .map(BukkitData.Items.Inventory.class::cast)
                .ifPresent(editor));
    }

    /**
     * Edit the current {@link BukkitData.Items.Inventory} of the given {@link User}
     *
     * @param user   the user to edit the inventory of
     * @param editor the editor to apply to the inventory
     * @since 3.0
     */
    public void editCurrentInventoryContents(@NotNull User user, ThrowingConsumer<ItemStack[]> editor) {
        editCurrentData(user, dataHolder -> dataHolder.getInventory()
                .map(BukkitData.Items.Inventory.class::cast)
                .ifPresent(inventory -> editor.accept(inventory.getContents())));
    }

    /**
     * Get the current {@link BukkitData.Items.EnderChest} of the given {@link User}
     *
     * @param user the user to get the ender chest of
     * @return the {@link BukkitData.Items.EnderChest} of the given {@link User}, or {@link Optional#empty()} if the
     * user data could not be found
     * @since 3.0
     */
    public CompletableFuture<Optional<BukkitData.Items.EnderChest>> getCurrentEnderChest(@NotNull User user) {
        return getCurrentData(user).thenApply(data -> data.flatMap(DataHolder::getEnderChest)
                .map(BukkitData.Items.EnderChest.class::cast));
    }

    /**
     * Get the current {@link BukkitData.Items.EnderChest} of the given {@link Player}
     *
     * @param user the user to get the ender chest of
     * @return the {@link BukkitData.Items.EnderChest} of the given {@link Player}, or {@link Optional#empty()} if the
     * user data could not be found
     * @since 3.0
     */
    public CompletableFuture<Optional<ItemStack[]>> getCurrentEnderChestContents(@NotNull User user) {
        return getCurrentEnderChest(user)
                .thenApply(enderChest -> enderChest.map(BukkitData.Items.EnderChest::getContents));
    }

    /**
     * Set the current {@link BukkitData.Items.EnderChest} of the given {@link User}
     *
     * @param user     the user to set the ender chest of
     * @param contents the contents to set the ender chest to
     * @since 3.0
     */
    public void setCurrentEnderChest(@NotNull User user, @NotNull BukkitData.Items.EnderChest contents) {
        editCurrentData(user, dataHolder -> dataHolder.setEnderChest(contents));
    }

    /**
     * Set the current {@link BukkitData.Items.EnderChest} of the given {@link User}
     *
     * @param user     the user to set the ender chest of
     * @param contents the contents to set the ender chest to
     * @since 3.0
     */
    public void setCurrentEnderChestContents(@NotNull User user, @NotNull ItemStack[] contents) {
        editCurrentData(
                user,
                dataHolder -> dataHolder.getEnderChest().ifPresent(
                        enderChest -> enderChest.setContents(adaptItems(contents))
                )
        );
    }

    /**
     * Edit the current {@link BukkitData.Items.EnderChest} of the given {@link User}
     *
     * @param user   the user to edit the ender chest of
     * @param editor the editor to apply to the ender chest
     * @since 3.0
     */
    public void editCurrentEnderChest(@NotNull User user, Consumer<BukkitData.Items.EnderChest> editor) {
        editCurrentData(user, dataHolder -> dataHolder.getEnderChest()
                .map(BukkitData.Items.EnderChest.class::cast)
                .ifPresent(editor));
    }

    /**
     * Edit the current {@link BukkitData.Items.EnderChest} of the given {@link User}
     *
     * @param user   the user to edit the ender chest of
     * @param editor the editor to apply to the ender chest
     * @since 3.0
     */
    public void editCurrentEnderChestContents(@NotNull User user, Consumer<ItemStack[]> editor) {
        editCurrentData(user, dataHolder -> dataHolder.getEnderChest()
                .map(BukkitData.Items.EnderChest.class::cast)
                .ifPresent(enderChest -> editor.accept(enderChest.getContents())));
    }

    /**
     * Adapts an array of {@link ItemStack} to a {@link BukkitData.Items} instance
     *
     * @param contents the contents to adapt
     * @return the adapted {@link BukkitData.Items} instance
     * @since 3.0
     */
    @NotNull
    public BukkitData.Items adaptItems(@NotNull ItemStack[] contents) {
        return BukkitData.Items.ItemArray.adapt(contents);
    }

}
