package net.william278.husksync.listener;

import net.william278.husksync.HuskSync;
import net.william278.husksync.config.Settings;
import net.william278.husksync.player.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class EventListener {

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
        if (user.isDead()) {
            return;
        }
        usersAwaitingSync.add(user.uuid);
        CompletableFuture.runAsync(() -> huskSync.getRedisManager().getUserServerSwitch(user).thenAccept(changingServers -> {
            if (!changingServers) {
                // Fetch from the database if the user isn't changing servers
                setUserFromDatabase(user).thenRun(() -> handleSynchronisationCompletion(user));
            } else {
                final int TIME_OUT_MILLISECONDS = 3200;
                CompletableFuture.runAsync(() -> {
                    final AtomicInteger currentMilliseconds = new AtomicInteger(0);
                    final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

                    // Set the user as soon as the source server has set the data to redis
                    executor.scheduleAtFixedRate(() -> {
                        if (disabling || currentMilliseconds.get() > TIME_OUT_MILLISECONDS) {
                            executor.shutdown();
                            setUserFromDatabase(user).thenRun(() -> handleSynchronisationCompletion(user));
                            return;
                        }
                        huskSync.getRedisManager().getUserData(user).thenAccept(redisUserData ->
                                redisUserData.ifPresent(redisData -> {
                                    user.setData(redisData, huskSync.getSettings()).join();
                                    executor.shutdown();
                                })).join();
                        currentMilliseconds.addAndGet(200);
                    }, 0, 200L, TimeUnit.MILLISECONDS);
                });
            }
        }));
    }

    private CompletableFuture<Void> setUserFromDatabase(@NotNull OnlineUser user) {
        return huskSync.getDatabase().getCurrentUserData(user)
                .thenAccept(databaseUserData -> databaseUserData.ifPresent(databaseData -> user
                        .setData(databaseData.userData(), huskSync.getSettings()).join()));
    }

    private void handleSynchronisationCompletion(@NotNull OnlineUser user) {
        huskSync.getLocales().getLocale("synchronisation_complete").ifPresent(user::sendActionBar);
        usersAwaitingSync.remove(user.uuid);
        huskSync.getDatabase().ensureUser(user).join();
    }

    public final void handlePlayerQuit(@NotNull OnlineUser user) {
        if (disabling) {
            return;
        }
        huskSync.getRedisManager().setUserServerSwitch(user).thenRun(() -> user.getUserData().thenAccept(
                userData -> huskSync.getRedisManager().setUserData(user, userData).thenRun(
                        () -> huskSync.getDatabase().setUserData(user, userData).join())));
    }

    public final void handleWorldSave(@NotNull List<OnlineUser> usersInWorld) {
        if (disabling || !huskSync.getSettings().getBooleanValue(Settings.ConfigOption.SYNCHRONIZATION_SAVE_ON_WORLD_SAVE)) {
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
