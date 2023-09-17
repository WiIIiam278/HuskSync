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

package net.william278.husksync.redis;

import net.william278.husksync.HuskSync;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.player.User;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisException;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Manages the connection to the Redis server, handling the caching of user data
 */
public class RedisManager extends JedisPubSub {

    protected static final String KEY_NAMESPACE = "husksync:";

    private final HuskSync plugin;
    private final String clusterId;
    private JedisPool jedisPool;
    private final Map<UUID, CompletableFuture<Optional<DataSnapshot.Packed>>> pendingRequests;

    public RedisManager(@NotNull HuskSync plugin) {
        this.plugin = plugin;
        this.clusterId = plugin.getSettings().getClusterId();
        this.pendingRequests = new ConcurrentHashMap<>();
    }

    /**
     * Initialize the redis connection pool
     */
    public void initialize() throws IllegalStateException {
        final String password = plugin.getSettings().getRedisPassword();
        final String host = plugin.getSettings().getRedisHost();
        final int port = plugin.getSettings().getRedisPort();
        final boolean useSSL = plugin.getSettings().redisUseSsl();

        // Create the jedis pool
        final JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxIdle(0);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        this.jedisPool = password.isEmpty()
                ? new JedisPool(config, host, port, 0, useSSL)
                : new JedisPool(config, host, port, 0, password, useSSL);

        // Ping the server to check the connection
        try {
            jedisPool.getResource().ping();
        } catch (JedisException e) {
            throw new IllegalStateException("Failed to establish connection with the Redis server. "
                    + "Please check the supplied credentials in the config file", e);
        }

        // Subscribe using a thread (rather than a task)
        new Thread(this::subscribe, "husksync:redis_subscriber").start();
    }

