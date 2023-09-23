package net.william278.husksync.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.util.ActionResult;
import net.william278.husksync.HuskSync;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.user.OnlineUser;
import net.william278.husksync.user.User;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.NotNull;

public interface FabricDataSaveCallback extends FabricEventCallback<DataSaveEvent> {

    @NotNull
    Event<FabricDataSaveCallback> EVENT = EventFactory.createArrayBacked(FabricDataSaveCallback.class,
            (listeners) -> (event) -> {
                for (FabricDataSaveCallback listener : listeners) {
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
    TriFunction<User, DataSnapshot.Packed, HuskSync, DataSaveEvent> SUPPLIER = (user, data, plugin) ->

            new DataSaveEvent() {
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
                public User getUser() {
                    return user;
                }

                @NotNull
                public Event<FabricDataSaveCallback> getEvent() {
                    return EVENT;
                }
            };

}
