package net.william278.husksync.event;

import java.util.concurrent.CompletableFuture;

public interface Event {

    CompletableFuture<Event> fire();

}
