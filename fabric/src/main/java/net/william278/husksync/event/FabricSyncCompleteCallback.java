package net.william278.husksync.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.util.ActionResult;
import net.william278.husksync.HuskSync;
import net.william278.husksync.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

public interface FabricSyncCompleteCallback extends FabricEventCallback<SyncCompleteEvent> {

    @NotNull
    Event<FabricSyncCompleteCallback> EVENT = EventFactory.createArrayBacked(FabricSyncCompleteCallback.class,
            (listeners) -> (event) -> {
                for (FabricSyncCompleteCallback listener : listeners) {
                    listener.invoke(event);
                }

                return ActionResult.PASS;
            });

    @NotNull
    BiFunction<OnlineUser, HuskSync, SyncCompleteEvent> SUPPLIER = (user, plugin) ->

            new SyncCompleteEvent() {

                @NotNull
                @Override
                public OnlineUser getUser() {
                    return user;
                }

                @NotNull
                @SuppressWarnings("unused")
                public Event<FabricSyncCompleteCallback> getEvent() {
                    return EVENT;
                }
            };

}
