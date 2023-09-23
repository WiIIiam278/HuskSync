package net.william278.husksync.util;

import net.william278.husksync.FabricHuskSync;
import net.william278.husksync.HuskSync;
import org.jetbrains.annotations.NotNull;

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
        default Task.Sync getSyncTask(@NotNull Runnable runnable, long delayTicks) {
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
