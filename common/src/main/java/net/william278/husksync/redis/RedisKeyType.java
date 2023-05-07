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

package net.william278.husksync.redis;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum RedisKeyType {
    CACHE(60 * 60 * 24),
    DATA_UPDATE(10),
    SERVER_SWITCH(10);

    public final int timeToLive;

    RedisKeyType(int timeToLive) {
        this.timeToLive = timeToLive;
    }

    @NotNull
    public String getKeyPrefix() {
        return RedisManager.KEY_NAMESPACE.toLowerCase(Locale.ENGLISH) + ":" + RedisManager.clusterId.toLowerCase(Locale.ENGLISH) + ":" + name().toLowerCase(Locale.ENGLISH);
    }
}
