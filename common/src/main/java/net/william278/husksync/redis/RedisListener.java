package net.william278.husksync.redis;

import net.william278.husksync.Settings;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;
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
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxIdle(0);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        if (Settings.redisPassword.isEmpty()) {
            jedisPool = new JedisPool(config,
                    Settings.redisHost,
                    Settings.redisPort,
                    0,
                    Settings.redisSSL);
        } else {
            jedisPool = new JedisPool(config,
                    Settings.redisHost,
                    Settings.redisPort,
                    0,
                    Settings.redisPassword,
                    Settings.redisSSL);
        }
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
        new Thread(() -> {
            isActiveAndEnabled = true;
            while (isActiveAndEnabled) {

                Jedis subscriber;
                if (Settings.redisPassword.isEmpty()) {
                    subscriber = new Jedis(Settings.redisHost,
                            Settings.redisPort,
                            0);
                } else {
                    final JedisClientConfig config = DefaultJedisClientConfig.builder()
                            .password(Settings.redisPassword)
                            .timeoutMillis(0).build();

                    subscriber = new Jedis(Settings.redisHost,
                            Settings.redisPort,
                            config);
                }
                subscriber.connect();

                log(Level.INFO, "Enabled Redis listener successfully!");
                try {
                    subscriber.subscribe(new JedisPubSub() {
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
                } catch (JedisConnectionException connectionException) {
                    log(Level.SEVERE, "A connection exception occurred with the Jedis listener");
                    connectionException.printStackTrace();
                } catch (JedisException jedisException) {
                    isActiveAndEnabled = false;
                    log(Level.SEVERE, "An exception occurred with the Jedis listener");
                    jedisException.printStackTrace();
                } finally {
                    subscriber.close();
                }
            }
        }, "Redis Subscriber").start();
    }
}
