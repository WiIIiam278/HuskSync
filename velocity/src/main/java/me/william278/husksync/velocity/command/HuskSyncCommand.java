package me.william278.husksync.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class HuskSyncCommand implements SimpleCommand {

    /**
     * Executes the command for the specified invocation.
     *
     * @param invocation the invocation context
     */
    @Override
    public void execute(Invocation invocation) {
        final String[] args = invocation.arguments();
        final CommandSource source = invocation.source();

    }

    /**
     * Provides tab complete suggestions for the specified invocation.
     *
     * @param invocation the invocation context
     * @return the tab complete suggestions
     */
    @Override
    public List<String> suggest(Invocation invocation) {
        return new ArrayList<>();
    }
}