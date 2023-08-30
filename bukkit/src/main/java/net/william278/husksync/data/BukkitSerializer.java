package net.william278.husksync.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBTCompoundList;
import net.william278.husksync.HuskSync;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class BukkitSerializer {

    protected final HuskSync plugin;

    public BukkitSerializer(@NotNull HuskSync plugin) {
        this.plugin = plugin;
    }

    public static class Inventory extends BukkitSerializer implements Serializer<BukkitDataContainer.Items.Inventory> {
        private static final String ITEMS_TAG = "items";
        private static final String HELD_ITEM_SLOT_TAG = "held_item_slot";

        public Inventory(@NotNull HuskSync plugin) {
            super(plugin);
        }

        @Override
        public BukkitDataContainer.Items.Inventory deserialize(byte[] serialized) throws DeserializationException {
            final ReadWriteNBT root = NBT.parseNBT(new String(serialized, StandardCharsets.UTF_8));
            final ItemStack[] items = root.getItemStackArray(ITEMS_TAG);
            final int selectedHotbarSlot = root.getInteger(HELD_ITEM_SLOT_TAG);
            return BukkitDataContainer.Items.Inventory.from(items, selectedHotbarSlot);
        }

        @NotNull
        @Override
        public byte[] serialize(@NotNull BukkitDataContainer.Items.Inventory data) throws SerializationException {
            final ReadWriteNBT root = NBT.createNBTObject();
            root.setItemStackArray(ITEMS_TAG, data.getContents());
            root.setInteger(HELD_ITEM_SLOT_TAG, data.getHeldItemSlot());
            return root.toString().getBytes(StandardCharsets.UTF_8);
        }
    }

    public static class EnderChest extends BukkitSerializer implements Serializer<BukkitDataContainer.Items.EnderChest> {

        public EnderChest(@NotNull HuskSync plugin) {
            super(plugin);
        }

        @Override
        public BukkitDataContainer.Items.EnderChest deserialize(byte[] serialized) throws DeserializationException {
            return BukkitDataContainer.Items.EnderChest.adapt(
                    NBT.itemStackArrayFromNBT(NBT.parseNBT(new String(serialized, StandardCharsets.UTF_8)))
            );
        }

        @NotNull
        @Override
        public byte[] serialize(@NotNull BukkitDataContainer.Items.EnderChest data) throws SerializationException {
            return NBT.itemStackArrayToNBT(data.getContents()).toString().getBytes(StandardCharsets.UTF_8);
        }
    }

    public static class PotionEffects extends BukkitSerializer implements Serializer<BukkitDataContainer.PotionEffects> {

        private static final String SIZE_TAG = "size";
        private static final String EFFECTS_TAG = "effects";
        private static final String ID_TAG = "id";
        private static final String DURATION_TAG = "duration";
        private static final String AMPLIFIER_TAG = "amplifier";
        private static final String AMBIENT_TAG = "ambient";
        private static final String PARTICLES_TAG = "particles";
        private static final String ICON_TAG = "icon";

        public PotionEffects(@NotNull HuskSync plugin) {
            super(plugin);
        }

        @Override
        public BukkitDataContainer.PotionEffects deserialize(byte[] serialized) throws DeserializationException {
            final ReadWriteNBT root = NBT.parseNBT(new String(serialized, StandardCharsets.UTF_8));
            final int size = root.getInteger(SIZE_TAG);
            final ReadWriteNBTCompoundList effectsList = root.getCompoundList(EFFECTS_TAG);
            final PotionEffect[] effects = new PotionEffect[size];
            for (int i = 0; i < size; i++) {
                final ReadWriteNBT compound = effectsList.get(i);
                final PotionEffectType type = PotionEffectType.getByName(compound.getString(ID_TAG));

                if (type == null) {
                    continue;
                }
                effects[i] = new PotionEffect(
                        type,
                        compound.getInteger(DURATION_TAG),
                        compound.getInteger(AMPLIFIER_TAG),
                        compound.getBoolean(AMBIENT_TAG),
                        compound.getBoolean(PARTICLES_TAG),
                        compound.getBoolean(ICON_TAG)
                );
            }
            return BukkitDataContainer.PotionEffects.adapt(effects);
        }

        @NotNull
        @Override
        public byte[] serialize(@NotNull BukkitDataContainer.PotionEffects element) throws SerializationException {
            final PotionEffect[] effects = element.getEffects();
            final ReadWriteNBT root = NBT.createNBTObject();
            root.setInteger(SIZE_TAG, effects.length);
            final ReadWriteNBTCompoundList effectsList = root.getCompoundList(EFFECTS_TAG);
            for (PotionEffect effect : effects) {
                final ReadWriteNBT compound = effectsList.addCompound();
                compound.setString(ID_TAG, effect.getType().getName());
                compound.setInteger(DURATION_TAG, effect.getDuration());
                compound.setInteger(AMPLIFIER_TAG, effect.getAmplifier());
                compound.setBoolean(AMBIENT_TAG, effect.isAmbient());
                compound.setBoolean(PARTICLES_TAG, effect.hasParticles());
                compound.setBoolean(ICON_TAG, effect.hasIcon());
            }
            return root.toString().getBytes(StandardCharsets.UTF_8);
        }
    }

    public static class Advancements extends BukkitSerializer implements Serializer<BukkitDataContainer.Advancements> {

        private static final TypeToken<List<DataContainer.Advancements.CompletedAdvancement>> TOKEN = new TypeToken<>() {
        };

        public Advancements(@NotNull HuskSync plugin) {
            super(plugin);
        }

        @SuppressWarnings("UnstableApiUsage")
        @Override
        public BukkitDataContainer.Advancements deserialize(byte[] serialized) throws DeserializationException {
            return BukkitDataContainer.Advancements.from(
                    new Gson().fromJson(new String(serialized, StandardCharsets.UTF_8), TOKEN.getType()), plugin
            );
        }

        @NotNull
        @Override
        public byte[] serialize(@NotNull BukkitDataContainer.Advancements element) throws SerializationException {
            return new Gson().toJson(element.getCompleted()).getBytes(StandardCharsets.UTF_8);
        }
    }

}
