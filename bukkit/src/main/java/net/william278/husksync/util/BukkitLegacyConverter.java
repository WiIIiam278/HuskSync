package net.william278.husksync.util;

import net.william278.husksync.HuskSync;
import net.william278.husksync.adapter.DataAdapter;
import net.william278.husksync.data.BukkitDataContainer;
import net.william278.husksync.data.DataContainer;
import net.william278.husksync.data.DataSnapshot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class BukkitLegacyConverter extends LegacyConverter {

    public BukkitLegacyConverter(@NotNull HuskSync plugin) {
        super(plugin);
    }

    @NotNull
    @Override
    public DataSnapshot.Packed convert(@NotNull byte[] data) throws DataAdapter.AdaptionException {
        final JSONObject object = new JSONObject(new String(data, StandardCharsets.UTF_8));
        final int version = object.getInt("format_version");
        if (version != 3) {
            throw new DataAdapter.AdaptionException(String.format(
                    "Unsupported legacy format version: %s. Please downgrade to an earlier version of HuskSync, " +
                            "perform a manual legacy migration, then attempt an upgrade again.", version
            ));
        }

        final Map<DataContainer.Type, DataContainer> containers = new LinkedHashMap<>(readStatusData(object));
        readInventory(object).ifPresent(i -> containers.put(DataContainer.Type.INVENTORY, i));
        readEnderChest(object).ifPresent(e -> containers.put(DataContainer.Type.ENDER_CHEST, e));
        readLocation(object).ifPresent(l -> containers.put(DataContainer.Type.LOCATION, l));
        readAdvancements(object).ifPresent(a -> containers.put(DataContainer.Type.ADVANCEMENTS, a));

        return DataSnapshot.create(plugin, containers, DataSnapshot.SaveCause.LEGACY_MIGRATION);
    }

    @NotNull
    private Map<DataContainer.Type, DataContainer> readStatusData(@NotNull JSONObject object) {
        if (!object.has("status_data")) {
            return Map.of();
        }

        final JSONObject status = object.getJSONObject("status_data");
        final HashMap<DataContainer.Type, DataContainer> containers = new HashMap<>();
        if (shouldImport(DataContainer.Type.HEALTH)) {
            containers.put(DataContainer.Type.HEALTH, BukkitDataContainer.Health.from(
                    status.getDouble("health"),
                    status.getDouble("max_health"),
                    status.getDouble("health_scale")
            ));
        }
        if (shouldImport(DataContainer.Type.FOOD)) {
            containers.put(DataContainer.Type.FOOD, BukkitDataContainer.Food.from(
                    status.getInt("hunger"),
                    status.getFloat("saturation"),
                    status.getFloat("saturation_exhaustion")
            ));
        }
        if (shouldImport(DataContainer.Type.EXPERIENCE)) {
            containers.put(DataContainer.Type.EXPERIENCE, BukkitDataContainer.Experience.from(
                    status.getInt("total_experience"),
                    status.getInt("experience_level"),
                    status.getFloat("experience_progress")
            ));
        }
        if (shouldImport(DataContainer.Type.GAME_MODE)) {
            containers.put(DataContainer.Type.GAME_MODE, BukkitDataContainer.GameMode.from(
                    status.getString("game_mode"),
                    status.getBoolean("is_flying"),
                    status.getBoolean("is_flying")
            ));
        }
        return containers;
    }

    @NotNull
    private Optional<DataContainer.Items> readInventory(@NotNull JSONObject object) {
        if (!object.has("inventory") || !shouldImport(DataContainer.Type.INVENTORY)) {
            return Optional.empty();
        }

        final JSONObject inventoryData = object.getJSONObject("inventory");
        return Optional.of(BukkitDataContainer.Items.Inventory.from(
                deserializeLegacyItemStacks(inventoryData.getString("serialized_items")), 0
        ));
    }

    @NotNull
    private Optional<DataContainer.Items> readEnderChest(@NotNull JSONObject object) {
        if (!object.has("ender_chest") || !shouldImport(DataContainer.Type.ENDER_CHEST)) {
            return Optional.empty();
        }

        final JSONObject inventoryData = object.getJSONObject("ender_chest");
        return Optional.of(BukkitDataContainer.Items.EnderChest.adapt(
                deserializeLegacyItemStacks(inventoryData.getString("serialized_items"))
        ));
    }

    @NotNull
    private Optional<DataContainer.Location> readLocation(@NotNull JSONObject object) {
        if (!object.has("location") || !shouldImport(DataContainer.Type.LOCATION)) {
            return Optional.empty();
        }

        final JSONObject locationData = object.getJSONObject("location");
        return Optional.of(BukkitDataContainer.Location.from(
                locationData.getDouble("x"),
                locationData.getDouble("y"),
                locationData.getDouble("z"),
                locationData.getFloat("yaw"),
                locationData.getFloat("pitch"),
                new DataContainer.Location.World(
                        locationData.getString("world_name"),
                        UUID.fromString(locationData.getString("world_uuid")),
                        locationData.getString("world_environment")
                )
        ));
    }

    //todo check against actual v3 data format
    @NotNull
    private Optional<DataContainer.Advancements> readAdvancements(@NotNull JSONObject object) {
        if (!object.has("advancements") || !shouldImport(DataContainer.Type.ADVANCEMENTS)) {
            return Optional.empty();
        }

        final JSONArray advancements = object.getJSONArray("advancements");
        final List<DataContainer.Advancements.Advancement> converted = new ArrayList<>();
        advancements.iterator().forEachRemaining(o -> {
            final JSONObject advancement = (JSONObject) JSONObject.wrap(o);
            final String key = advancement.getString("key");

            final JSONObject criteria = advancement.getJSONObject("completed_criteria");
            final Map<String, Date> criteriaMap = new LinkedHashMap<>();
            criteria.keys().forEachRemaining(criteriaKey -> criteriaMap.put(
                    criteriaKey, parseDate(criteria.getString(criteriaKey)))
            );
            converted.add(DataContainer.Advancements.Advancement.adapt(key, criteriaMap));
        });

        return Optional.of(BukkitDataContainer.Advancements.from(converted));
    }

    @NotNull
    private ItemStack[] deserializeLegacyItemStacks(@NotNull String items) {
        // Return empty array if there is no inventory data (set the player as having an empty inventory)
        if (items.isEmpty()) {
            return new ItemStack[0];
        }

        // Create a byte input stream to read the serialized data
        try (ByteArrayInputStream byteInputStream = new ByteArrayInputStream(Base64Coder.decodeLines(items))) {
            try (BukkitObjectInputStream bukkitInputStream = new BukkitObjectInputStream(byteInputStream)) {
                return new ItemStack[bukkitInputStream.readInt()];
            }
        } catch (IOException e) {
            throw new DataAdapter.AdaptionException("Failed to deserialize legacy item stack data", e);
        }
    }

    private boolean shouldImport(@NotNull DataContainer.Type type) {
        return plugin.getSettings().getSynchronizationFeature(type);
    }

    @NotNull
    private Date parseDate(@NotNull String dateString) {
        try {
            return new SimpleDateFormat().parse(dateString);
        } catch (ParseException e) {
            return new Date();
        }
    }

}
