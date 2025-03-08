package net.william278.husksync.redis;

import io.lettuce.core.pubsub.RedisPubSubListener;
import net.william278.husksync.HuskSync;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public interface PubSubListener extends RedisPubSubListener<String, String> {

    @Override
    default void message(String pattern, String channel, String message) {
        getPlugin().log(Level.WARNING, "[Redis] Got message on pattern channel '%s'".formatted(channel));
    }

    @Override
    default void subscribed(String channel, long count) {
        getPlugin().log(Level.INFO, "[Redis] Subscribed to channel '%s'".formatted(channel));
    }

    @Override
    default void unsubscribed(String channel, long count) {
        getPlugin().log(Level.INFO, "[Redis] Unsubscribed from channel '%s'".formatted(channel));
    }

    @Override
    default void psubscribed(String pattern, long count) {
        getPlugin().log(Level.INFO, "[Redis] Subscribed to pattern '%s'".formatted(pattern));
    }

    @Override
    default void punsubscribed(String pattern, long count) {
        getPlugin().log(Level.INFO, "[Redis] Unsubscribed from pattern '%s'".formatted(pattern));
    }

    @NotNull
    HuskSync getPlugin();

}
