package net.william278.husksync;

import net.william278.husksync.config.Locales;
import net.william278.husksync.config.Settings;
import net.william278.husksync.data.DataAdapter;
import net.william278.husksync.editor.DataEditor;
import net.william278.husksync.database.Database;
import net.william278.husksync.event.EventCannon;
import net.william278.husksync.migrator.Migrator;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.redis.RedisManager;
import net.william278.husksync.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface HuskSync {

    @NotNull Set<OnlineUser> getOnlineUsers();

    @NotNull Optional<OnlineUser> getOnlineUser(@NotNull UUID uuid);

    @NotNull Database getDatabase();

    @NotNull RedisManager getRedisManager();

    @NotNull DataAdapter getDataAdapter();

    @NotNull DataEditor getDataEditor();

    @NotNull EventCannon getEventCannon();

    @NotNull List<Migrator> getAvailableMigrators();

    @NotNull Settings getSettings();

    @NotNull Locales getLocales();

    @NotNull Logger getLoggingAdapter();

    @NotNull String getVersion();

    CompletableFuture<Boolean> reload();

}
