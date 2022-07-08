package net.william278.husksync.redis;

import org.jetbrains.annotations.NotNull;

public enum RedisKeyType {
    CACHE(60 * 60 * 24),
    DATA_UPDATE(10),
    SERVER_SWITCH(10);

    public final int timeToLive;

    RedisKeyType(int timeToLive) {
        this.timeToLive = timeToLive;
    }

    @NotNull
    public String getKeyPrefix() {
        return RedisManager.KEY_NAMESPACE.toLowerCase() + ":" + RedisManager.clusterId.toLowerCase() + ":" + name().toLowerCase();
    }
}
