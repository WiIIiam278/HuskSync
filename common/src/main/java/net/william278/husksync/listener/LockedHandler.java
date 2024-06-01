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

package net.william278.husksync.listener;

import net.william278.husksync.HuskSync;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Interface for doing stuff with locked users or when the plugin is disabled
 */
public interface LockedHandler {

    /**
     * Get if a command should be disabled while the user is locked
     */
    default boolean isCommandDisabled(@NotNull String label) {
        final List<String> blocked = getPlugin().getSettings().getSynchronization().getBlacklistedCommandsWhileLocked();
        return blocked.contains("*") || blocked.contains(label.toLowerCase(Locale.ENGLISH));
    }

    /**
     * Determine whether a player event should be canceled
     *
     * @param userUuid The UUID of the user to check
     * @return Whether the event should be canceled
     */
    default boolean cancelPlayerEvent(@NotNull UUID userUuid) {
        return getPlugin().isDisabling() || getPlugin().isLocked(userUuid);
    }

    @NotNull
    @ApiStatus.Internal
    HuskSync getPlugin();

    default void onLoad() {

    }

    default void onEnable() {

    }

    default void onDisable() {

    }

}
