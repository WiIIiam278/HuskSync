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
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import net.william278.husksync.HuskSync;
import net.william278.husksync.adapter.Adaptable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class RedisMessage implements Adaptable {

    @SerializedName("target_uuid")
    private UUID targetUuid;
    @Getter
    @Setter
    @SerializedName("payload")
    private byte[] payload;

    private RedisMessage(@NotNull UUID targetUuid, byte[] message) {
        this.setTargetUuid(targetUuid);
        this.setPayload(message);
    }

    @SuppressWarnings("unused")
    public RedisMessage() {
    }

    @NotNull
    public static RedisMessage create(@NotNull UUID targetUuid, byte[] message) {
        return new RedisMessage(targetUuid, message);
    }

    @NotNull
    public static RedisMessage fromJson(@NotNull HuskSync plugin, @NotNull String json) throws JsonSyntaxException {
        return plugin.getGson().fromJson(json, RedisMessage.class);
    }

    public void dispatch(@NotNull HuskSync plugin, @NotNull Type type) {
        plugin.runAsync(() -> plugin.getRedisManager().sendMessage(
                type.getMessageChannel(plugin.getSettings().getClusterId()),
                plugin.getGson().toJson(this)
        ));
    }

    @NotNull
    public UUID getTargetUuid() {
        return targetUuid;
    }

    public void setTargetUuid(@NotNull UUID targetUuid) {
        this.targetUuid = targetUuid;
    }

    public enum Type {

        UPDATE_USER_DATA,
        REQUEST_USER_DATA,
        RETURN_USER_DATA;

        @NotNull
        public String getMessageChannel(@NotNull String clusterId) {
            return String.format(
                    "%s:%s:%s",
                    RedisManager.KEY_NAMESPACE.toLowerCase(Locale.ENGLISH),
                    clusterId.toLowerCase(Locale.ENGLISH),
                    name().toLowerCase(Locale.ENGLISH)
            );
        }

        public static Optional<Type> getTypeFromChannel(@NotNull String channel, @NotNull String clusterId) {
            return Arrays.stream(values())
                    .filter(messageType -> messageType.getMessageChannel(clusterId).equalsIgnoreCase(channel))
                    .findFirst();
        }

    }
}