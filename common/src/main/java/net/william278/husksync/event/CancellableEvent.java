package net.william278.husksync.event;

public interface CancellableEvent extends Event {

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    default boolean isCancelled() {
        return false;
    }

    void setCancelled(boolean cancelled);

}
