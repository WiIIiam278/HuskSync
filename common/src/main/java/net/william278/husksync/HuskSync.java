package net.william278.husksync;

import net.william278.husksync.config.Locales;
import net.william278.husksync.config.Settings;
import net.william278.husksync.listener.EventListener;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.redis.RedisManager;
import net.william278.husksync.database.Database;
import net.william278.husksync.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface HuskSync {

    @NotNull Set<OnlineUser> getOnlineUsers();

    @NotNull Optional<OnlineUser> getOnlineUser(@NotNull UUID uuid);

    @NotNull EventListener getEventListener();

    @NotNull Database getDatabase();

    @NotNull RedisManager getRedisManager();

    @NotNull Settings getSettings();

    @NotNull Locales getLocales();

    @NotNull Logger getLogger();

    @NotNull String getVersion();

    void reload();

}
