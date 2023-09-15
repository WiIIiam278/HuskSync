/*
 * This file is part of HuskSync, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.husksync.util;

import net.william278.husksync.HuskSync;
import net.william278.husksync.adapter.DataAdapter;
import net.william278.husksync.data.BukkitData;
import net.william278.husksync.data.Data;
import net.william278.husksync.data.DataSnapshot;
import net.william278.husksync.data.Identifier;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
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
                    "Unsupported legacy data format version: %s. Please downgrade to an earlier version of HuskSync, " +
                            "perform a manual legacy migration, then attempt to upgrade again.", version
            ));
        }

        // Read legacy data from the JSON object
        final Map<Identifier, Data> containers = new LinkedHashMap<>(readStatusData(object));
        readInventory(object).ifPresent(i -> containers.put(Identifier.INVENTORY, i));
        readEnderChest(object).ifPresent(e -> containers.put(Identifier.ENDER_CHEST, e));
        readLocation(object).ifPresent(l -> containers.put(Identifier.LOCATION, l));
        readAdvancements(object).ifPresent(a -> containers.put(Identifier.ADVANCEMENTS, a));
        readStatistics(object).ifPresent(s -> containers.put(Identifier.STATISTICS, s));

        return DataSnapshot.create(plugin, containers, DataSnapshot.SaveCause.LEGACY_MIGRATION);
    }

    @NotNull
    private Map<Identifier, Data> readStatusData(@NotNull JSONObject object) {
        if (!object.has("status_data")) {
            return Map.of();
        }

        final JSONObject status = object.getJSONObject("status_data");
        final HashMap<Identifier, Data> containers = new HashMap<>();
        if (shouldImport(Identifier.HEALTH)) {
            containers.put(Identifier.HEALTH, BukkitData.Health.from(
                    status.getDouble("health"),
                    status.getDouble("max_health"),
                    status.getDouble("health_scale")
            ));
        }
        if (shouldImport(Identifier.HUNGER)) {
            containers.put(Identifier.HUNGER, BukkitData.Hunger.from(
                    status.getInt("hunger"),
                    status.getFloat("saturation"),
                    status.getFloat("saturation_exhaustion")
            ));
        }
        if (shouldImport(Identifier.EXPERIENCE)) {
            containers.put(Identifier.EXPERIENCE, BukkitData.Experience.from(
                    status.getInt("total_experience"),
                    status.getInt("experience_level"),
                    status.getFloat("experience_progress")
            ));
        }
        if (shouldImport(Identifier.GAME_MODE)) {
            containers.put(Identifier.GAME_MODE, BukkitData.GameMode.from(
                    status.getString("game_mode"),
                    status.getBoolean("is_flying"),
                    status.getBoolean("is_flying")
            ));
        }
        return containers;
    }

    @NotNull
    private Optional<Data.Items> readInventory(@NotNull JSONObject object) {
        if (!object.has("inventory") || !shouldImport(Identifier.INVENTORY)) {
            return Optional.empty();
        }

        final JSONObject inventoryData = object.getJSONObject("inventory");
        return Optional.of(BukkitData.Items.Inventory.from(
                deserializeLegacyItemStacks(inventoryData.getString("serialized_items")), 0
        ));
    }

    @NotNull
    private Optional<Data.Items> readEnderChest(@NotNull JSONObject object) {
        if (!object.has("ender_chest") || !shouldImport(Identifier.ENDER_CHEST)) {
            return Optional.empty();
        }

        final JSONObject inventoryData = object.getJSONObject("ender_chest");
        return Optional.of(BukkitData.Items.EnderChest.adapt(
                deserializeLegacyItemStacks(inventoryData.getString("serialized_items"))
        ));
    }

    @NotNull
    private Optional<Data.Location> readLocation(@NotNull JSONObject object) {
        if (!object.has("location") || !shouldImport(Identifier.LOCATION)) {
            return Optional.empty();
        }

        final JSONObject locationData = object.getJSONObject("location");
        return Optional.of(BukkitData.Location.from(
                locationData.getDouble("x"),
                locationData.getDouble("y"),
                locationData.getDouble("z"),
                locationData.getFloat("yaw"),
                locationData.getFloat("pitch"),
                new Data.Location.World(
                        locationData.getString("world_name"),
                        UUID.fromString(locationData.getString("world_uuid")),
                        locationData.getString("world_environment")
                )
        ));
    }

    @NotNull
    private Optional<Data.Advancements> readAdvancements(@NotNull JSONObject object) {
        if (!object.has("advancements") || !shouldImport(Identifier.ADVANCEMENTS)) {
            return Optional.empty();
        }

        final JSONArray advancements = object.getJSONArray("advancements");
        final List<Data.Advancements.Advancement> converted = new ArrayList<>();
        advancements.iterator().forEachRemaining(o -> {
            final JSONObject advancement = (JSONObject) JSONObject.wrap(o);
            final String key = advancement.getString("key");

            final JSONObject criteria = advancement.getJSONObject("completed_criteria");
            final Map<String, Date> criteriaMap = new LinkedHashMap<>();
            criteria.keys().forEachRemaining(criteriaKey -> criteriaMap.put(
                    criteriaKey, parseDate(criteria.getString(criteriaKey)))
            );
            converted.add(Data.Advancements.Advancement.adapt(key, criteriaMap));
        });

        return Optional.of(BukkitData.Advancements.from(converted));
    }

    @NotNull
    private Optional<Data.Statistics> readStatistics(@NotNull JSONObject object) {
        if (!object.has("statistics") || !shouldImport(Identifier.ADVANCEMENTS)) {
            return Optional.empty();
        }

        final JSONObject stats = object.getJSONObject("statistics");
        return Optional.of(readStatisticMaps(
                stats.getJSONObject("untyped_statistics"),
                stats.getJSONObject("block_statistics"),
                stats.getJSONObject("item_statistics"),
                stats.getJSONObject("entity_statistics")
        ));
    }

    @NotNull
    private BukkitData.Statistics readStatisticMaps(@NotNull JSONObject untyped, @NotNull JSONObject blocks,
                                                    @NotNull JSONObject items, @NotNull JSONObject entities) {
        final Map<Statistic, Integer> genericStats = new HashMap<>();
        untyped.keys().forEachRemaining(stat -> genericStats.put(Statistic.valueOf(stat), untyped.getInt(stat)));

        final Map<Statistic, Map<Material, Integer>> blockStats = new HashMap<>();
        blocks.keys().forEachRemaining(stat -> {
            final JSONObject blockStat = blocks.getJSONObject(stat);
            final Map<Material, Integer> blockMap = new HashMap<>();
            blockStat.keys().forEachRemaining(block -> blockMap.put(Material.valueOf(block), blockStat.getInt(block)));
            blockStats.put(Statistic.valueOf(stat), blockMap);
        });

        final Map<Statistic, Map<Material, Integer>> itemStats = new HashMap<>();
        items.keys().forEachRemaining(stat -> {
            final JSONObject itemStat = items.getJSONObject(stat);
            final Map<Material, Integer> itemMap = new HashMap<>();
            itemStat.keys().forEachRemaining(item -> itemMap.put(Material.valueOf(item), itemStat.getInt(item)));
            itemStats.put(Statistic.valueOf(stat), itemMap);
        });

        final Map<Statistic, Map<EntityType, Integer>> entityStats = new HashMap<>();
        entities.keys().forEachRemaining(stat -> {
            final JSONObject entityStat = entities.getJSONObject(stat);
            final Map<EntityType, Integer> entityMap = new HashMap<>();
            entityStat.keys().forEachRemaining(entity -> entityMap.put(EntityType.valueOf(entity), entityStat.getInt(entity)));
            entityStats.put(Statistic.valueOf(stat), entityMap);
        });

        return BukkitData.Statistics.from(genericStats, blockStats, itemStats, entityStats);
    }

    @NotNull
    public ItemStack[] deserializeLegacyItemStacks(@NotNull String items) {
        // Return an empty array if there is no inventory data (set the player as having an empty inventory)
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

    private boolean shouldImport(@NotNull Identifier type) {
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
