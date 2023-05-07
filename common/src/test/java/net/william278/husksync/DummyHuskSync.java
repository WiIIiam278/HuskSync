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

package net.william278.husksync;

import net.william278.desertwell.util.Version;
import net.william278.husksync.config.Locales;
import net.william278.husksync.config.Settings;
import net.william278.husksync.data.DataAdapter;
import net.william278.husksync.database.Database;
import net.william278.husksync.event.EventCannon;
import net.william278.husksync.migrator.Migrator;
import net.william278.husksync.player.OnlineUser;
import net.william278.husksync.redis.RedisManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class DummyHuskSync implements HuskSync {
    @Override
    @NotNull
    public Set<OnlineUser> getOnlineUsers() {
        return Set.of();
    }

    @Override
    @NotNull
    public Optional<OnlineUser> getOnlineUser(@NotNull UUID uuid) {
        return Optional.empty();
    }

    @Override
    @NotNull
    public Database getDatabase() {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public RedisManager getRedisManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public DataAdapter getDataAdapter() {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public EventCannon getEventCannon() {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public List<Migrator> getAvailableMigrators() {
        return List.of();
    }

    @Override
    @NotNull
    public Settings getSettings() {
        return new Settings();
    }

    @Override
    @NotNull
    public Locales getLocales() {
        return new Locales();
    }

    @Override
    public InputStream getResource(@NotNull String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void log(@NotNull Level level, @NotNull String message, @NotNull Throwable... throwable) {
        System.out.println(message);
    }

    @Override
    @NotNull
    public Version getPluginVersion() {
        return Version.fromString("1.0.0");
    }

    @Override
    @NotNull
    public File getDataFolder() {
        return new File(".");
    }

    @Override
    @NotNull
    public Version getMinecraftVersion() {
        return Version.fromString("1.16.2");
    }

    @Override
    public CompletableFuture<Boolean> reload() {
        return CompletableFuture.supplyAsync(() -> true);
    }

    @Override
    public Set<UUID> getLockedPlayers() {
        return Set.of();
    }
}
