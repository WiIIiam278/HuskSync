package net.william278.husksync.migrator;

import net.william278.husksync.HuskSync;
import org.jetbrains.annotations.NotNull;
import net.william278.husksync.data.UserData;

import java.util.concurrent.CompletableFuture;

/**
 * A migrator that migrates data from other data formats to HuskSync's {@link UserData} format
 */
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
     *
     * @param args The command arguments
     */
    public abstract void handleConfigurationCommand(@NotNull String[] args);

    /**
     * Obfuscates a data string to prevent important data from being logged to console
     *
     * @param dataString The data string to obfuscate
     * @return The data string obfuscated with stars (*)
     */
    protected final String obfuscateDataString(@NotNull String dataString) {
        return (dataString.length() > 1 ? dataString.charAt(0) + "*".repeat(dataString.length() - 1) : "");
    }

    @NotNull
    public abstract String getIdentifier();

    @NotNull
    public abstract String getName();

    @NotNull
    public abstract String getHelpMenu();

}
