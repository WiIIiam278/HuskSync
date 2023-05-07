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

import java.util.Optional;

/**
 * Represents the type of a {@link PersistentDataTag}
 */
public enum PersistentDataTagType {

    BYTE,
    SHORT,
    INTEGER,
    LONG,
    FLOAT,
    DOUBLE,
    STRING,
    BYTE_ARRAY,
    INTEGER_ARRAY,
    LONG_ARRAY,
    TAG_CONTAINER_ARRAY,
    TAG_CONTAINER;


    public static Optional<PersistentDataTagType> getDataType(@NotNull String typeName) {
        for (PersistentDataTagType type : values()) {
            if (type.name().equalsIgnoreCase(typeName)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

}
