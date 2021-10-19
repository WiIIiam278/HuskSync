package me.william278.crossserversync.redis;

import me.william278.crossserversync.Settings;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.logging.Level;

public abstract class RedisListener {

    /**
     * Handle an incoming {@link RedisMessage}
     * @param message The {@link RedisMessage} to handle
     */
    public abstract void handleMessage(RedisMessage message);

    /**
     * Log to console
     * @param level The {@link Level} to log
     * @param message Message to log
     */
    public abstract void log(Level level, String message);

    /**
     * Start the Redis listener
     */
    public final void listen() {
        Jedis jedis = new Jedis(Settings.redisHost, Settings.redisPort);
        final String jedisPassword = Settings.redisPassword;
        if (!jedisPassword.equals("")) {
            jedis.auth(jedisPassword);
        }
        jedis.connect();
        if (jedis.isConnected()) {
            log(Level.INFO,"Enabled Redis listener successfully!");
            new Thread(() -> jedis.subscribe(new JedisPubSub() {
                @Override
                public void onMessage(String channel, String message) {
                    // Only accept messages to the CrossServerSync channel
                    if (!channel.equals(RedisMessage.REDIS_CHANNEL)) {
                        return;
                    }

                    // Handle the message
                    handleMessage(new RedisMessage(message));
                }
            }, RedisMessage.REDIS_CHANNEL), "Redis Subscriber").start();
        } else {
            log(Level.SEVERE, "Failed to initialize the redis listener!");
        }
    }
}
