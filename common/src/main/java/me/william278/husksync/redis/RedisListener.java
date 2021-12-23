package me.william278.husksync.redis;

import me.william278.husksync.Settings;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisException;

import java.io.IOException;
import java.util.logging.Level;

public abstract class RedisListener {

    /**
     * Determines if the RedisListener is working properly
     */
    public boolean isActiveAndEnabled;

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
     * Start the Redis listener
     */
    public final void listen() {
        try (Jedis jedis = new Jedis(Settings.redisHost, Settings.redisPort)) {
            final String jedisPassword = Settings.redisPassword;
            jedis.connect();
            if (jedis.isConnected()) {
                if (!jedisPassword.equals("")) {
                    jedis.auth(jedisPassword);
                }
                isActiveAndEnabled = true;
                log(Level.INFO, "Enabled Redis listener successfully!");
                new Thread(() -> jedis.subscribe(new JedisPubSub() {
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
                }, RedisMessage.REDIS_CHANNEL), "Redis Subscriber").start();
            } else {
                isActiveAndEnabled = false;
                log(Level.SEVERE, "Failed to initialize the redis listener!");
            }
        } catch (JedisException e) {
            log(Level.SEVERE, "Failed to establish a connection to the Redis server!");
        }
    }
}
