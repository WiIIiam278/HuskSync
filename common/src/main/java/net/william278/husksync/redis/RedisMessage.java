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

import com.google.gson.JsonSyntaxException;
import net.william278.husksync.HuskSync;
import net.william278.husksync.adapter.Adaptable;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class RedisMessage implements Adaptable {

    public UUID targetUserUuid;
    public byte[] data;

    public RedisMessage(@NotNull UUID targetUserUuid, byte[] message) {
        this.targetUserUuid = targetUserUuid;
        this.data = message;
    }

    @SuppressWarnings("unused")
    public RedisMessage() {
    }

    public void dispatch(@NotNull HuskSync plugin, @NotNull RedisMessageType type) {
        plugin.runAsync(() -> plugin.getRedisManager().sendMessage(
                type.getMessageChannel(plugin.getSettings().getClusterId()),
                plugin.getGson().toJson(this)
        ));
    }

    @NotNull
    public static RedisMessage fromJson(@NotNull HuskSync plugin, @NotNull String json) throws JsonSyntaxException {
        return plugin.getGson().fromJson(json, RedisMessage.class);
    }

}