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
import net.william278.husksync.config.Settings;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.user.User;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.Pool;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Manages the connection to Redis, handling the caching of user data
 */
public class RedisManager extends JedisPubSub {

    protected static final String KEY_NAMESPACE = "husksync:";
    private static final int RECONNECTION_TIME = 8000;

    private final HuskSync plugin;
    private final String clusterId;
    private Pool<Jedis> jedisPool;
    private final Map<UUID, CompletableFuture<Optional<DataSnapshot.Packed>>> pendingRequests;

    private boolean enabled;
    private boolean reconnected;

    public RedisManager(@NotNull HuskSync plugin) {
        this.plugin = plugin;
        this.clusterId = plugin.getSettings().getClusterId();
        this.pendingRequests = new ConcurrentHashMap<>();
    }

    /**
     * Initialize Redis connection pool
     */
    @Blocking
    public void initialize() throws IllegalStateException {
        final Settings.RedisSettings.RedisCredentials credentials = plugin.getSettings().getRedis().getCredentials();
        final String password = credentials.getPassword();
        final String host = credentials.getHost();
        final int port = credentials.getPort();
        final boolean useSSL = credentials.isUseSsl();

        // Create the jedis pool
        final JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxIdle(0);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);

        final Settings.RedisSettings.RedisSentinel sentinel = plugin.getSettings().getRedis().getSentinel();
        Set<String> redisSentinelNodes = new HashSet<>(sentinel.getNodes());
        if (redisSentinelNodes.isEmpty()) {
            this.jedisPool = password.isEmpty()
                    ? new JedisPool(config, host, port, 0, useSSL)
                    : new JedisPool(config, host, port, 0, password, useSSL);
        } else {
            final String sentinelPassword = sentinel.getPassword();
            this.jedisPool = new JedisSentinelPool(sentinel.getMaster(), redisSentinelNodes, password.isEmpty()
                    ? null : password, sentinelPassword.isEmpty() ? null : sentinelPassword);
        }

        // Ping the server to check the connection
        try {
            jedisPool.getResource().ping();
        } catch (JedisException e) {
            throw new IllegalStateException("Failed to establish connection with Redis. "
                                            + "Please check the supplied credentials in the config file", e);
        }

