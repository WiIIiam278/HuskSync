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
import com.google.common.collect.Sets;
import net.william278.husksync.BukkitHuskSync;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static com.comphenix.protocol.PacketType.Play.Client;

public class BukkitProtocolLibLockedPacketListener extends BukkitLockedEventListener implements LockedHandler {

    protected BukkitProtocolLibLockedPacketListener(@NotNull BukkitHuskSync plugin) {
        super(plugin);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        ProtocolLibrary.getProtocolManager().addPacketListener(new PlayerPacketAdapter(this));
        plugin.log(Level.INFO, "Using ProtocolLib to cancel packets for locked players");
    }

    private static class PlayerPacketAdapter extends PacketAdapter {

        // Packets we want the player to still be able to send/receiver to/from the server
        private static final Set<PacketType> ALLOWED_PACKETS = Set.of(
                Client.KEEP_ALIVE, Client.PONG, Client.CUSTOM_PAYLOAD, // Connection packets
                Client.CHAT_COMMAND, Client.CLIENT_COMMAND, Client.CHAT, Client.CHAT_SESSION_UPDATE, // Chat / command packets
                Client.POSITION, Client.POSITION_LOOK, Client.LOOK, // Movement packets
                Client.HELD_ITEM_SLOT, Client.ARM_ANIMATION, Client.TELEPORT_ACCEPT, // Animation packets
                Client.SETTINGS // Video setting packets
        );

        private final BukkitProtocolLibLockedPacketListener listener;

        public PlayerPacketAdapter(@NotNull BukkitProtocolLibLockedPacketListener listener) {
            super(listener.getPlugin(), ListenerPriority.HIGHEST, getPacketsToListenFor());
            this.listener = listener;
        }

        @Override
        public void onPacketReceiving(@NotNull PacketEvent event) {
            if (listener.cancelPlayerEvent(event.getPlayer().getUniqueId()) && !event.isReadOnly()) {
                event.setCancelled(true);
            }
        }

        @Override
        public void onPacketSending(@NotNull PacketEvent event) {
            if (listener.cancelPlayerEvent(event.getPlayer().getUniqueId()) && !event.isReadOnly()) {
                event.setCancelled(true);
            }
        }

        // Returns the set of ALL Server packets, excluding the set of allowed packets
        @NotNull
        private static Set<PacketType> getPacketsToListenFor() {
            return Sets.difference(
                    Client.getInstance().values().stream().filter(PacketType::isSupported).collect(Collectors.toSet()),
                    ALLOWED_PACKETS
            );
        }

    }

}
