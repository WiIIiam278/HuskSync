package net.william278.husksync.migrator;

import net.william278.husksync.HuskSync;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

//todo: implement this
public class LegacyMigrator extends Migrator {

    public LegacyMigrator(@NotNull HuskSync plugin) {
        super(plugin);
    }

    @Override
    public CompletableFuture<Boolean> start() {
        return null;
    }

    @Override
    public void handleConfigurationCommand(@NotNull String[] args) {

    }

    @NotNull
    @Override
    public String getIdentifier() {
        return "legacy";
    }

    @NotNull
    @Override
    public String getName() {
        return "HuskSync v1.x --> v2.x";
    }

    @NotNull
    @Override
    public String getHelpMenu() {
        return null;
    }

}
