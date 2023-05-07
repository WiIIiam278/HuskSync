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
 * Represents a persistent data tag set by a plugin.
 */
public class PersistentDataTag<T> {

    /**
     * The enumerated primitive data type name value of the tag
     */
    protected String type;

    /**
     * The value of the tag
     */
    public T value;

    public PersistentDataTag(@NotNull PersistentDataTagType type, @NotNull T value) {
        this.type = type.name();
        this.value = value;
    }

    @SuppressWarnings("unused")
    private PersistentDataTag() {
    }

    public Optional<PersistentDataTagType> getType() {
        return PersistentDataTagType.getDataType(type);
    }

}
