package me.william278.husksync.redis;

import me.william278.husksync.Settings;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisException;

import java.io.IOException;
import java.util.logging.Level;

public abstract class RedisListener {

    /**
     * Determines if the RedisListener is working properly
     */
    public boolean isActiveAndEnabled;

    /**
     * Pool of connections to the Redis server
     */
    private static JedisPool jedisPool;

    /**
     * Creates a new RedisListener and initialises the Redis connection
     */
    public RedisListener() {
        jedisPool = new JedisPool(new JedisPoolConfig(), Settings.redisHost, Settings.redisPort);
    }

    /**
     * Handle an incoming {@link RedisMessage}
     *
     * @param message The {@link RedisMessage} to handle
     */
    public abstract void handleMessage(RedisMessage message);

    /**
     * Log to console
     *
     * @param level   The {@link Level} to log
     * @param message Message to log
     */
    public abstract void log(Level level, String message);

    /**
     * Fetch a connection to the Redis server from the JedisPool
     *
     * @return Jedis instance from the pool
     */
    public static Jedis getJedisConnection() {
        return jedisPool.getResource();
    }

    /**
     * Start the Redis listener
     */
    public final void listen() {
        final String jedisPassword = Settings.redisPassword;
        new Thread(() -> {
            try (Jedis jedis = getJedisConnection()) {
                if (!jedisPassword.equals("")) {
                    jedis.auth(jedisPassword);
                }
                if (jedis.isConnected()) {
                    isActiveAndEnabled = true;
                    log(Level.INFO, "Enabled Redis listener successfully!");
                } else {
                    isActiveAndEnabled = false;
                    log(Level.SEVERE, "Connection to the Redis server could not be established, please check the credentials.");
                    return;
                }
                jedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        // Only accept messages to the HuskSync channel
                        if (!channel.equals(RedisMessage.REDIS_CHANNEL)) {
                            return;
                        }

                        // Handle the message
                        try {
                            handleMessage(new RedisMessage(message));
                        } catch (IOException | ClassNotFoundException e) {
                            log(Level.SEVERE, "Failed to deserialize message target");
                        }
                    }
                }, RedisMessage.REDIS_CHANNEL);
            } catch (JedisException | IllegalStateException e) {
                log(Level.SEVERE, "An exception occurred with the Jedis Subscriber!");
                isActiveAndEnabled = false;
            }
        }, "Redis Subscriber").start();
    }
}
