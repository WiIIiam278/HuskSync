package net.william278.husksync.data;

import net.william278.husksync.BukkitHuskSync;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class BukkitSerializer {

    /**
     * Returns a serialized array of {@link ItemStack}s
     *
     * @param inventoryContents The contents of the inventory
     * @return The serialized inventory contents
     */
    public static CompletableFuture<String> serializeItemStackArray(@NotNull ItemStack[] inventoryContents)
            throws DataSerializationException {
        return CompletableFuture.supplyAsync(() -> {
            // Return an empty string if there is no inventory item data to serialize
            if (inventoryContents.length == 0) {
                return "";
            }

            // Create an output stream that will be encoded into base 64
            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();

            try (BukkitObjectOutputStream bukkitOutputStream = new BukkitObjectOutputStream(byteOutputStream)) {
                // Define the length of the inventory array to serialize
                bukkitOutputStream.writeInt(inventoryContents.length);

                // Write each serialize each ItemStack to the output stream
                for (ItemStack inventoryItem : inventoryContents) {
                    bukkitOutputStream.writeObject(serializeItemStack(inventoryItem));
                }

                // Return encoded data, using the encoder from SnakeYaml to get a ByteArray conversion
                return Base64Coder.encodeLines(byteOutputStream.toByteArray());
            } catch (IOException e) {
                BukkitHuskSync.getInstance().getLoggingAdapter().log(Level.SEVERE, "Failed to serialize item stack data", e);
                throw new DataSerializationException("Failed to serialize item stack data", e);
            }
        });
    }

    /**
     * Returns a {@link BukkitInventoryMap} from a serialized array of ItemStacks representing the contents of a player's inventory.
     *
     * @param serializedPlayerInventory The serialized {@link ItemStack} inventory array
     * @return The deserialized ItemStacks, mapped for convenience as a {@link BukkitInventoryMap}
     * @throws DataSerializationException If the serialized item stack array could not be deserialized
     */
    public static CompletableFuture<BukkitInventoryMap> deserializeInventory(@NotNull String serializedPlayerInventory)
            throws DataSerializationException {
        return CompletableFuture.supplyAsync(() -> new BukkitInventoryMap(deserializeItemStackArray(serializedPlayerInventory).join()));
    }

    /**
     * Returns an array of ItemStacks from serialized inventory data.
     *
     * @param serializeItemStackArray The serialized {@link ItemStack} array
     * @return The deserialized array of {@link ItemStack}s
     * @throws DataSerializationException If the serialized item stack array could not be deserialized
     * @implNote Empty slots will be represented by {@code null}
     */
    public static CompletableFuture<ItemStack[]> deserializeItemStackArray(@NotNull String serializeItemStackArray)
            throws DataSerializationException {
        return CompletableFuture.supplyAsync(() -> {
            // Return empty array if there is no inventory data (set the player as having an empty inventory)
            if (serializeItemStackArray.isEmpty()) {
                return new ItemStack[0];
            }

            // Create a byte input stream to read the serialized data
            try (ByteArrayInputStream byteInputStream = new ByteArrayInputStream(Base64Coder.decodeLines(serializeItemStackArray))) {
                try (BukkitObjectInputStream bukkitInputStream = new BukkitObjectInputStream(byteInputStream)) {
                    // Read the length of the Bukkit input stream and set the length of the array to this value
                    ItemStack[] inventoryContents = new ItemStack[bukkitInputStream.readInt()];

                    // Set the ItemStacks in the array from deserialized ItemStack data
                    int slotIndex = 0;
                    for (ItemStack ignored : inventoryContents) {
                        inventoryContents[slotIndex] = deserializeItemStack(bukkitInputStream.readObject());
                        slotIndex++;
                    }

                    // Return the finished, serialized inventory contents
                    return inventoryContents;
                }
            } catch (IOException | ClassNotFoundException e) {
                BukkitHuskSync.getInstance().getLoggingAdapter().log(Level.SEVERE, "Failed to deserialize item stack data", e);
                throw new DataSerializationException("Failed to deserialize item stack data", e);
            }
        });
    }

    /**
     * Returns the serialized version of an {@link ItemStack} as a string to object Map
     *
     * @param item The {@link ItemStack} to serialize
     * @return The serialized {@link ItemStack}
     */
    @Nullable
    private static Map<String, Object> serializeItemStack(@Nullable ItemStack item) {
        return item != null ? item.serialize() : null;
    }

    /**
     * Returns the deserialized {@link ItemStack} from the Object read from the {@link BukkitObjectInputStream}
     *
     * @param serializedItemStack The serialized item stack; a String-Object map
     * @return The deserialized {@link ItemStack}
     */
    @SuppressWarnings("unchecked") // Ignore the "Unchecked cast" warning
    @Nullable
    private static ItemStack deserializeItemStack(@Nullable Object serializedItemStack) {
        return serializedItemStack != null ? ItemStack.deserialize((Map<String, Object>) serializedItemStack) : null;
    }

    /**
     * Returns a serialized array of {@link PotionEffect}s
     *
     * @param potionEffects The potion effect array
     * @return The serialized potion effects
     */
    public static CompletableFuture<String> serializePotionEffectArray(@NotNull PotionEffect[] potionEffects) throws DataSerializationException {
        return CompletableFuture.supplyAsync(() -> {
            // Return an empty string if there are no effects to serialize
            if (potionEffects.length == 0) {
                return "";
            }

            // Create an output stream that will be encoded into base 64
            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();

            try (BukkitObjectOutputStream bukkitOutputStream = new BukkitObjectOutputStream(byteOutputStream)) {
                // Define the length of the potion effect array to serialize
                bukkitOutputStream.writeInt(potionEffects.length);

                // Write each serialize each PotionEffect to the output stream
                for (PotionEffect potionEffect : potionEffects) {
                    bukkitOutputStream.writeObject(serializePotionEffect(potionEffect));
                }

                // Return encoded data, using the encoder from SnakeYaml to get a ByteArray conversion
                return Base64Coder.encodeLines(byteOutputStream.toByteArray());
            } catch (IOException e) {
                BukkitHuskSync.getInstance().getLoggingAdapter().log(Level.SEVERE, "Failed to serialize potion effect data", e);
                throw new DataSerializationException("Failed to serialize potion effect data", e);
            }
        });
    }

    /**
     * Returns an array of ItemStacks from serialized potion effect data
     *
     * @param potionEffectData The serialized {@link PotionEffect} array
     * @return The {@link PotionEffect}s
     */
    public static CompletableFuture<PotionEffect[]> deserializePotionEffectArray(@NotNull String potionEffectData) throws DataSerializationException {
        return CompletableFuture.supplyAsync(() -> {
            // Return empty array if there is no potion effect data (don't apply any effects to the player)
            if (potionEffectData.isEmpty()) {
                return new PotionEffect[0];
            }

            // Create a byte input stream to read the serialized data
            try (ByteArrayInputStream byteInputStream = new ByteArrayInputStream(Base64Coder.decodeLines(potionEffectData))) {
                try (BukkitObjectInputStream bukkitInputStream = new BukkitObjectInputStream(byteInputStream)) {
                    // Read the length of the Bukkit input stream and set the length of the array to this value
                    PotionEffect[] potionEffects = new PotionEffect[bukkitInputStream.readInt()];

                    // Set the potion effects in the array from deserialized PotionEffect data
                    int potionIndex = 0;
                    for (PotionEffect ignored : potionEffects) {
                        potionEffects[potionIndex] = deserializePotionEffect(bukkitInputStream.readObject());
                        potionIndex++;
                    }

                    // Return the finished, serialized potion effect array
                    return potionEffects;
                }
            } catch (IOException | ClassNotFoundException e) {
                BukkitHuskSync.getInstance().getLoggingAdapter().log(Level.SEVERE, "Failed to deserialize potion effect data", e);
                throw new DataSerializationException("Failed to deserialize potion effects", e);
            }
        });
    }

    /**
     * Returns the serialized version of an {@link ItemStack} as a string to object Map
     *
     * @param potionEffect The {@link ItemStack} to serialize
     * @return The serialized {@link ItemStack}
     */
    @Nullable
    private static Map<String, Object> serializePotionEffect(@Nullable PotionEffect potionEffect) {
        return potionEffect != null ? potionEffect.serialize() : null;
    }

    /**
     * Returns the deserialized {@link PotionEffect} from the Object read from the {@link BukkitObjectInputStream}
     *
     * @param serializedPotionEffect The serialized potion effect; a String-Object map
     * @return The deserialized {@link PotionEffect}
     */
    @SuppressWarnings("unchecked") // Ignore the "Unchecked cast" warning
    @Nullable
    private static PotionEffect deserializePotionEffect(@Nullable Object serializedPotionEffect) {
        return serializedPotionEffect != null ? new PotionEffect((Map<String, Object>) serializedPotionEffect) : null;
    }


}
