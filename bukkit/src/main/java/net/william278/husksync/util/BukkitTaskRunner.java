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

package net.william278.husksync.util;

import org.jetbrains.annotations.NotNull;
import space.arim.morepaperlib.scheduling.GracefulScheduling;
import space.arim.morepaperlib.scheduling.ScheduledTask;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public interface BukkitTaskRunner extends TaskRunner {

    @Override
    default void runAsync(@NotNull Runnable runnable) {
        getScheduler().asyncScheduler().run(runnable);
    }

    @Override
    default <T> CompletableFuture<T> supplyAsync(@NotNull Supplier<T> supplier) {
        final CompletableFuture<T> future = new CompletableFuture<>();
        getScheduler().asyncScheduler().run(() -> future.complete(supplier.get()));
        return future;
    }

    @Override
    default void runSync(@NotNull Runnable runnable) {
        getScheduler().globalRegionalScheduler().run(runnable);
    }

    @NotNull
    @Override
    default UUID runAsyncRepeating(@NotNull Runnable runnable, long period) {
        final UUID taskId = UUID.randomUUID();
        getTasks().put(taskId, getScheduler().asyncScheduler().runAtFixedRate(
                runnable, Duration.ZERO, getDurationTicks(period))
        );
        return taskId;
    }

    @Override
    default void runLater(@NotNull Runnable runnable, long delay) {
        getScheduler().asyncScheduler().runDelayed(runnable, getDurationTicks(delay));
    }

    @Override
    default void cancelTask(@NotNull UUID taskId) {
        getTasks().computeIfPresent(taskId, (id, task) -> {
            task.cancel();
            return null;
        });
        getTasks().remove(taskId);
    }

    @Override
    default void cancelAllTasks() {
        getScheduler().cancelGlobalTasks();
        getTasks().values().forEach(ScheduledTask::cancel);
        getTasks().clear();
    }

    @NotNull
    GracefulScheduling getScheduler();

    @NotNull
    @Override
    ConcurrentHashMap<UUID, ScheduledTask> getTasks();

    @NotNull
    default Duration getDurationTicks(long ticks) {
        return Duration.of(ticks * 50, ChronoUnit.MILLIS);
    }

}