package net.william278.husksync.data;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class FabricInventoryMap {
    public static final int INVENTORY_SLOT_COUNT = 41;

    private final PlayerInventory inventory;

    public FabricInventoryMap(PlayerInventory inventory) {
        this.inventory = inventory;
    }

    public int getSize() {
        return inventory.size();
    }

    public Optional<ItemStack> getItemAt(int index) {
        if (index < inventory.size()) {
            return Optional.ofNullable(inventory.getStack(index));
        }

        return Optional.empty();
    }

    public void setItemAt(@NotNull ItemStack itemStack, int index) throws IllegalArgumentException {
        if (index < inventory.size()) {
            inventory.setStack(index, itemStack);
        } else {
            throw new IllegalArgumentException("Index out of bounds");
        }
    }

    public ItemStack[] getInventory() {
        final ItemStack[] inventory = new ItemStack[getSize()];
        for (int i = 0; i < getSize(); i++) {
            inventory[i] = getItemAt(i).orElse(null);
        }
        return inventory;
    }

    @SuppressWarnings("SpellCheckingInspection")
    public ItemStack[] getHotbar() {
        final ItemStack[] hotbar = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            hotbar[i] = getItemAt(i).orElse(null);
        }
        return hotbar;
    }

    public Optional<ItemStack> getOffHand() {
        return Optional.of(inventory.offHand.get(0));
    }

    public Optional<ItemStack> getHelmet() {
        return Optional.of(inventory.armor.get(3));
    }

    @SuppressWarnings("SpellCheckingInspection")
    // FIXME: typo?
    public Optional<ItemStack> getChestplate() {
        return Optional.of(inventory.armor.get(2));
    }

    public Optional<ItemStack> getLeggings() {
        return Optional.of(inventory.armor.get(1));
    }

    public Optional<ItemStack> getBoots() {
        return Optional.of(inventory.armor.get(0));
    }

    public ItemStack[] getArmor() {
        return inventory.armor.toArray(new ItemStack[0]);
    }
}
