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
