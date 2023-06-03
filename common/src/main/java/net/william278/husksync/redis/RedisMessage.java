/*
 * This file is part of HuskSync by William278. Do not redistribute!
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  All rights reserved.
 *
 *  This source code is provided as reference to licensed individuals that have purchased the HuskSync
 *  plugin once from any of the official sources it is provided. The availability of this code does
 *  not grant you the rights to modify, re-distribute, compile or redistribute this source code or
 *  "plugin" outside this intended purpose. This license does not cover libraries developed by third
 *  parties that are utilised in the plugin.
 */

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