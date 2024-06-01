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

package net.william278.husksync.util;

import net.william278.husksync.FabricHuskSync;
import net.william278.husksync.HuskSync;
import net.william278.husksync.data.UserDataHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public interface FabricTask extends Task {

    class Sync extends Task.Sync implements FabricTask {

        protected Sync(@NotNull HuskSync plugin, @NotNull Runnable runnable, long delayTicks) {
            super(plugin, runnable, delayTicks);
        }

        @Override
        public void cancel() {
            super.cancel();
        }

        @Override
        public void run() {
            if (!cancelled) {
                Executors.newSingleThreadScheduledExecutor().schedule(
                        () -> ((FabricHuskSync) getPlugin()).getMinecraftServer().executeSync(runnable),
                        delayTicks * 50,
                        TimeUnit.MILLISECONDS
                );
            }
        }
    }

    class Async extends Task.Async implements FabricTask {
        private CompletableFuture<Void> task;

        protected Async(@NotNull HuskSync plugin, @NotNull Runnable runnable, long delayTicks) {
            super(plugin, runnable, delayTicks);
        }

        @Override
        public void cancel() {
            if (task != null && !cancelled) {
                task.cancel(true);
            }
            super.cancel();
        }

        @Override
        public void run() {
            if (!cancelled) {
                this.task = CompletableFuture.runAsync(runnable, ((FabricHuskSync) getPlugin()).getMinecraftServer());
            }
        }
    }

    class Repeating extends Task.Repeating implements FabricTask {

        private ScheduledFuture<?> task;

        protected Repeating(@NotNull HuskSync plugin, @NotNull Runnable runnable, long repeatingTicks) {
            super(plugin, runnable, repeatingTicks);
        }

        @Override
        public void cancel() {
            if (task != null && !cancelled) {
                task.cancel(true);
            }
            super.cancel();
        }

        @Override
        public void run() {
            if (!cancelled) {
                this.task = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                        runnable,
                        0,
                        repeatingTicks * 50,
                        TimeUnit.MILLISECONDS
                );
            }
        }
    }

    interface Supplier extends Task.Supplier {

        @NotNull
        @Override
        default Task.Sync getSyncTask(@NotNull Runnable runnable, @Nullable UserDataHolder user, long delayTicks) {
            return new Sync(getPlugin(), runnable, delayTicks);
        }

        @NotNull
        @Override
        default Task.Async getAsyncTask(@NotNull Runnable runnable, long delayTicks) {
            return new Async(getPlugin(), runnable, delayTicks);
        }

        @NotNull
        @Override
        default Task.Repeating getRepeatingTask(@NotNull Runnable runnable, long repeatingTicks) {
            return new Repeating(getPlugin(), runnable, repeatingTicks);
        }

        @Override
        default void cancelTasks() {
            // Do nothing
        }

    }

}