        // Subscribe using a thread (rather than a task)
        enabled = true;
        new Thread(this::subscribe, "husksync:redis_subscriber").start();
    }

    @Blocking
    private void subscribe() {
        while (enabled && !Thread.interrupted() && jedisPool != null && !jedisPool.isClosed()) {
            try (Jedis jedis = jedisPool.getResource()) {
                if (reconnected) {
                    plugin.log(Level.INFO, "Redis connection is alive again");
                }
                // Subscribe channels and lock the thread
                jedis.subscribe(
                        this,
                        Arrays.stream(RedisMessage.Type.values())
                                .map(type -> type.getMessageChannel(clusterId))
                                .toArray(String[]::new)
                );
            } catch (Throwable t) {
                // Thread was unlocked due error
                onThreadUnlock(t);
            }
        }
    }

    private void onThreadUnlock(@NotNull Throwable t) {
        if (!enabled) {
            return;
        }

        if (reconnected) {
            plugin.log(Level.WARNING, "Redis Server connection lost. Attempting reconnect in %ss..."
                    .formatted(RECONNECTION_TIME / 1000), t);
        }
        try {
            this.unsubscribe();
        } catch (Throwable ignored) {
            // empty catch
        }

        // Make an instant subscribe if occurs any error on initialization
        if (!reconnected) {
            reconnected = true;
        } else {
            try {
                Thread.sleep(RECONNECTION_TIME);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void onMessage(@NotNull String channel, @NotNull String message) {
        final RedisMessage.Type messageType = RedisMessage.Type.getTypeFromChannel(channel, clusterId).orElse(null);
        if (messageType == null) {
            return;
        }

        final RedisMessage redisMessage = RedisMessage.fromJson(plugin, message);
        switch (messageType) {
            case UPDATE_USER_DATA -> plugin.getOnlineUser(redisMessage.getTargetUuid()).ifPresent(
                    user -> {
                        plugin.lockPlayer(user.getUuid());
                        try {
                            final DataSnapshot.Packed data = DataSnapshot.deserialize(plugin, redisMessage.getPayload());
                            user.applySnapshot(data, DataSnapshot.UpdateCause.UPDATED);
                        } catch (Throwable e) {
                            plugin.log(Level.SEVERE, "An exception occurred updating user data from Redis", e);
                            user.completeSync(false, DataSnapshot.UpdateCause.UPDATED, plugin);
                        }
                    }
            );
            case REQUEST_USER_DATA -> plugin.getOnlineUser(redisMessage.getTargetUuid()).ifPresent(
                    user -> RedisMessage.create(
                            UUID.fromString(new String(redisMessage.getPayload(), StandardCharsets.UTF_8)),
                            user.createSnapshot(DataSnapshot.SaveCause.INVENTORY_COMMAND).asBytes(plugin)
                    ).dispatch(plugin, RedisMessage.Type.RETURN_USER_DATA)
            );
            case RETURN_USER_DATA -> {
                final CompletableFuture<Optional<DataSnapshot.Packed>> future = pendingRequests.get(
                        redisMessage.getTargetUuid()
                );
                if (future != null) {
                    try {
                        final DataSnapshot.Packed data = DataSnapshot.deserialize(plugin, redisMessage.getPayload());
                        future.complete(Optional.of(data));
                    } catch (Throwable e) {
                        plugin.log(Level.SEVERE, "An exception occurred returning user data from Redis", e);
                        future.complete(Optional.empty());
                    }
                    pendingRequests.remove(redisMessage.getTargetUuid());
                }
            }
        }
    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
        plugin.log(Level.INFO, "Redis subscribed to channel '" + channel + "'");
    }

    @Override
    public void onUnsubscribe(String channel, int subscribedChannels) {
        plugin.log(Level.INFO, "Redis unsubscribed from channel '" + channel + "'");
    }

    @Blocking
    protected void sendMessage(@NotNull String channel, @NotNull String message) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.publish(channel, message);
        }
    }

    public void sendUserDataUpdate(@NotNull User user, @NotNull DataSnapshot.Packed data) {
        plugin.runAsync(() -> {
            final RedisMessage redisMessage = RedisMessage.create(user.getUuid(), data.asBytes(plugin));
            redisMessage.dispatch(plugin, RedisMessage.Type.UPDATE_USER_DATA);
        });
    }

    public CompletableFuture<Optional<DataSnapshot.Packed>> getUserData(@NotNull UUID requestId, @NotNull User user) {
        return plugin.getOnlineUser(user.getUuid())
                .map(online -> CompletableFuture.completedFuture(
                        Optional.of(online.createSnapshot(DataSnapshot.SaveCause.API)))
                )
                .orElse(this.requestData(requestId, user));
    }

    private CompletableFuture<Optional<DataSnapshot.Packed>> requestData(@NotNull UUID requestId, @NotNull User user) {
        final CompletableFuture<Optional<DataSnapshot.Packed>> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);
        plugin.runAsync(() -> {
            final RedisMessage redisMessage = RedisMessage.create(
                    user.getUuid(),
                    requestId.toString().getBytes(StandardCharsets.UTF_8)
            );
            redisMessage.dispatch(plugin, RedisMessage.Type.REQUEST_USER_DATA);
        });
        return future
                .orTimeout(
                        plugin.getSettings().getSynchronization().getNetworkLatencyMilliseconds(),
                        TimeUnit.MILLISECONDS
                )
                .exceptionally(throwable -> {
                    pendingRequests.remove(requestId);
                    return Optional.empty();
                });
    }

    /**
     * Set a user's data to Redis
     *
     * @param user       the user to set data for
     * @param data       the user's data to set
     * @param timeToLive The time to cache the data for
     */
    @Blocking
    public void setUserData(@NotNull User user, @NotNull DataSnapshot.Packed data, int timeToLive) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(
                    getKey(RedisKeyType.LATEST_SNAPSHOT, user.getUuid(), clusterId),
                    timeToLive,
                    data.asBytes(plugin)
            );
            plugin.debug(String.format("[%s] Set %s key on Redis", user.getUsername(), RedisKeyType.LATEST_SNAPSHOT));
        } catch (Throwable e) {
            plugin.log(Level.SEVERE, "An exception occurred setting user data on Redis", e);
        }
    }

    @Blocking
    public void clearUserData(@NotNull User user) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(
                    getKey(RedisKeyType.LATEST_SNAPSHOT, user.getUuid(), clusterId)
            );
            plugin.debug(String.format("[%s] Cleared %s on Redis", user.getUsername(), RedisKeyType.LATEST_SNAPSHOT));
        } catch (Throwable e) {
            plugin.log(Level.SEVERE, "An exception occurred clearing user data on Redis", e);
        }
    }

    @Blocking
    public void setUserCheckedOut(@NotNull User user, boolean checkedOut) {
        try (Jedis jedis = jedisPool.getResource()) {
            final String key = getKeyString(RedisKeyType.DATA_CHECKOUT, user.getUuid(), clusterId);
            if (checkedOut) {
                jedis.set(
                        key.getBytes(StandardCharsets.UTF_8),
                        plugin.getServerName().getBytes(StandardCharsets.UTF_8)
                );
            } else {
                if (jedis.del(key.getBytes(StandardCharsets.UTF_8)) == 0) {
                    plugin.debug(String.format("[%s] %s key not set on Redis when attempting removal (%s)",
                            user.getUsername(), RedisKeyType.DATA_CHECKOUT, key));
                    return;
                }
            }
            plugin.debug(String.format("[%s] %s %s key %s Redis (%s)", user.getUsername(),
                    checkedOut ? "Set" : "Removed", RedisKeyType.DATA_CHECKOUT, checkedOut ? "to" : "from", key));
        } catch (Throwable e) {
            plugin.log(Level.SEVERE, "An exception occurred setting checkout to", e);
        }
    }

    @Blocking
    public Optional<String> getUserCheckedOut(@NotNull User user) {
        try (Jedis jedis = jedisPool.getResource()) {
            final byte[] key = getKey(RedisKeyType.DATA_CHECKOUT, user.getUuid(), clusterId);
            final byte[] readData = jedis.get(key);
            if (readData != null) {
                final String checkoutServer = new String(readData, StandardCharsets.UTF_8);
                plugin.debug(String.format("[%s] Waiting for %s %s key to be unset on Redis",
                        user.getUsername(), checkoutServer, RedisKeyType.DATA_CHECKOUT));
                return Optional.of(checkoutServer);
            }
        } catch (Throwable e) {
            plugin.log(Level.SEVERE, "An exception occurred getting a user's checkout key from Redis", e);
        }
        plugin.debug(String.format("[%s] %s key not set on Redis", user.getUsername(),
                RedisKeyType.DATA_CHECKOUT));
        return Optional.empty();
    }

    @Blocking
    public void clearUsersCheckedOutOnServer() {
        final String keyFormat = String.format("%s*", RedisKeyType.DATA_CHECKOUT.getKeyPrefix(clusterId));
        try (Jedis jedis = jedisPool.getResource()) {
            final Set<String> keys = jedis.keys(keyFormat);
            if (keys == null) {
                plugin.log(Level.WARNING, "Checkout key returned null from Redis during clearing");
                return;
            }
            for (String key : keys) {
                if (jedis.get(key).equals(plugin.getServerName())) {
                    jedis.del(key);
                }
            }
        } catch (Throwable e) {
            plugin.log(Level.SEVERE, "An exception occurred clearing this server's checkout keys on Redis", e);
        }
    }

    /**
     * Set a user's server switch to Redis
     *
     * @param user the user to set the server switch for
     */
    @Blocking
    public void setUserServerSwitch(@NotNull User user) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(
                    getKey(RedisKeyType.SERVER_SWITCH, user.getUuid(), clusterId),
                    RedisKeyType.TTL_10_SECONDS,
                    new byte[0]
            );
            plugin.debug(String.format("[%s] Set %s key to Redis",
                    user.getUsername(), RedisKeyType.SERVER_SWITCH));
        } catch (Throwable e) {
            plugin.log(Level.SEVERE, "An exception occurred setting a user's server switch key from Redis", e);
        }
    }

    /**
     * Fetch a user's data from Redis and consume the key if found
     *
     * @param user The user to fetch data for
     * @return The user's data, if it's present on the database. Otherwise, an empty optional.
     */
    @Blocking
    public Optional<DataSnapshot.Packed> getUserData(@NotNull User user) {
        try (Jedis jedis = jedisPool.getResource()) {
            final byte[] key = getKey(RedisKeyType.LATEST_SNAPSHOT, user.getUuid(), clusterId);
            final byte[] dataByteArray = jedis.get(key);
            if (dataByteArray == null) {
                plugin.debug(String.format("[%s] Waiting for %s key from Redis",
                        user.getUsername(), RedisKeyType.LATEST_SNAPSHOT));
                return Optional.empty();
            }
            plugin.debug(String.format("[%s] Read %s key from Redis",
                    user.getUsername(), RedisKeyType.LATEST_SNAPSHOT));

            // Consume the key (delete from redis)
            jedis.del(key);

            // Use Snappy to decompress the json
            return Optional.of(DataSnapshot.deserialize(plugin, dataByteArray));
        } catch (Throwable e) {
            plugin.log(Level.SEVERE, "An exception occurred getting a user's data from Redis", e);
            return Optional.empty();
        }
    }

    @Blocking
    public boolean getUserServerSwitch(@NotNull User user) {
        try (Jedis jedis = jedisPool.getResource()) {
            final byte[] key = getKey(RedisKeyType.SERVER_SWITCH, user.getUuid(), clusterId);
            final byte[] readData = jedis.get(key);
            if (readData == null) {
                plugin.debug(String.format("[%s] Waiting for %s key from Redis",
                        user.getUsername(), RedisKeyType.SERVER_SWITCH));
                return false;
            }
            plugin.debug(String.format("[%s] Read %s key from Redis",
                    user.getUsername(), RedisKeyType.SERVER_SWITCH));

            // Consume the key (delete from redis)
            jedis.del(key);
            return true;
        } catch (Throwable e) {
            plugin.log(Level.SEVERE, "An exception occurred getting a user's server switch from Redis", e);
            return false;
        }
    }

    @Blocking
    public void terminate() {
        enabled = false;
        if (jedisPool != null) {
            if (!jedisPool.isClosed()) {
                jedisPool.close();
            }
        }
        this.unsubscribe();
    }

    private static byte[] getKey(@NotNull RedisKeyType keyType, @NotNull UUID uuid, @NotNull String clusterId) {
        return getKeyString(keyType, uuid, clusterId).getBytes(StandardCharsets.UTF_8);
    }

    @NotNull
    private static String getKeyString(@NotNull RedisKeyType keyType, @NotNull UUID uuid, @NotNull String clusterId) {
        return String.format("%s:%s", keyType.getKeyPrefix(clusterId), uuid);
    }

}
