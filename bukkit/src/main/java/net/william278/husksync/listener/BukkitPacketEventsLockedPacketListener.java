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

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.google.common.collect.Sets;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import net.william278.husksync.BukkitHuskSync;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.logging.Level;

public class BukkitPacketEventsLockedPacketListener extends BukkitLockedEventListener implements LockedHandler {

    protected BukkitPacketEventsLockedPacketListener(@NotNull BukkitHuskSync plugin) {
        super(plugin);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(getPlugin()));
        PacketEvents.getAPI().getSettings().reEncodeByDefault(false)
                .checkForUpdates(false)
                .bStats(true);
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        PacketEvents.getAPI().getEventManager().registerListener(new PlayerPacketAdapter(this));
        PacketEvents.getAPI().init();
        plugin.log(Level.INFO, "Using PacketEvents to cancel packets for locked players");
    }

    private static class PlayerPacketAdapter extends PacketListenerAbstract {

        private static final Set<PacketType.Play.Client> ALLOWED_PACKETS = Set.of(
                PacketType.Play.Client.KEEP_ALIVE, PacketType.Play.Client.PONG, PacketType.Play.Client.PLUGIN_MESSAGE, // Connection packets
                PacketType.Play.Client.CHAT_MESSAGE, PacketType.Play.Client.CHAT_COMMAND, PacketType.Play.Client.CHAT_SESSION_UPDATE, // Chat / command packets
                PacketType.Play.Client.PLAYER_POSITION, PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION, PacketType.Play.Client.PLAYER_ROTATION, // Movement packets
                PacketType.Play.Client.HELD_ITEM_CHANGE, PacketType.Play.Client.ANIMATION, PacketType.Play.Client.TELEPORT_CONFIRM, // Animation packets
                PacketType.Play.Client.CLIENT_SETTINGS // Video setting packets
        );

        private static final Set<PacketType.Play.Client> CANCEL_PACKETS = getPacketsToListenFor();


        private final BukkitPacketEventsLockedPacketListener listener;

        public PlayerPacketAdapter(@NotNull BukkitPacketEventsLockedPacketListener listener) {
            super(PacketListenerPriority.HIGH);
            this.listener = listener;
        }

        @Override
        public void onPacketReceive(PacketReceiveEvent event) {
            if (!(event.getPacketType() instanceof PacketType.Play.Client client)) {
                return;
            }
            if (!CANCEL_PACKETS.contains(client)) {
                return;
            }
            if (listener.cancelPlayerEvent(event.getUser().getUUID())) {
                event.setCancelled(true);
            }
        }

        @Override
        public void onPacketSend(PacketSendEvent event) {
            if (!(event.getPacketType() instanceof PacketType.Play.Client client)) {
                return;
            }
            if (!CANCEL_PACKETS.contains(client)) {
                return;
            }
            if (listener.cancelPlayerEvent(event.getUser().getUUID())) {
                event.setCancelled(true);
            }
        }

        // Returns the set of ALL Server packets, excluding the set of allowed packets
        @NotNull
        private static Set<PacketType.Play.Client> getPacketsToListenFor() {
            return Sets.difference(
                    Sets.newHashSet(PacketType.Play.Client.values()),
                    ALLOWED_PACKETS
            );
        }

    }

}
