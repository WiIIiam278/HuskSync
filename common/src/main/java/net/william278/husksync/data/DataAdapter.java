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

/**
 * An adapter that adapts {@link UserData} to and from a portable byte array.
 */
public interface DataAdapter {

    /**
     * Converts {@link UserData} to a byte array
     *
     * @param data The {@link UserData} to adapt
     * @return The byte array.
     * @throws DataAdaptionException If an error occurred during adaptation.
     */
    byte[] toBytes(@NotNull UserData data) throws DataAdaptionException;

    /**
     * Serializes {@link UserData} to a JSON string.
     *
     * @param data   The {@link UserData} to serialize
     * @param pretty Whether to pretty print the JSON.
     * @return The output json string.
     * @throws DataAdaptionException If an error occurred during adaptation.
     */
    @NotNull
    String toJson(@NotNull UserData data, boolean pretty) throws DataAdaptionException;

    /**
     * Converts a byte array to {@link UserData}.
     *
     * @param data The byte array to adapt.
     * @return The {@link UserData}.
     * @throws DataAdaptionException If an error occurred during adaptation, such as if the byte array is invalid.
     */
    @NotNull
    UserData fromBytes(final byte[] data) throws DataAdaptionException;

}
