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
import org.jetbrains.annotations.Nullable;
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
        final String user = credentials.getUser();
        final String password = credentials.getPassword();
        final String host = credentials.getHost();
        final int port = credentials.getPort();
        final int database = credentials.getDatabase();
        final boolean useSSL = credentials.isUseSsl();

        // Create the jedis pool
        final JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxIdle(0);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);

        final Settings.RedisSettings.RedisSentinel sentinel = plugin.getSettings().getRedis().getSentinel();
        Set<String> redisSentinelNodes = new HashSet<>(sentinel.getNodes());
        if (redisSentinelNodes.isEmpty()) {
            DefaultJedisClientConfig.Builder clientConfigBuilder = DefaultJedisClientConfig.builder()
                    .ssl(useSSL)
                    .database(database)
                    .timeoutMillis(0);

            if (!user.isEmpty()) {
                clientConfigBuilder.user(user);
            }

            if (!password.isEmpty()) {
                clientConfigBuilder.password(password);
            }

            this.jedisPool = new JedisPool(config, new HostAndPort(host, port), clientConfigBuilder.build());
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
            case UPDATE_USER_DATA -> redisMessage.getTargetUser(plugin).ifPresent(
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
            case REQUEST_USER_DATA -> redisMessage.getTargetUser(plugin).ifPresent(
                    user -> RedisMessage.create(
                            UUID.fromString(new String(redisMessage.getPayload(), StandardCharsets.UTF_8)),
                            user.createSnapshot(DataSnapshot.SaveCause.INVENTORY_COMMAND).asBytes(plugin)
                    ).dispatch(plugin, RedisMessage.Type.RETURN_USER_DATA)
            );
            case CHECK_IN_PETITION -> {
                if (!redisMessage.isTargetServer(plugin)
                        || !plugin.getSettings().getSynchronization().isCheckinPetitions()) {
                    return;
                }
                final String payload = new String(redisMessage.getPayload(), StandardCharsets.UTF_8);
                final User user = new User(UUID.fromString(payload.split("/")[0]), payload.split("/")[1]);
                boolean online = plugin.getDisconnectingPlayers().contains(user.getUuid())
                        || plugin.getOnlineUser(user.getUuid()).isEmpty();
                if (!online && !plugin.isLocked(user.getUuid())) {
                    plugin.debug("[%s] Received check-in petition for online/unlocked user, ignoring".formatted(user.getName()));
                    return;
                }
                plugin.getRedisManager().setUserCheckedOut(user, false);
                plugin.debug("[%s] Received petition for offline user, checking them in".formatted(user.getName()));
            }
            case RETURN_USER_DATA -> {
                final UUID target = redisMessage.getTargetUuid().orElse(null);
                final CompletableFuture<Optional<DataSnapshot.Packed>> future = pendingRequests.get(target);
                if (future != null) {
                    try {
                        final DataSnapshot.Packed data = DataSnapshot.deserialize(plugin, redisMessage.getPayload());
                        future.complete(Optional.of(data));
                    } catch (Throwable e) {
                        plugin.log(Level.SEVERE, "An exception occurred returning user data from Redis", e);
                        future.complete(Optional.empty());
                    }
                    pendingRequests.remove(target);
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

    @Blocking
    public void sendUserDataUpdate(@NotNull User user, @NotNull DataSnapshot.Packed data) {
        final RedisMessage redisMessage = RedisMessage.create(user.getUuid(), data.asBytes(plugin));
        redisMessage.dispatch(plugin, RedisMessage.Type.UPDATE_USER_DATA);
    }

    @Blocking
    public void petitionServerCheckin(@NotNull String server, @NotNull User user) {
        final RedisMessage redisMessage = RedisMessage.create(
                server, "%s/%s".formatted(user.getUuid(), user.getName()).getBytes(StandardCharsets.UTF_8));
        redisMessage.dispatch(plugin, RedisMessage.Type.CHECK_IN_PETITION);
    }

    public CompletableFuture<Optional<DataSnapshot.Packed>> getOnlineUserData(@NotNull UUID requestId, @NotNull User user,
                                                                              @NotNull DataSnapshot.SaveCause saveCause) {
        return plugin.getOnlineUser(user.getUuid())
                .map(online -> CompletableFuture.completedFuture(
                        Optional.of(online.createSnapshot(saveCause)))
                )
                .orElse(this.getNetworkedUserData(requestId, user));
    }

    // Request a user's dat x-server
    private CompletableFuture<Optional<DataSnapshot.Packed>> getNetworkedUserData(@NotNull UUID requestId, @NotNull User user) {
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

    // Set a user's data to Redis
    @Blocking
    public void setUserData(@NotNull User user, @NotNull DataSnapshot.Packed data) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(
                    getKey(RedisKeyType.LATEST_SNAPSHOT, user.getUuid(), clusterId),
                    RedisKeyType.TTL_1_YEAR,
                    data.asBytes(plugin)
            );
            plugin.debug(String.format("[%s] Set %s key on Redis", user.getName(), RedisKeyType.LATEST_SNAPSHOT));
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
            plugin.debug(String.format("[%s] Cleared %s on Redis", user.getName(), RedisKeyType.LATEST_SNAPSHOT));
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
                            user.getName(), RedisKeyType.DATA_CHECKOUT, key));
                    return;
                }
            }
            plugin.debug(String.format("[%s] %s %s key %s Redis (%s)", user.getName(),
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
                        user.getName(), checkoutServer, RedisKeyType.DATA_CHECKOUT));
                return Optional.of(checkoutServer);
            }
        } catch (Throwable e) {
            plugin.log(Level.SEVERE, "An exception occurred getting a user's checkout key from Redis", e);
        }
        plugin.debug(String.format("[%s] %s key not set on Redis", user.getName(),
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
                    user.getName(), RedisKeyType.SERVER_SWITCH));
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
                        user.getName(), RedisKeyType.LATEST_SNAPSHOT));
                return Optional.empty();
            }
            plugin.debug(String.format("[%s] Read %s key from Redis",
                    user.getName(), RedisKeyType.LATEST_SNAPSHOT));

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
                        user.getName(), RedisKeyType.SERVER_SWITCH));
                return false;
            }
            plugin.debug(String.format("[%s] Read %s key from Redis",
                    user.getName(), RedisKeyType.SERVER_SWITCH));

            // Consume the key (delete from redis)
            jedis.del(key);
            return true;
        } catch (Throwable e) {
            plugin.log(Level.SEVERE, "An exception occurred getting a user's server switch from Redis", e);
            return false;
        }
    }

    @Blocking
    public String getStatusDump() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.info();
        }
    }

    @Blocking
    public long getLatency() {
        final long startTime = System.currentTimeMillis();
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.ping();
            return System.currentTimeMillis() - startTime;
        }
    }

    @Blocking
    public String getVersion() {
        final String info = getStatusDump();
        for (String line : info.split("\n")) {
            if (line.startsWith("redis_version:")) {
                return line.split(":")[1];
            }
        }
        return "unknown";
    }

    @Blocking
    public void bindMapIds(@NotNull String fromServer, int fromId, @NotNull String toServer, int toId) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(
                    getMapIdKey(fromServer, fromId, toServer, clusterId),
                    RedisKeyType.TTL_1_YEAR,
                    String.valueOf(toId).getBytes(StandardCharsets.UTF_8)
            );
            jedis.setex(
                    getReversedMapIdKey(toServer, toId, clusterId),
                    RedisKeyType.TTL_1_YEAR,
                    String.format("%s:%s", fromServer, fromId).getBytes(StandardCharsets.UTF_8)
            );
            plugin.debug(String.format("Bound map %s:%s -> %s:%s on Redis", fromServer, fromId, toServer, toId));
        } catch (Throwable e) {
            plugin.log(Level.SEVERE, "An exception occurred binding map ids on Redis", e);
        }
    }

    @Blocking
    public Optional<Integer> getBoundMapId(@NotNull String fromServer, int fromId, @NotNull String toServer) {
        try (Jedis jedis = jedisPool.getResource()) {
            final byte[] readData = jedis.get(getMapIdKey(fromServer, fromId, toServer, clusterId));
            if (readData == null) {
                plugin.debug(String.format("[%s:%s] No bound map id for server %s Redis",
                        fromServer, fromId, toServer));
                return Optional.empty();
            }
            plugin.debug(String.format("[%s:%s] Read bound map id for server %s from Redis",
                    fromServer, fromId, toServer));

            return Optional.of(Integer.parseInt(new String(readData, StandardCharsets.UTF_8)));
        } catch (Throwable e) {
            plugin.log(Level.SEVERE, "An exception occurred getting bound map id from Redis", e);
            return Optional.empty();
        }
    }

    @Blocking
    public @Nullable Map.Entry<String, Integer> getReversedMapBound(@NotNull String toServer, int toId) {
        try (Jedis jedis = jedisPool.getResource()) {
            final byte[] readData = jedis.get(getReversedMapIdKey(toServer, toId, clusterId));
            if (readData == null) {
                plugin.debug(String.format("[%s:%s] No reversed map bound on Redis",
                        toServer, toId));
                return null;
            }
            plugin.debug(String.format("[%s:%s] Read reversed map bound from Redis",
                    toServer, toId));

            String[] parts = new String(readData, StandardCharsets.UTF_8).split(":");
            return Map.entry(parts[0], Integer.parseInt(parts[1]));
        } catch (Throwable e) {
            plugin.log(Level.SEVERE, "An exception occurred reading reversed map bound from Redis", e);
            return null;
        }
    }

    @Blocking
    public void setMapData(@NotNull String serverName, int mapId, byte[] data) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(
                    getMapDataKey(serverName, mapId, clusterId),
                    RedisKeyType.TTL_1_YEAR,
                    data
            );
            plugin.debug(String.format("Set map data %s:%s on Redis", serverName, mapId));
        } catch (Throwable e) {
            plugin.log(Level.SEVERE, "An exception occurred setting map data on Redis", e);
        }
    }

    @Blocking
    public byte @Nullable [] getMapData(@NotNull String serverName, int mapId) {
        try (Jedis jedis = jedisPool.getResource()) {
            final byte[] readData = jedis.get(getMapDataKey(serverName, mapId, clusterId));
            if (readData == null) {
                plugin.debug(String.format("[%s:%s] No map data on Redis",
                        serverName, mapId));
                return null;
            }
            plugin.debug(String.format("[%s:%s] Read map data from Redis",
                    serverName, mapId));

            return readData;
        } catch (Throwable e) {
            plugin.log(Level.SEVERE, "An exception occurred reading map data from Redis", e);
            return null;
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

    private static byte[] getMapIdKey(@NotNull String fromServer, int fromId, @NotNull String toServer, @NotNull String clusterId) {
        return String.format("%s:%s:%s:%s", RedisKeyType.MAP_ID.getKeyPrefix(clusterId), fromServer, fromId, toServer).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] getReversedMapIdKey(@NotNull String toServer, int toId, @NotNull String clusterId) {
        return String.format("%s:%s:%s", RedisKeyType.MAP_ID_REVERSED.getKeyPrefix(clusterId), toServer, toId).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] getMapDataKey(@NotNull String serverName, int mapId, @NotNull String clusterId) {
        return String.format("%s:%s:%s", RedisKeyType.MAP_DATA.getKeyPrefix(clusterId), serverName, mapId).getBytes(StandardCharsets.UTF_8);
    }

}
