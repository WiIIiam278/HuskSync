package net.william278.husksync.redis;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RedisMessage {

    public UUID targetUserUuid;
    public byte[] data;

    public RedisMessage(@NotNull UUID targetUserUuid, byte[] message) {
        this.targetUserUuid = targetUserUuid;
        this.data = message;
    }

    public RedisMessage() {
    }

    public void dispatch(@NotNull RedisManager redisManager, @NotNull RedisMessageType type) {
        CompletableFuture.runAsync(() -> redisManager.sendMessage(type.getMessageChannel(),
                new GsonBuilder().create().toJson(this)));
    }

    @NotNull
    public static RedisMessage fromJson(@NotNull String json) throws JsonSyntaxException {
        return new GsonBuilder().create().fromJson(json, RedisMessage.class);
    }

}