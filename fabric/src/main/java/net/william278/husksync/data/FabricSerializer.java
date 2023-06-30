package net.william278.husksync.data;

import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class FabricSerializer {
    // TODO: Implement
    public static CompletableFuture<String> serializeItemStackArray(@NotNull ItemStack[] inventoryContents)
            throws DataSerializationException {
        return CompletableFuture.supplyAsync(() -> {
            return null;
        });
    }
}
