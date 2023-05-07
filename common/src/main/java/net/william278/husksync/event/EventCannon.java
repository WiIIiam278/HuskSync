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

import net.william278.husksync.data.DataSaveCause;
import net.william278.husksync.data.UserData;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.player.User;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Used to fire plugin {@link Event}s
 */
public abstract class EventCannon {

    protected EventCannon() {
    }

    /**
     * Fires a {@link PreSyncEvent}
     *
     * @param user     The user to fire the event for
     * @param userData The user data to fire the event with
     * @return A future that will be completed when the event is fired
     */
    public abstract CompletableFuture<Event> firePreSyncEvent(@NotNull OnlineUser user, @NotNull UserData userData);

    /**
     * Fires a {@link DataSaveEvent}
     *
     * @param user     The user to fire the event for
     * @param userData The user data to fire the event with
     * @return A future that will be completed when the event is fired
     */
    public abstract CompletableFuture<Event> fireDataSaveEvent(@NotNull User user, @NotNull UserData userData,

                                                               @NotNull DataSaveCause saveCause);

    /**
     * Fires a {@link SyncCompleteEvent}
     *
     * @param user The user to fire the event for
     */
    public abstract void fireSyncCompleteEvent(@NotNull OnlineUser user);

}
