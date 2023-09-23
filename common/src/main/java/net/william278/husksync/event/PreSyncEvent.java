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

package net.william278.husksync.event;

import net.william278.husksync.HuskSync;
import net.william278.husksync.data.DataSnapshot;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

@SuppressWarnings("unused")
public interface PreSyncEvent extends PlayerEvent, Cancellable {

    @NotNull
    DataSnapshot.Packed getData();

    default void editData(@NotNull Consumer<DataSnapshot.Unpacked> editor) {
        getData().edit(getPlugin(), editor);
    }

    @NotNull
    default DataSnapshot.SaveCause getSaveCause() {
        return getData().getSaveCause();
    }

    @NotNull
    @ApiStatus.Internal
    HuskSync getPlugin();

}
