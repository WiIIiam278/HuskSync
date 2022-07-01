package net.william278.husksync.redis;

import net.william278.husksync.config.Settings;
import net.william278.husksync.data.UserData;
import net.william278.husksync.player.User;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RedisManager {

    private static final String KEY_NAMESPACE = "husksync:";
    private static String clusterId = "";
    private final JedisPool jedisPool;

    private RedisManager(@NotNull Settings settings) {
        clusterId = settings.getStringValue(Settings.ConfigOption.CLUSTER_ID);
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(0);
        jedisPoolConfig.setTestOnBorrow(true);
        jedisPoolConfig.setTestOnReturn(true);
        if (settings.getStringValue(Settings.ConfigOption.REDIS_PASSWORD).isBlank()) {
            jedisPool = new JedisPool(jedisPoolConfig,
                    settings.getStringValue(Settings.ConfigOption.REDIS_HOST),
                    settings.getIntegerValue(Settings.ConfigOption.REDIS_PORT),
                    0,
                    settings.getBooleanValue(Settings.ConfigOption.REDIS_USE_SSL));
        } else {
            jedisPool = new JedisPool(jedisPoolConfig,
                    settings.getStringValue(Settings.ConfigOption.REDIS_HOST),
                    settings.getIntegerValue(Settings.ConfigOption.REDIS_PORT),
                    0,
                    settings.getStringValue(Settings.ConfigOption.REDIS_PASSWORD),
                    settings.getBooleanValue(Settings.ConfigOption.REDIS_USE_SSL));
        }
    }

    public CompletableFuture<Void> setPlayerData(@NotNull User user, @NotNull UserData userData,
                                                 @NotNull RedisKeyType redisKeyType) {
        return CompletableFuture.runAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.setex(redisKeyType.getKeyPrefix() + user.uuid.toString(),
                        redisKeyType.timeToLive, userData.toJson());
            }
        });
    }

    public CompletableFuture<Optional<UserData>> getUserData(@NotNull User user, @NotNull RedisKeyType redisKeyType) {
        return CompletableFuture.supplyAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                final String json = jedis.get(redisKeyType.getKeyPrefix() + user.uuid.toString());
                if (json == null) {
                    return Optional.empty();
                }
                return Optional.of(UserData.fromJson(json));
            }
        });
    }

    public static CompletableFuture<RedisManager> initialize(@NotNull Settings settings) {
        return CompletableFuture.supplyAsync(() -> new RedisManager(settings));
    }

    public enum RedisKeyType {
        CACHE(60 * 60 * 24),
        SERVER_CHANGE(2);

        public final int timeToLive;

        RedisKeyType(int timeToLive) {
            this.timeToLive = timeToLive;
        }

        @NotNull
        public String getKeyPrefix() {
            return KEY_NAMESPACE.toLowerCase() + ":" + clusterId.toLowerCase() + ":" + name().toLowerCase() + ":";
        }
    }

}
