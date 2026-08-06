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

package net.william278.husksync.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import net.william278.husksync.BukkitHuskSync;
import net.william278.husksync.data.BukkitData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class BukkitAdvancementAnnounceMuter {

    public static void register(@NotNull BukkitHuskSync plugin) {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
                plugin, ListenerPriority.NORMAL, PacketType.Play.Server.SYSTEM_CHAT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                final WrappedChatComponent chat = event.getPacket().getChatComponents().readSafely(0);
                if (chat == null) return;
                final String json = chat.getJson();
                if (json == null || !json.contains("\"chat.type.advancement")) return;

                for (UUID uuid : BukkitData.Advancements.SUPPRESS_ANNOUNCE) {
                    final Player p = Bukkit.getPlayer(uuid);
                    if (p != null && json.contains("\"" + p.getName() + "\"")) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        });
    }
}