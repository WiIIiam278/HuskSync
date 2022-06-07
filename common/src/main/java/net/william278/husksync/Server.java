package net.william278.husksync;

import java.util.UUID;

/**
 * A record representing a server synchronised on the network and whether it has MySqlPlayerDataBridge installed
 */
public record Server(UUID serverUUID, boolean hasMySqlPlayerDataBridge, String huskSyncVersion, String serverBrand,
                     String clusterId) {
}
