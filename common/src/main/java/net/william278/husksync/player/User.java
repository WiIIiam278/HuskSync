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

package net.william278.husksync.player;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents a user who has their data synchronised by HuskSync
 */
public class User {

    /**
     * The user's unique account ID
     */
    public final UUID uuid;

    /**
     * The user's username
     */
    public final String username;

    public User(@NotNull UUID uuid, @NotNull String username) {
        this.username = username;
        this.uuid = uuid;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof User other) {
            return this.uuid.equals(other.uuid);
        }
        return super.equals(object);
    }
}
