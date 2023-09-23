package net.william278.husksync.data;

import com.mojang.brigadier.StringReader;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.StringNbtReader;
import net.william278.husksync.HuskSync;
import net.william278.husksync.api.HuskSyncAPI;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

//TODO
public abstract class FabricSerializer {

    protected final HuskSync plugin;

    private FabricSerializer(@NotNull HuskSync plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("unused")
    public FabricSerializer(@NotNull HuskSyncAPI api) {
        this.plugin = api.getPlugin();
    }

    @ApiStatus.Internal
    @NotNull
    public HuskSync getPlugin() {
        return plugin;
    }

    public static class Inventory extends FabricSerializer implements Serializer<FabricData.Items.Inventory> {
        private static final String ITEMS_TAG = "items";
        private static final String HELD_ITEM_SLOT_TAG = "held_item_slot";

        public Inventory(@NotNull HuskSync plugin) {
            super(plugin);
        }

        @Override
        @SuppressWarnings("DuplicatedCode")
        public FabricData.Items.Inventory deserialize(@NotNull String serialized) throws DeserializationException {
            try {
                final NbtCompound root = StringNbtReader.parse(serialized);
                final NbtList items = root.getList(ITEMS_TAG, NbtElement.COMPOUND_TYPE);
                final ItemStack[] contents = new ItemStack[items.size()];
                for (int i = 0; i < items.size(); i++) {
                    final NbtCompound item = items.getCompound(i);
                    final ItemStack stack = ItemStack.fromNbt(item);
                    items.set(i, stack.writeNbt(new NbtCompound()));
                }
                final int heldItemSlot = root.getInt(HELD_ITEM_SLOT_TAG);
                return FabricData.Items.Inventory.from(
                        contents,
                        heldItemSlot
                );
            } catch (Throwable e) {
                throw new DeserializationException("Failed to read item NBT", e);
            }
        }

        @NotNull
        @Override
        public String serialize(@NotNull FabricData.Items.Inventory data) throws SerializationException {
            final NbtCompound root = new NbtCompound();
            final NbtList items = new NbtList();
            Arrays.stream(data.getContents()).forEach(item -> items.add(item.writeNbt(new NbtCompound())));
            root.put(ITEMS_TAG, items);
            root.putInt(HELD_ITEM_SLOT_TAG, data.getHeldItemSlot());
            return root.toString();
        }

    }

    public static class EnderChest extends FabricSerializer implements Serializer<FabricData.Items.EnderChest> {

        public EnderChest(@NotNull HuskSync plugin) {
            super(plugin);
        }

        @Override
        @SuppressWarnings("DuplicatedCode")
        public FabricData.Items.EnderChest deserialize(@NotNull String serialized) throws DeserializationException {
            try {
                final NbtList items = (NbtList) new StringNbtReader(new StringReader(serialized)).parseElement();
                final ItemStack[] contents = new ItemStack[items.size()];
                for (int i = 0; i < items.size(); i++) {
                    final NbtCompound item = items.getCompound(i);
                    final ItemStack stack = ItemStack.fromNbt(item);
                    items.set(i, stack.writeNbt(new NbtCompound()));
                }
                return FabricData.Items.EnderChest.adapt(contents);
            } catch (Throwable e) {
                throw new DeserializationException("Failed to read item NBT", e);
            }
        }

        @NotNull
        @Override
        public String serialize(@NotNull FabricData.Items.EnderChest data) throws SerializationException {
            final NbtList items = new NbtList();
            Arrays.stream(data.getContents()).forEach(item -> items.add(item.writeNbt(new NbtCompound())));
            return items.toString();
        }
    }
    
}
