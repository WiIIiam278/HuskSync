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
