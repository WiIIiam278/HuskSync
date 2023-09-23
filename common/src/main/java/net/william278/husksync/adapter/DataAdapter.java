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

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

/**
 * An adapter that adapts data to and from a portable byte array.
 */
public interface DataAdapter {

    /**
     * Converts an {@link Adaptable} to a string.
     *
     * @param data The {@link Adaptable} to adapt
     * @param <A>  The type of the {@link Adaptable}
     * @return The string
     * @throws AdaptionException If an error occurred during adaptation.
     */
    @NotNull
    default <A extends Adaptable> String toString(@NotNull A data) throws AdaptionException {
        return new String(this.toBytes(data), StandardCharsets.UTF_8);
    }

    /**
     * Converts an {@link Adaptable} to a byte array.
     *
     * @param data The {@link Adaptable} to adapt
     * @param <A>  The type of the {@link Adaptable}
     * @return The byte array
     * @throws AdaptionException If an error occurred during adaptation.
     */
    <A extends Adaptable> byte[] toBytes(@NotNull A data) throws AdaptionException;

    /**
     * Converts a JSON string to an {@link Adaptable}.
     *
     * @param data The JSON string to adapt.
     * @param type The class type of the {@link Adaptable} to adapt to.
     * @param <A>  The type of the {@link Adaptable}
     * @return The {@link Adaptable}
     * @throws AdaptionException If an error occurred during adaptation.
     */
    @NotNull
    <A extends Adaptable> A fromJson(@NotNull String data, @NotNull Class<A> type) throws AdaptionException;

    /**
     * Converts an {@link Adaptable} to a JSON string.
     *
     * @param data The {@link Adaptable} to adapt
     * @param <A>  The type of the {@link Adaptable}
     * @return The JSON string
     * @throws AdaptionException If an error occurred during adaptation.
     */
    @NotNull
    <A extends Adaptable> String toJson(@NotNull A data) throws AdaptionException;

    /**
     * Converts a byte array to an {@link Adaptable}.
     *
     * @param data The byte array to adapt.
     * @param type The class type of the {@link Adaptable} to adapt to.
     * @param <A>  The type of the {@link Adaptable}
     * @return The {@link Adaptable}
     * @throws AdaptionException If an error occurred during adaptation.
     */
    <A extends Adaptable> A fromBytes(@NotNull byte[] data, @NotNull Class<A> type) throws AdaptionException;

    /**
     * Converts a byte array to a string, including decompression if required.
     *
     * @param bytes The byte array to convert
     * @return the string form of the bytes
     */
    @NotNull
    String bytesToString(byte[] bytes);

    final class AdaptionException extends IllegalStateException {
        static final String FORMAT = "An exception occurred when adapting serialized/deserialized data: %s";

        public AdaptionException(@NotNull String message, @NotNull Throwable cause) {
            super(String.format(FORMAT, message), cause);
        }

        public AdaptionException(@NotNull String message) {
            super(String.format(FORMAT, message));
        }
    }
}