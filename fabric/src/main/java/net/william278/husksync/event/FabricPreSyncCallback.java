package net.william278.husksync.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.util.ActionResult;
import net.william278.husksync.HuskSync;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.user.OnlineUser;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.NotNull;

public interface FabricPreSyncCallback extends FabricEventCallback<PreSyncEvent> {

    @NotNull
    Event<FabricPreSyncCallback> EVENT = EventFactory.createArrayBacked(FabricPreSyncCallback.class,
            (listeners) -> (event) -> {
                for (FabricPreSyncCallback listener : listeners) {
                    final ActionResult result = listener.invoke(event);
                    if (event.isCancelled()) {
                        return ActionResult.CONSUME;
                    } else if (result != ActionResult.PASS) {
                        event.setCancelled(true);
                        return result;
                    }
                }

                return ActionResult.PASS;
            });

    @NotNull
    TriFunction<OnlineUser, DataSnapshot.Packed, HuskSync, PreSyncEvent> SUPPLIER = (user, data, plugin) ->

            new PreSyncEvent() {
                private boolean cancelled = false;

                @Override
                public boolean isCancelled() {
                    return cancelled;
                }

                @Override
                public void setCancelled(boolean cancelled) {
                    this.cancelled = cancelled;
                }

                @NotNull
                @Override
                public DataSnapshot.Packed getData() {
                    return data;
                }

                @NotNull
                @Override
                public HuskSync getPlugin() {
                    return plugin;
                }

                @NotNull
                @Override
                public OnlineUser getUser() {
                    return user;
                }

                @NotNull
                @SuppressWarnings("unused")
                public Event<FabricPreSyncCallback> getEvent() {
                    return EVENT;
                }
            };

}
