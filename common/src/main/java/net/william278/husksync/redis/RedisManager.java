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
import net.william278.husksync.user.User;
import org.jetbrains.annotations.Blocking;
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
    @Blocking
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

    @Blocking
    private void subscribe() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.subscribe(
                    this,
                    Arrays.stream(RedisMessage.Type.values())
                            .map(type -> type.getMessageChannel(clusterId))
                            .toArray(String[]::new)
            );
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
                    user -> user.applySnapshot(
                            DataSnapshot.deserialize(plugin, redisMessage.getPayload()),
                            DataSnapshot.UpdateCause.UPDATED
                    )
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
                    future.complete(Optional.of(DataSnapshot.deserialize(plugin, redisMessage.getPayload())));
                    pendingRequests.remove(redisMessage.getTargetUuid());
                }
            }
        }
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
        return future.orTimeout(
                        plugin.getSettings().getNetworkLatencyMilliseconds(),
                        TimeUnit.MILLISECONDS
                )
                .exceptionally(throwable -> {
                    pendingRequests.remove(requestId);
                    return Optional.empty();
                });
    }

    /**
     * Set a user's data to the Redis server
     *
     * @param user the user to set data for
     * @param data the user's data to set
     */
    @Blocking
    public void setUserData(@NotNull User user, @NotNull DataSnapshot.Packed data) {
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
    }

    @Blocking
    public void setUserCheckedOut(@NotNull User user, boolean checkedOut) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (checkedOut) {
                jedis.set(
                        getKey(RedisKeyType.DATA_CHECKOUT, user.getUuid(), clusterId),
                        plugin.getServerName().getBytes(StandardCharsets.UTF_8)
                );
            } else {
                jedis.del(getKey(RedisKeyType.DATA_CHECKOUT, user.getUuid(), clusterId));
            }
            plugin.debug(String.format("[%s] %s %s key to redis at: %s",
                    checkedOut ? "set" : "removed", user.getUsername(), RedisKeyType.DATA_CHECKOUT.name(),
                    new SimpleDateFormat("mm:ss.SSS").format(new Date())));
        } catch (Throwable e) {
            plugin.log(Level.SEVERE, "An exception occurred setting a user's server switch", e);
        }
    }

    @Blocking
    public Optional<String> getUserCheckedOut(@NotNull User user) {
        try (Jedis jedis = jedisPool.getResource()) {
            final byte[] key = getKey(RedisKeyType.DATA_CHECKOUT, user.getUuid(), clusterId);
            final byte[] readData = jedis.get(key);
            if (readData != null) {
                plugin.debug("[" + user.getUsername() + "] Successfully read "
                        + RedisKeyType.DATA_CHECKOUT.name() + " key from redis at: " +
                        new SimpleDateFormat("mm:ss.SSS").format(new Date()));
                return Optional.of(new String(readData, StandardCharsets.UTF_8));
            }
        } catch (Throwable e) {
            plugin.log(Level.SEVERE, "An exception occurred fetching a user's checkout key from redis", e);
        }
        plugin.debug("[" + user.getUsername() + "] Could not read " +
                RedisKeyType.DATA_CHECKOUT.name() + " key from redis at: " +
                new SimpleDateFormat("mm:ss.SSS").format(new Date()));
        return Optional.empty();
    }

    @Blocking
    public void clearUsersCheckedOutOnServer() {
        final String keyFormat = String.format("%s*", RedisKeyType.DATA_CHECKOUT.getKeyPrefix(clusterId));
        try (Jedis jedis = jedisPool.getResource()) {
            final Set<String> keys = jedis.keys(keyFormat);
            if (keys == null) {
                plugin.log(Level.WARNING, "Checkout key set returned null from jedis during clearing");
                return;
            }
            for (String key : keys) {
                if (jedis.get(key).equals(plugin.getServerName())) {
                    jedis.del(key);
                }
            }
        } catch (Throwable e) {
            plugin.log(Level.SEVERE, "An exception occurred clearing users checked out on this server", e);
        }
    }

    /**
     * Set a user's server switch to the Redis server
     *
     * @param user the user to set the server switch for
     */
    @Blocking
    public void setUserServerSwitch(@NotNull User user) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(
                    getKey(RedisKeyType.SERVER_SWITCH, user.getUuid(), clusterId),
                    RedisKeyType.SERVER_SWITCH.getTimeToLive(), new byte[0]
            );
            plugin.debug(String.format("[%s] Set %s key to redis at: %s", user.getUsername(),
                    RedisKeyType.SERVER_SWITCH.name(), new SimpleDateFormat("mm:ss.SSS").format(new Date())));
        } catch (Throwable e) {
            plugin.log(Level.SEVERE, "An exception occurred setting a user's server switch", e);
        }
    }

    /**
     * Fetch a user's data from the Redis server and consume the key if found
     *
     * @param user The user to fetch data for
     * @return The user's data, if it's present on the database. Otherwise, an empty optional.
     */
    @Blocking
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

    @Blocking
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

    @Blocking
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
