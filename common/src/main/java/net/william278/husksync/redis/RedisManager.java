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

package net.william278.husksync.redis;

import net.william278.husksync.HuskSync;
import net.william278.husksync.data.UserData;
import net.william278.husksync.player.User;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisException;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Manages the connection to the Redis server, handling the caching of user data
 */
public class RedisManager extends JedisPubSub {

    protected static final String KEY_NAMESPACE = "husksync:";
    protected static String clusterId = "";
    private final HuskSync plugin;
    private final JedisPoolConfig jedisPoolConfig;
    private final String redisHost;
    private final int redisPort;
    private final String redisPassword;
    private final boolean redisUseSsl;
    private JedisPool jedisPool;

    public RedisManager(@NotNull HuskSync plugin) {
        this.plugin = plugin;
        clusterId = plugin.getSettings().getClusterId();

        // Set redis credentials
        this.redisHost = plugin.getSettings().getRedisHost();
        this.redisPort = plugin.getSettings().getRedisPort();
        this.redisPassword = plugin.getSettings().getRedisPassword();
        this.redisUseSsl = plugin.getSettings().isRedisUseSsl();

        // Configure the jedis pool
        this.jedisPoolConfig = new JedisPoolConfig();
        this.jedisPoolConfig.setMaxIdle(0);
        this.jedisPoolConfig.setTestOnBorrow(true);
        this.jedisPoolConfig.setTestOnReturn(true);
    }

    /**
     * Initialize the redis connection pool
     *
     * @return a future returning void when complete
     */
    public boolean initialize() {
        if (redisPassword.isBlank()) {
            jedisPool = new JedisPool(jedisPoolConfig, redisHost, redisPort, 0, redisUseSsl);
        } else {
            jedisPool = new JedisPool(jedisPoolConfig, redisHost, redisPort, 0, redisPassword, redisUseSsl);
        }
        try {
            jedisPool.getResource().ping();
        } catch (JedisException e) {
            return false;
        }
        CompletableFuture.runAsync(this::subscribe);
        return true;
    }

    private void subscribe() {
        try (final Jedis subscriber = redisPassword.isBlank() ? new Jedis(redisHost, redisPort, 0, redisUseSsl) :
                new Jedis(redisHost, redisPort, DefaultJedisClientConfig.builder()
                        .password(redisPassword).timeoutMillis(0).ssl(redisUseSsl).build())) {
            subscriber.connect();
            subscriber.subscribe(this, Arrays.stream(RedisMessageType.values())
                    .map(RedisMessageType::getMessageChannel)
                    .toArray(String[]::new));
        }
    }

    @Override
    public void onMessage(@NotNull String channel, @NotNull String message) {
        final RedisMessageType messageType = RedisMessageType.getTypeFromChannel(channel).orElse(null);
        if (messageType != RedisMessageType.UPDATE_USER_DATA) {
            return;
        }

        final RedisMessage redisMessage = RedisMessage.fromJson(message);
        plugin.getOnlineUser(redisMessage.targetUserUuid).ifPresent(user -> {
            user.setData(plugin.getDataAdapter().fromBytes(redisMessage.data), plugin);
        });
    }

    protected void sendMessage(@NotNull String channel, @NotNull String message) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.publish(channel, message);
        }
    }

    public CompletableFuture<Void> sendUserDataUpdate(@NotNull User user, @NotNull UserData userData) {
        return CompletableFuture.runAsync(() -> {
            final RedisMessage redisMessage = new RedisMessage(user.uuid, plugin.getDataAdapter().toBytes(userData));
            redisMessage.dispatch(this, RedisMessageType.UPDATE_USER_DATA);
        });
    }

    /**
     * Set a user's data to the Redis server
     *
     * @param user     the user to set data for
     * @param userData the user's data to set
     * @return a future returning void when complete
     */
    public CompletableFuture<Void> setUserData(@NotNull User user, @NotNull UserData userData) {
        try {
            return CompletableFuture.runAsync(() -> {
                try (Jedis jedis = jedisPool.getResource()) {
                    // Set the user's data as a compressed byte array of the json using Snappy
                    jedis.setex(getKey(RedisKeyType.DATA_UPDATE, user.uuid),
                            RedisKeyType.DATA_UPDATE.timeToLive,
                            plugin.getDataAdapter().toBytes(userData));

                    // Debug logging
                    plugin.debug("[" + user.username + "] Set " + RedisKeyType.DATA_UPDATE.name()
                                 + " key to redis at: " +
                                 new SimpleDateFormat("mm:ss.SSS").format(new Date()));
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public CompletableFuture<Void> setUserServerSwitch(@NotNull User user) {
        return CompletableFuture.runAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.setex(getKey(RedisKeyType.SERVER_SWITCH, user.uuid),
                        RedisKeyType.SERVER_SWITCH.timeToLive, new byte[0]);
                plugin.debug("[" + user.username + "] Set " + RedisKeyType.SERVER_SWITCH.name()
                             + " key to redis at: " +
                             new SimpleDateFormat("mm:ss.SSS").format(new Date()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Fetch a user's data from the Redis server and consume the key if found
     *
     * @param user The user to fetch data for
     * @return The user's data, if it's present on the database. Otherwise, an empty optional.
     */
    public Optional<UserData> getUserData(@NotNull User user) {
        try (Jedis jedis = jedisPool.getResource()) {
            final byte[] key = getKey(RedisKeyType.DATA_UPDATE, user.uuid);
            final byte[] dataByteArray = jedis.get(key);
            if (dataByteArray == null) {
                plugin.debug("[" + user.username + "] Could not read " +
                             RedisKeyType.DATA_UPDATE.name() + " key from redis at: " +
                             new SimpleDateFormat("mm:ss.SSS").format(new Date()));
                return Optional.empty();
            }
            plugin.debug("[" + user.username + "] Successfully read "
                         + RedisKeyType.DATA_UPDATE.name() + " key from redis at: " +
                         new SimpleDateFormat("mm:ss.SSS").format(new Date()));

            // Consume the key (delete from redis)
            jedis.del(key);

            // Use Snappy to decompress the json
            return Optional.of(plugin.getDataAdapter().fromBytes(dataByteArray));
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public boolean getUserServerSwitch(@NotNull User user) {
        try (Jedis jedis = jedisPool.getResource()) {
            final byte[] key = getKey(RedisKeyType.SERVER_SWITCH, user.uuid);
            final byte[] readData = jedis.get(key);
            if (readData == null) {
                plugin.debug("[" + user.username + "] Could not read " +
                             RedisKeyType.SERVER_SWITCH.name() + " key from redis at: " +
                             new SimpleDateFormat("mm:ss.SSS").format(new Date()));
                return false;
            }
            plugin.debug("[" + user.username + "] Successfully read "
                         + RedisKeyType.SERVER_SWITCH.name() + " key from redis at: " +
                         new SimpleDateFormat("mm:ss.SSS").format(new Date()));

            // Consume the key (delete from redis)
            jedis.del(key);
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    public void terminate() {
        if (jedisPool != null) {
            if (!jedisPool.isClosed()) {
                jedisPool.close();
            }
        }
    }

    private static byte[] getKey(@NotNull RedisKeyType keyType, @NotNull UUID uuid) {
        return (keyType.getKeyPrefix() + ":" + uuid).getBytes(StandardCharsets.UTF_8);
    }

}
