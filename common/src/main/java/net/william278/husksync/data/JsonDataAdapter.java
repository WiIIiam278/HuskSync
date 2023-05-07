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

import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

public class JsonDataAdapter implements DataAdapter {

    @Override
    public byte[] toBytes(@NotNull UserData data) throws DataAdaptionException {
        return toJson(data, false).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public @NotNull String toJson(@NotNull UserData data, boolean pretty) throws DataAdaptionException {
        return (pretty ? new GsonBuilder().setPrettyPrinting() : new GsonBuilder()).create().toJson(data);
    }

    @Override
    public @NotNull UserData fromBytes(byte[] data) throws DataAdaptionException {
        try {
            return new GsonBuilder().create().fromJson(new String(data, StandardCharsets.UTF_8), UserData.class);
        } catch (JsonSyntaxException e) {
            throw new DataAdaptionException("Failed to parse JSON data", e);
        }
    }
}
