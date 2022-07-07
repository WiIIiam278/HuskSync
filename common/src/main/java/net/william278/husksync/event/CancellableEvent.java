package net.william278.husksync.event;

public interface CancellableEvent extends Event {

    boolean isCancelled();

    void setCancelled(boolean cancelled);

}
