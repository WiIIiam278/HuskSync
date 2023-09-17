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

import net.william278.husksync.HuskSync;
import org.jetbrains.annotations.NotNull;
import org.xerial.snappy.Snappy;

import java.io.IOException;

public class SnappyGsonAdapter extends GsonAdapter {

    public SnappyGsonAdapter(@NotNull HuskSync plugin) {
        super(plugin);
    }

    @NotNull
    @Override
    public <A extends Adaptable> byte[] toBytes(@NotNull A data) throws AdaptionException {
        try {
            return Snappy.compress(super.toBytes(data));
        } catch (IOException e) {
            throw new AdaptionException("Failed to compress data through Snappy", e);
        }
    }

    @NotNull
    @Override
    public <A extends Adaptable> A fromBytes(@NotNull byte[] data, @NotNull Class<A> type) throws AdaptionException {
        try {
            return super.fromBytes(decompressBytes(data), type);
        } catch (IOException e) {
            throw new AdaptionException("Failed to decompress data through Snappy", e);
        }
    }

    @Override
    @NotNull
    public String bytesToString(byte[] bytes) {
        try {
            return super.bytesToString(decompressBytes(bytes));
        } catch (IOException e) {
            throw new AdaptionException("Failed to decompress data through Snappy", e);
        }
    }

    private byte[] decompressBytes(byte[] bytes) throws IOException {
        return Snappy.uncompress(bytes);
    }

}
