package net.william278.husksync.util;

import net.william278.husksync.HuskSync;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface TaskRunner {

    int runAsync(@NotNull Runnable runnable);

    <T> CompletableFuture<T> supplyAsync(@NotNull Supplier<T> supplier);

    void runSync(@NotNull Runnable runnable);

    int runAsyncRepeating(@NotNull Runnable runnable, long delay);

    void runLater(@NotNull Runnable runnable, long delay);

    void cancelTask(int taskId);

    void cancelAllTasks();

    @NotNull
    HuskSync getPlugin();

}
