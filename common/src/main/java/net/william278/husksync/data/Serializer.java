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

package net.william278.husksync.data;

import net.william278.desertwell.util.Version;
import net.william278.husksync.HuskSync;
import net.william278.husksync.adapter.Adaptable;
import org.jetbrains.annotations.NotNull;

public interface Serializer<T extends Data> {

    T deserialize(@NotNull String serialized);

    default T deserialize(@NotNull String serialized, @NotNull Version dataMcVersion) throws DeserializationException {
        return deserialize(serialized);
    }

    @NotNull
    String serialize(@NotNull T element) throws SerializationException;

    final class DeserializationException extends IllegalStateException {
        DeserializationException(@NotNull String message, @NotNull Throwable cause) {
            super(message, cause);
        }
    }

    final class SerializationException extends IllegalStateException {
        SerializationException(@NotNull String message, @NotNull Throwable cause) {
            super(message, cause);
        }
    }


    class Json<T extends Data & Adaptable> implements Serializer<T> {

        private final HuskSync plugin;
        private final Class<T> type;

        public Json(@NotNull HuskSync plugin, @NotNull Class<T> type) {
            this.type = type;
            this.plugin = plugin;
        }

        @Override
        public T deserialize(@NotNull String serialized) throws DeserializationException {
            return plugin.getDataAdapter().fromJson(serialized, type);
        }

        @NotNull
        @Override
        public String serialize(@NotNull T element) throws SerializationException {
            return plugin.getDataAdapter().toJson(element);
        }

    }
}
