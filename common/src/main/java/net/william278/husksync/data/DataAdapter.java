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
