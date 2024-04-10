package net.william278.husksync.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.Sets;
import lombok.Getter;
import net.william278.husksync.BukkitHuskSync;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.logging.Level;

import static com.comphenix.protocol.PacketType.Play.Server;

@Getter
public class BukkitLockedPacketListener implements LockedHandler {

    private final BukkitHuskSync plugin;

    protected BukkitLockedPacketListener(@NotNull BukkitHuskSync plugin) {
        this.plugin = plugin;
        ProtocolLibrary.getProtocolManager().addPacketListener(new PlayerPacketAdapter(this));
        plugin.log(Level.INFO, "Using ProtocolLib to cancel packets for locked players");
    }

    @Getter
    private static class PlayerPacketAdapter extends PacketAdapter {

        // Packets we want the player to still be able to SEND to the server
        private static final Set<PacketType> ALLOWED_PACKETS = Set.of(
                Server.KEEP_ALIVE, Server.LOGIN, Server.KICK_DISCONNECT, // Connection packets are OK
                Server.PLAYER_LIST_HEADER_FOOTER, Server.PLAYER_INFO, Server.PLAYER_INFO_REMOVE, // TAB stuff is OK
                Server.COMMANDS // Handled elsewhere
        );

        private final BukkitLockedPacketListener listener;

        public PlayerPacketAdapter(@NotNull BukkitLockedPacketListener listener) {
            super(listener.getPlugin(), ListenerPriority.HIGHEST, getPacketsToListenFor());
            this.listener = listener;
        }

        @Override
        public void onPacketSending(@NotNull PacketEvent event) {
            if (listener.cancelPlayerEvent(event.getPlayer().getUniqueId())) {
                event.setCancelled(true);
            }
        }

        // Returns the set of ALL Server packets, excluding the set of allowed packets
        @NotNull
        private static Set<PacketType> getPacketsToListenFor() {
            return Sets.difference(Server.getInstance().values(), ALLOWED_PACKETS);
        }

    }

}
