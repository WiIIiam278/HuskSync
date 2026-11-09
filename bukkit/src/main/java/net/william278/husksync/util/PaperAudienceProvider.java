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

package net.william278.husksync.util;

import io.papermc.paper.text.PaperComponents;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.AudienceProvider;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

// Audiences backed by Paper's native Adventure support. The adventure-platform-bukkit audiences used on Spigot must not
// be used on Paper: they'd call Paper's own Adventure serializers with HuskSync's relocated Gson types (NoSuchMethodError)
public class PaperAudienceProvider implements AudienceProvider {

    private final Server server;

    public PaperAudienceProvider(@NotNull Server server) {
        this.server = server;
    }

    @NotNull
    @Override
    public Audience all() {
        return server;
    }

    @NotNull
    @Override
    public Audience console() {
        return server.getConsoleSender();
    }

    @NotNull
    @Override
    public Audience players() {
        return (ForwardingAudience) () -> List.copyOf(server.getOnlinePlayers());
    }

    @NotNull
    @Override
    public Audience player(@NotNull UUID playerId) {
        final Player player = server.getPlayer(playerId);
        return player != null ? player : Audience.empty();
    }

    @NotNull
    @Override
    public Audience permission(@NotNull String permission) {
        return (ForwardingAudience) () -> Stream.<CommandSender>concat(
                Stream.of(server.getConsoleSender()), server.getOnlinePlayers().stream()
        ).filter(sender -> sender.hasPermission(permission)).toList();
    }

    @NotNull
    @Override
    public Audience world(@NotNull Key world) {
        final World bukkitWorld = server.getWorld(new NamespacedKey(world.namespace(), world.value()));
        return bukkitWorld != null ? bukkitWorld : Audience.empty();
    }

    @NotNull
    @Override
    public Audience server(@NotNull String serverName) {
        return all();
    }

    @NotNull
    @Override
    public ComponentFlattener flattener() {
        return PaperComponents.flattener();
    }

    @Override
    public void close() {
    }

}
