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
import net.minecraft.server.network.ServerPlayerEntity;
import net.william278.desertwell.util.Version;
import net.william278.husksync.FabricHuskSync;
import net.william278.husksync.config.Settings;
import net.william278.husksync.data.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class FabricPlayer extends OnlineUser {
    private final ServerPlayerEntity player;
    private final Audience audience;

    private FabricPlayer(@NotNull ServerPlayerEntity player) {
        super(player.getUuid(), player.getName().getString());
        this.player = player;
        this.audience = FabricHuskSync.INSTANCE.getAudiences().player(player.getUuid());
    }

    @NotNull
    public static FabricPlayer adapt(@NotNull ServerPlayerEntity player) {
        return new FabricPlayer(player);
    }

    @SuppressWarnings("all")
    private static double getMaxHealth(@NotNull ServerPlayerEntity player) {
        double maxHealth = player.getMaxHealth();

        // TODO: If the player has additional health bonuses from synchronised potion effects,
        //  subtract these from this number as they are synchronised separately

        return maxHealth;
    }

    @Override
    public CompletableFuture<StatusData> getStatus() {
        return CompletableFuture.supplyAsync(() -> {
            // TODO: StatusData
            return new StatusData(Math.min(player.getHealth(), player.getMaxHealth()),
                    player.getMaxHealth(),
                    player.getHealth() == player.getMaxHealth() ? player.getHealth() / player.getMaxHealth() : 0d,
                    player.getHungerManager().getFoodLevel(),
                    player.getHungerManager().getSaturationLevel(),
                    player.getHungerManager().getExhaustion(),
                    player.getInventory().selectedSlot,
                    player.totalExperience,
                    player.experienceLevel,
                    player.experienceProgress,
                    // Warning, there is a behavioral difference here due to the lack of a suitable method
                    // to obtain the current player's game mode,
                    // which may result in incorrect transmission of the game mode.
                    player.isCreative() ? "CREATIVE" : player.isSpectator() ? "SPECTATOR" : "SURVIVAL",
                    player.getAbilities().allowFlying && player.getAbilities().flying
            );
        });
    }

    @Override
    public CompletableFuture<Void> setStatus(@NotNull StatusData statusData, @NotNull Settings settings) {
        return null;
    }

    @Override
    public CompletableFuture<ItemData> getInventory() {
        return null;
    }

    @Override
    public CompletableFuture<Void> setInventory(@NotNull ItemData itemData) {
        return null;
    }

    @Override
    public CompletableFuture<ItemData> getEnderChest() {
        return null;
    }

    @Override
    public CompletableFuture<Void> setEnderChest(@NotNull ItemData itemData) {
        return null;
    }

    @Override
    public CompletableFuture<PotionEffectData> getPotionEffects() {
        return null;
    }

    @Override
    public CompletableFuture<Void> setPotionEffects(@NotNull PotionEffectData potionEffectData) {
        return null;
    }

    @Override
    public CompletableFuture<List<AdvancementData>> getAdvancements() {
        return null;
    }

    @Override
    public CompletableFuture<Void> setAdvancements(@NotNull List<AdvancementData> list) {
        return null;
    }

    @Override
    public CompletableFuture<StatisticsData> getStatistics() {
        return null;
    }

    @Override
    public CompletableFuture<Void> setStatistics(@NotNull StatisticsData statisticsData) {
        return null;
    }

    @Override
    public CompletableFuture<LocationData> getLocation() {
        return null;
    }

    @Override
    public CompletableFuture<Void> setLocation(@NotNull LocationData locationData) {
        return null;
    }

    @Override
    public CompletableFuture<PersistentDataContainerData> getPersistentDataContainer() {
        return null;
    }

    @Override
    public CompletableFuture<Void> setPersistentDataContainer(@NotNull PersistentDataContainerData persistentDataContainerData) {
        return null;
    }

    @Override
    public boolean isOffline() {
        return false;
    }

    @Override
    public @NotNull Version getMinecraftVersion() {
        return FabricHuskSync.INSTANCE.getMinecraftVersion();
    }

    @Override
    public @NotNull Audience getAudience() {
        return this.audience;
    }

    @Override
    public void sendMessage(@NotNull MineDown mineDown) {

    }

    @Override
    public void sendActionBar(@NotNull MineDown mineDown) {

    }

    @Override
    public void sendToast(@NotNull MineDown mineDown, @NotNull MineDown mineDown1, @NotNull String s, @NotNull String s1) {

    }

    @Override
    public boolean hasPermission(@NotNull String s) {
        return false;
    }

    @Override
    public CompletableFuture<Optional<ItemData>> showMenu(@NotNull ItemData itemData, boolean b, int i, @NotNull MineDown mineDown) {
        return null;
    }

    @Override
    public boolean isDead() {
        return false;
    }

    @Override
    public boolean isLocked() {
        return false;
    }

    @Override
    public boolean isNpc() {
        return false;
    }
}
