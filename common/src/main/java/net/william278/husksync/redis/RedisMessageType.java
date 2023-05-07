package net.william278.husksync.redis;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum RedisMessageType {

    UPDATE_USER_DATA;

    @NotNull
    public String getMessageChannel() {
        return RedisManager.KEY_NAMESPACE.toLowerCase(Locale.ENGLISH) + ":" + RedisManager.clusterId.toLowerCase(Locale.ENGLISH)
               + ":" + name().toLowerCase(Locale.ENGLISH);
    }

    public static Optional<RedisMessageType> getTypeFromChannel(@NotNull String messageChannel) {
        return Arrays.stream(values()).filter(messageType -> messageType.getMessageChannel()
                .equalsIgnoreCase(messageChannel)).findFirst();
    }

}