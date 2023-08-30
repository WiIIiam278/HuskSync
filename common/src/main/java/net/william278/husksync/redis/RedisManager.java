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
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisException;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

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
    public void initialize() throws IllegalStateException {
        if (redisPassword.isBlank()) {
            jedisPool = new JedisPool(jedisPoolConfig, redisHost, redisPort, 0, redisUseSsl);
        } else {
            jedisPool = new JedisPool(jedisPoolConfig, redisHost, redisPort, 0, redisPassword, redisUseSsl);
        }
        try {
            jedisPool.getResource().ping();
        } catch (JedisException e) {
            throw new IllegalStateException("Failed to establish connection with the Redis server."
                    + "Please check the supplied credentials in the config file", e);
        }
        plugin.runAsync(this::subscribe);
    }

    private void subscribe() {
        try (Jedis subscriber = redisPassword.isBlank() ? new Jedis(redisHost, redisPort, 0, redisUseSsl) :
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
        plugin.getOnlineUser(redisMessage.targetUserUuid).ifPresent(
                user -> user.applySnapshot(DataSnapshot.deserialize(plugin, redisMessage.data))
        );
    }

    protected void sendMessage(@NotNull String channel, @NotNull String message) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.publish(channel, message);
        }
    }

    public void sendUserDataUpdate(@NotNull User user, @NotNull DataSnapshot.Packed data) {
        plugin.runAsync(() -> {
            final RedisMessage redisMessage = new RedisMessage(user.getUuid(), data.serialize(plugin));
            redisMessage.dispatch(plugin, RedisMessageType.UPDATE_USER_DATA);
        });
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
                        getKey(RedisKeyType.DATA_UPDATE, user.getUuid()),
                        RedisKeyType.DATA_UPDATE.getTimeToLive(),
                        data.serialize(plugin)
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
                jedis.setex(getKey(RedisKeyType.SERVER_SWITCH, user.getUuid()),
                        RedisKeyType.SERVER_SWITCH.getTimeToLive(), new byte[0]);
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
            final byte[] key = getKey(RedisKeyType.DATA_UPDATE, user.getUuid());
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
            final byte[] key = getKey(RedisKeyType.SERVER_SWITCH, user.getUuid());
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
    }

    private static byte[] getKey(@NotNull RedisKeyType keyType, @NotNull UUID uuid) {
        return (keyType.getKeyPrefix() + ":" + uuid).getBytes(StandardCharsets.UTF_8);
    }

}
