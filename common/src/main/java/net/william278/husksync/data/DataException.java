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

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.william278.husksync.HuskSync;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

/**
 * An exception related to {@link DataSnapshot} formatting, thrown if an exception occurs when unpacking a snapshot
 */
@Getter
public class DataException extends IllegalStateException {

    private final Reason reason;

    private DataException(@NotNull DataException.Reason reason, @NotNull DataSnapshot data, @NotNull HuskSync plugin) {
        super(reason.getMessage(plugin, data));
        this.reason = reason;
    }

    /**
     * Reasons why {@link DataException}s were thrown
     */
    @AllArgsConstructor
    public enum Reason {
        INVALID_MINECRAFT_VERSION((plugin, snapshot) -> String.format("The Minecraft version of the snapshot (%s) is " +
                                                                      "newer than the server's version (%s). Ensure each server is on the same version of Minecraft.",
                snapshot.getMinecraftVersion(), plugin.getMinecraftVersion())),
        INVALID_FORMAT_VERSION((plugin, snapshot) -> String.format("The format version of the snapshot (%s) is newer " +
                                                                   "than the server's version (%s). Ensure each server is running the same version of HuskSync.",
                snapshot.getFormatVersion(), DataSnapshot.CURRENT_FORMAT_VERSION)),
        INVALID_PLATFORM_TYPE((plugin, snapshot) -> String.format("The platform type of the snapshot (%s) does " +
                                                                  "not match the server's platform type (%s). Ensure each server has the same platform type.",
                snapshot.getPlatformType(), plugin.getPlatformType())),
        NO_LEGACY_CONVERTER((plugin, snapshot) -> String.format("No legacy converter to convert format version: %s",
                snapshot.getFormatVersion()));

        private final BiFunction<HuskSync, DataSnapshot, String> exception;

        @NotNull
        String getMessage(@NotNull HuskSync plugin, @NotNull DataSnapshot data) {
            return exception.apply(plugin, data);
        }

        @NotNull
        DataException toException(@NotNull DataSnapshot data, @NotNull HuskSync plugin) {
            return new DataException(this, data, plugin);
        }
    }

}
