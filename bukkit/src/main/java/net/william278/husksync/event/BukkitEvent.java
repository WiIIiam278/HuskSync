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
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public abstract class BukkitEvent extends Event implements net.william278.husksync.event.Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    protected BukkitEvent() {
    }

    @Override
    public CompletableFuture<net.william278.husksync.event.Event> fire() {
        final CompletableFuture<net.william278.husksync.event.Event> eventFireFuture = new CompletableFuture<>();
        // Don't fire events while the server is shutting down
        if (!BukkitHuskSync.getInstance().isEnabled()) {
            eventFireFuture.complete(this);
        } else {
            Bukkit.getScheduler().runTask(BukkitHuskSync.getInstance(), () -> {
                Bukkit.getServer().getPluginManager().callEvent(this);
                eventFireFuture.complete(this);
            });
        }
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
