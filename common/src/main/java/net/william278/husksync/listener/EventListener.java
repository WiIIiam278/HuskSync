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

    /**
     * The plugin instance
     */
    private final HuskSync huskSync;

    /**
     * Set of UUIDs current awaiting item synchronization. Events will be cancelled for these users
     */
    private final HashSet<UUID> usersAwaitingSync;

    /**
     * Whether the plugin is currently being disabled
     */
    private boolean disabling;

    protected EventListener(@NotNull HuskSync huskSync) {
        this.huskSync = huskSync;
        this.usersAwaitingSync = new HashSet<>();
        this.disabling = false;
    }

    public final void handlePlayerJoin(@NotNull OnlineUser user) {
        usersAwaitingSync.add(user.uuid);
        huskSync.getRedisManager().getUserData(user, RedisManager.RedisKeyType.SERVER_CHANGE).thenAccept(
                cachedUserData -> cachedUserData.ifPresentOrElse(
                        userData -> user.setData(userData, huskSync.getSettings()).join(),
                        () -> huskSync.getDatabase().getCurrentUserData(user).thenAccept(
                                databaseUserData -> databaseUserData.ifPresent(
                                        data -> user.setData(data.userData(), huskSync.getSettings()).join())).join())).thenRunAsync(
                () -> {
                    huskSync.getLocales().getLocale("synchronisation_complete").ifPresent(user::sendActionBar);
                    usersAwaitingSync.remove(user.uuid);
                    huskSync.getDatabase().ensureUser(user).join();
                });
    }

    public final void handlePlayerQuit(@NotNull OnlineUser user) {
        if (disabling) {
            return;
        }
        user.getUserData().thenAccept(userData -> {
            System.out.println(userData.userData().toJson());
            huskSync.getRedisManager()
                    .setUserData(user, userData.userData(), RedisManager.RedisKeyType.SERVER_CHANGE).thenRun(
                            () -> huskSync.getDatabase().setUserData(user, userData).join());
        });
    }

    public final void handleWorldSave(@NotNull List<OnlineUser> usersInWorld) {
        if (disabling) {
            return;
        }
        CompletableFuture.runAsync(() -> usersInWorld.forEach(user ->
                huskSync.getDatabase().setUserData(user, user.getUserData().join()).join()));
    }

    public final void handlePluginDisable() {
        disabling = true;

        huskSync.getOnlineUsers().stream().filter(user -> !usersAwaitingSync.contains(user.uuid)).forEach(user ->
                huskSync.getDatabase().setUserData(user, user.getUserData().join()).join());

        huskSync.getDatabase().close();
        huskSync.getRedisManager().close();
    }

    public final boolean cancelPlayerEvent(@NotNull OnlineUser user) {
        return usersAwaitingSync.contains(user.uuid);
    }

}
