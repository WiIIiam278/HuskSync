/*
 * This file is part of HuskSync by William278. Do not redistribute!
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  All rights reserved.
 *
 *  This source code is provided as reference to licensed individuals that have purchased the HuskSync
 *  plugin once from any of the official sources it is provided. The availability of this code does
 *  not grant you the rights to modify, re-distribute, compile or redistribute this source code or
 *  "plugin" outside this intended purpose. This license does not cover libraries developed by third
 *  parties that are utilised in the plugin.
 */

package net.william278.husksync.event;

import net.william278.husksync.BukkitHuskSync;
import net.william278.husksync.player.BukkitPlayer;
import net.william278.husksync.player.OnlineUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public abstract class BukkitPlayerEvent extends BukkitEvent implements PlayerEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    protected final Player player;

    protected BukkitPlayerEvent(@NotNull Player player) {
        this.player = player;
    }

    @Override
    public OnlineUser getUser() {
        return BukkitPlayer.adapt(player);
    }

    @Override
    public CompletableFuture<Event> fire() {
        final CompletableFuture<Event> eventFireFuture = new CompletableFuture<>();
        BukkitHuskSync.getInstance().runSync(() -> {
            Bukkit.getServer().getPluginManager().callEvent(this);
            eventFireFuture.complete(this);
        });
        return eventFireFuture;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }


}
