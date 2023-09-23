package net.william278.husksync.event;

import net.minecraft.util.ActionResult;
import org.jetbrains.annotations.NotNull;

public interface FabricEventCallback<E extends Event> {

    @NotNull
    ActionResult invoke(@NotNull E event);

}
