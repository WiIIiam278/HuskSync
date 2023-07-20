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

import org.jetbrains.annotations.NotNull;
import org.xerial.snappy.Snappy;

import java.io.IOException;

public class CompressedDataAdapter extends JsonDataAdapter {

    @Override
    public byte[] toBytes(@NotNull UserData data) throws DataAdaptionException {
        try {
            return Snappy.compress(super.toBytes(data));
        } catch (IOException e) {
            throw new DataAdaptionException("Failed to compress data", e);
        }
    }

    @Override
    @NotNull
    public UserData fromBytes(byte[] data) throws DataAdaptionException {
        try {
            return super.fromBytes(Snappy.uncompress(data));
        } catch (IOException e) {
            throw new DataAdaptionException("Failed to decompress data", e);
        }
    }
}
