package net.william278.husksync.listener;

import net.william278.husksync.HuskSync;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.redis.RedisManager;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EventListener {

    private final HuskSync huskSync;
    private final HashSet<UUID> usersAwaitingSync;

    protected EventListener(@NotNull HuskSync huskSync) {
        this.huskSync = huskSync;
        this.usersAwaitingSync = new HashSet<>();
    }

    public final void handlePlayerJoin(@NotNull OnlineUser user) {
        usersAwaitingSync.add(user.uuid);
        huskSync.getRedisManager().getUserData(user, RedisManager.RedisKeyType.SERVER_CHANGE).thenAccept(
                cachedUserData -> cachedUserData.ifPresentOrElse(
                        userData -> user.setData(userData, huskSync.getSettings()).join(),
                        () -> huskSync.getDatabase().getCurrentUserData(user).thenAccept(
                                databaseUserData -> databaseUserData.ifPresent(
                                        data -> user.setData(data, huskSync.getSettings()).join())).join())).thenRunAsync(
                () -> {
                    huskSync.getLocales().getLocale("synchronisation_complete").ifPresent(user::sendActionBar);
                    usersAwaitingSync.remove(user.uuid);
                    huskSync.getDatabase().ensureUser(user).join();
                });
    }

    public final void handlePlayerQuit(@NotNull OnlineUser user) {
        user.getUserData().thenAccept(userData -> huskSync.getRedisManager()
                .setPlayerData(user, userData, RedisManager.RedisKeyType.SERVER_CHANGE).thenRun(
                        () -> huskSync.getDatabase().setUserData(user, userData).join()));
    }

    public final void handleWorldSave(@NotNull List<OnlineUser> usersInWorld) {
        CompletableFuture.runAsync(() -> usersInWorld.forEach(user ->
                huskSync.getDatabase().setUserData(user, user.getUserData().join()).join()));
    }

    public final boolean cancelPlayerEvent(@NotNull OnlineUser user) {
        return usersAwaitingSync.contains(user.uuid);
    }

}
