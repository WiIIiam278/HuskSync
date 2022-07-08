package net.william278.husksync.migrator;

import net.william278.husksync.HuskSync;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public abstract class Migrator {

    protected final HuskSync plugin;

    protected Migrator(@NotNull HuskSync plugin) {
        this.plugin = plugin;
    }

    /**
     * Start the migrator
     *
     * @return A future that will be completed when the migrator is done
     */
    public abstract CompletableFuture<Boolean> start();

    /**
     * Handle a command that sets migrator configuration parameters
     * @param args The command arguments
     */
    public abstract void handleConfigurationCommand(@NotNull String[] args);

    @NotNull
    public abstract String getIdentifier();

    @NotNull
    public abstract String getName();

    @NotNull
    public abstract String getHelpMenu();

}
