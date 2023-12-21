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

package net.william278.husksync.redis;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum RedisKeyType {

    LATEST_SNAPSHOT,
    SERVER_SWITCH,
    DATA_CHECKOUT;

    public static final int TTL_1_YEAR = 60 * 60 * 24 * 7 * 52; // 1 year
    public static final int TTL_10_SECONDS = 10; // 10 seconds

    @NotNull
    public String getKeyPrefix(@NotNull String clusterId) {
        return String.format(
                "%s:%s:%s",
                RedisManager.KEY_NAMESPACE.toLowerCase(Locale.ENGLISH),
                clusterId.toLowerCase(Locale.ENGLISH),
                name().toLowerCase(Locale.ENGLISH)
        );
    }

}
