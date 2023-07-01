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
