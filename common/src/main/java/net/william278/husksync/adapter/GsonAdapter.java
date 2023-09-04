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

package net.william278.husksync.adapter;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

public class GsonAdapter implements DataAdapter {

    private final Gson gson;

    public GsonAdapter() {
        this.gson = Converters.registerOffsetDateTime(new GsonBuilder()).create();
    }

    @Override
    public <A extends Adaptable> byte[] toBytes(@NotNull A data) throws AdaptionException {
        return this.toJson(data).getBytes(StandardCharsets.UTF_8);
    }

    @NotNull
    @Override
    public <A extends Adaptable> String toJson(@NotNull A data) throws AdaptionException {
        try {
            return gson.toJson(data);
        } catch (Throwable e) {
            throw new AdaptionException("Failed to adapt data to JSON via Gson", e);
        }
    }

    @Override
    @NotNull
    public <A extends Adaptable> A fromBytes(@NotNull byte[] data, @NotNull Class<A> type) throws AdaptionException {
        return this.fromJson(new String(data, StandardCharsets.UTF_8), type);
    }

    @Override
    @NotNull
    public <A extends Adaptable> A fromJson(@NotNull String data, @NotNull Class<A> type) throws AdaptionException {
        try {
            return gson.fromJson(data, type);
        } catch (Throwable e) {
            throw new AdaptionException("Failed to adapt data from JSON via Gson", e);
        }
    }

}