    private void subscribe() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.subscribe(
                    this,
                    Arrays.stream(RedisMessageType.values())
                            .map(type -> type.getMessageChannel(clusterId))
                            .toArray(String[]::new)
            );
        }
    }

    @Override
    public void onMessage(@NotNull String channel, @NotNull String message) {
        final RedisMessageType messageType = RedisMessageType.getTypeFromChannel(channel, clusterId).orElse(null);
        if (messageType == null) {
            return;
        }

        final RedisMessage redisMessage = RedisMessage.fromJson(plugin, message);
        switch (messageType) {
            case UPDATE_USER_DATA -> plugin.getOnlineUser(redisMessage.targetUserUuid).ifPresent(
                    user -> user.applySnapshot(
                            DataSnapshot.deserialize(plugin, redisMessage.data),
                            DataSnapshot.UpdateCause.UPDATED
                    )
            );
            case REQUEST_USER_DATA -> plugin.getOnlineUser(redisMessage.targetUserUuid).ifPresent(
                    user -> new RedisMessage(
                            UUID.fromString(new String(redisMessage.data, StandardCharsets.UTF_8)),
                            user.createSnapshot(DataSnapshot.SaveCause.INVENTORY_COMMAND).asBytes(plugin)
                    ).dispatch(plugin, RedisMessageType.RETURN_USER_DATA)
            );
            case RETURN_USER_DATA -> {
                final CompletableFuture<Optional<DataSnapshot.Packed>> future = pendingRequests.get(
                        redisMessage.targetUserUuid
                );
                if (future != null) {
                    future.complete(Optional.of(DataSnapshot.deserialize(plugin, redisMessage.data)));
                }
            }
        }
    }

    protected void sendMessage(@NotNull String channel, @NotNull String message) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.publish(channel, message);
        }
    }

    public void sendUserDataUpdate(@NotNull User user, @NotNull DataSnapshot.Packed data) {
        plugin.runAsync(() -> {
            final RedisMessage redisMessage = new RedisMessage(user.getUuid(), data.asBytes(plugin));
            redisMessage.dispatch(plugin, RedisMessageType.UPDATE_USER_DATA);
        });
    }

    public CompletableFuture<Optional<DataSnapshot.Packed>> getUserData(@NotNull UUID requester, @NotNull User user) {
        return plugin.getOnlineUser(user.getUuid())
                .map(online -> CompletableFuture.completedFuture(
                        Optional.of(online.createSnapshot(DataSnapshot.SaveCause.API)))
                )
                .orElse(requestData(requester, user));
    }

    private CompletableFuture<Optional<DataSnapshot.Packed>> requestData(@NotNull UUID requester, @NotNull User user) {
        final CompletableFuture<Optional<DataSnapshot.Packed>> future = new CompletableFuture<>();
        pendingRequests.put(requester, future);
        plugin.runAsync(() -> {
            final RedisMessage redisMessage = new RedisMessage(
                    user.getUuid(),
                    requester.toString().getBytes(StandardCharsets.UTF_8)
            );
            redisMessage.dispatch(plugin, RedisMessageType.REQUEST_USER_DATA);
        });
        future.orTimeout(
                plugin.getSettings().getNetworkLatencyMilliseconds() * 2L, TimeUnit.MILLISECONDS
        ).exceptionally(throwable -> Optional.empty());
        return future;
    }

    /**
     * Set a user's data to the Redis server
     *
     * @param user the user to set data for
     * @param data the user's data to set
     */
    public void setUserData(@NotNull User user, @NotNull DataSnapshot.Packed data) {
        plugin.runAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.setex(
                        getKey(RedisKeyType.DATA_UPDATE, user.getUuid(), clusterId),
                        RedisKeyType.DATA_UPDATE.getTimeToLive(),
                        data.asBytes(plugin)
                );
                plugin.debug(String.format("[%s] Set %s key to redis at: %s", user.getUsername(),
                        RedisKeyType.DATA_UPDATE.name(), new SimpleDateFormat("mm:ss.SSS").format(new Date())));
            } catch (Throwable e) {
                plugin.log(Level.SEVERE, "An exception occurred setting a user's server switch", e);
            }
        });
    }

    /**
     * Set a user's server switch to the Redis server
     *
     * @param user the user to set the server switch for
     * @return a future returning void when complete
     */
    public CompletableFuture<Void> setUserServerSwitch(@NotNull User user) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        plugin.runAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.setex(
                        getKey(RedisKeyType.SERVER_SWITCH, user.getUuid(), clusterId),
                        RedisKeyType.SERVER_SWITCH.getTimeToLive(), new byte[0]
                );
                future.complete(null);
                plugin.debug(String.format("[%s] Set %s key to redis at: %s", user.getUsername(),
                        RedisKeyType.SERVER_SWITCH.name(), new SimpleDateFormat("mm:ss.SSS").format(new Date())));
            } catch (Throwable e) {
                plugin.log(Level.SEVERE, "An exception occurred setting a user's server switch", e);
            }
        });
        return future;
    }

    /**
     * Fetch a user's data from the Redis server and consume the key if found
     *
     * @param user The user to fetch data for
     * @return The user's data, if it's present on the database. Otherwise, an empty optional.
     */
    public Optional<DataSnapshot.Packed> getUserData(@NotNull User user) {
        try (Jedis jedis = jedisPool.getResource()) {
            final byte[] key = getKey(RedisKeyType.DATA_UPDATE, user.getUuid(), clusterId);
            final byte[] dataByteArray = jedis.get(key);
            if (dataByteArray == null) {
                plugin.debug("[" + user.getUsername() + "] Could not read " +
                        RedisKeyType.DATA_UPDATE.name() + " key from redis at: " +
                        new SimpleDateFormat("mm:ss.SSS").format(new Date()));
                return Optional.empty();
            }
            plugin.debug("[" + user.getUsername() + "] Successfully read "
                    + RedisKeyType.DATA_UPDATE.name() + " key from redis at: " +
                    new SimpleDateFormat("mm:ss.SSS").format(new Date()));

            // Consume the key (delete from redis)
            jedis.del(key);

            // Use Snappy to decompress the json
            return Optional.of(DataSnapshot.deserialize(plugin, dataByteArray));
        } catch (Throwable e) {
            plugin.log(Level.SEVERE, "An exception occurred fetching a user's data from redis", e);
            return Optional.empty();
        }
    }

    public boolean getUserServerSwitch(@NotNull User user) {
        try (Jedis jedis = jedisPool.getResource()) {
            final byte[] key = getKey(RedisKeyType.SERVER_SWITCH, user.getUuid(), clusterId);
            final byte[] readData = jedis.get(key);
            if (readData == null) {
                plugin.debug("[" + user.getUsername() + "] Could not read " +
                        RedisKeyType.SERVER_SWITCH.name() + " key from redis at: " +
                        new SimpleDateFormat("mm:ss.SSS").format(new Date()));
                return false;
            }
            plugin.debug("[" + user.getUsername() + "] Successfully read "
                    + RedisKeyType.SERVER_SWITCH.name() + " key from redis at: " +
                    new SimpleDateFormat("mm:ss.SSS").format(new Date()));

            // Consume the key (delete from redis)
            jedis.del(key);
            return true;
        } catch (Throwable e) {
            plugin.log(Level.SEVERE, "An exception occurred fetching a user's server switch from redis", e);
            return false;
        }
    }

    public void terminate() {
        if (jedisPool != null) {
            if (!jedisPool.isClosed()) {
                jedisPool.close();
            }
        }
        this.unsubscribe();
    }

    private static byte[] getKey(@NotNull RedisKeyType keyType, @NotNull UUID uuid, @NotNull String clusterId) {
        return String.format("%s:%s", keyType.getKeyPrefix(clusterId), uuid).getBytes(StandardCharsets.UTF_8);
    }

}
