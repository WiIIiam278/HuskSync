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

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.tag.CompoundTag;
import net.william278.husksync.HuskSync;
import net.william278.mapdataapi.MapBanner;
import net.william278.mapdataapi.MapData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;

public interface BukkitMapPersister {

    // The map used to store HuskSync data in ItemStack NBT
    String MAP_DATA_KEY = "husksync:persisted_locked_map";
    // The key used to store the serialized map data in NBT
    String MAP_PIXEL_DATA_KEY = "canvas_data";
    // The key used to store the map of World UIDs to MapView IDs in NBT
    String MAP_VIEW_ID_MAPPINGS_KEY = "id_mappings";

    /**
     * Persist locked maps in an array of {@link ItemStack}s
     *
     * @param items            the array of {@link ItemStack}s to persist locked maps in
     * @param delegateRenderer the player to delegate the rendering of map pixel canvases to
     * @return the array of {@link ItemStack}s with locked maps persisted to serialized NBT
     */
    @NotNull
    default ItemStack[] persistLockedMaps(@NotNull ItemStack[] items, @NotNull Player delegateRenderer) {
        if (!getPlugin().getSettings().doPersistLockedMaps()) {
            return items;
        }
        return forEachMap(items, map -> this.persistMapView(map, delegateRenderer));
    }

    /**
     * Apply persisted locked maps to an array of {@link ItemStack}s
     *
     * @param items the array of {@link ItemStack}s to apply persisted locked maps to
     * @return the array of {@link ItemStack}s with persisted locked maps applied
     */
    @NotNull
    default ItemStack[] setMapViews(@NotNull ItemStack[] items) {
        if (!getPlugin().getSettings().doPersistLockedMaps()) {
            return items;
        }
        return forEachMap(items, this::applyMapView);
    }

    // Perform an operation on each map in an array of ItemStacks
    @NotNull
    private ItemStack[] forEachMap(@NotNull ItemStack[] items, @NotNull Function<ItemStack, ItemStack> function) {
        for (int i = 0; i < items.length; i++) {
            final ItemStack item = items[i];
            if (item == null) {
                continue;
            }
            if (item.getType() == Material.FILLED_MAP && item.hasItemMeta()) {
                items[i] = function.apply(item);
            }
        }
        return items;
    }

    @NotNull
    private ItemStack persistMapView(@NotNull ItemStack map, @NotNull Player delegateRenderer) {
        final MapMeta meta = Objects.requireNonNull((MapMeta) map.getItemMeta());
        if (!meta.hasMapView()) {
            return map;
        }
        final MapView view = meta.getMapView();
        if (view == null || view.getWorld() == null || !view.isLocked() || view.isVirtual()) {
            return map;
        }

        NBT.modify(map, nbt -> {
            // Don't save the map's data twice
            if (nbt.hasTag(MAP_DATA_KEY)) {
                return;
            }

            // Render the map
            final PersistentMapCanvas canvas = new PersistentMapCanvas(view);
            for (MapRenderer renderer : view.getRenderers()) {
                renderer.render(view, canvas, delegateRenderer);
                getPlugin().debug(String.format("Rendered locked map canvas to view (#%s)", view.getId()));
            }

            // Persist map data
            final ReadWriteNBT mapData = nbt.getOrCreateCompound(MAP_DATA_KEY);
            final String worldUid = view.getWorld().getUID().toString();
            mapData.setByteArray(MAP_PIXEL_DATA_KEY, canvas.extractMapData().toBytes());
            nbt.getOrCreateCompound(MAP_VIEW_ID_MAPPINGS_KEY).setInteger(worldUid, view.getId());
            getPlugin().debug(String.format("Saved data for locked map (#%s, UID: %s)", view.getId(), worldUid));
        });
        return map;
    }

    @NotNull
    private ItemStack applyMapView(@NotNull ItemStack map) {
        final MapMeta meta = Objects.requireNonNull((MapMeta) map.getItemMeta());
        NBT.get(map, nbt -> {
            if (!nbt.hasTag(MAP_DATA_KEY)) {
                return;
            }
            final ReadableNBT mapData = nbt.getCompound(MAP_DATA_KEY);
            final ReadableNBT mapIds = nbt.getCompound(MAP_VIEW_ID_MAPPINGS_KEY);
            if (mapData == null || mapIds == null) {
                return;
            }

            // Search for an existing map view
            Optional<String> world = Optional.empty();
            for (String worldUid : mapIds.getKeys()) {
                world = Bukkit.getWorlds().stream()
                        .map(w -> w.getUID().toString()).filter(u -> u.equals(worldUid))
                        .findFirst();
                if (world.isPresent()) {
                    break;
                }
            }
            if (world.isPresent()) {
                final String uid = world.get();
                final Optional<MapView> existingView = this.getMapView(mapIds.getInteger(uid));
                if (existingView.isPresent()) {
                    final MapView view = existingView.get();
                    view.setLocked(true);
                    meta.setMapView(view);
                    map.setItemMeta(meta);
                    getPlugin().debug(String.format("View exists (#%s); updated map (UID: %s)", view.getId(), uid));
                    return;
                }
            }

            // Read the pixel data and generate a map view otherwise
            final MapData canvasData;
            try {
                getPlugin().debug("Deserializing map data from NBT and generating view...");
                canvasData = MapData.fromByteArray(Objects.requireNonNull(mapData.getByteArray(MAP_PIXEL_DATA_KEY),
                        "Map pixel data is null"));
            } catch (Throwable e) {
                getPlugin().log(Level.WARNING, "Failed to deserialize map data from NBT", e);
                return;
            }

            // Add a renderer to the map with the data and save to file
            final MapView view = generateRenderedMap(canvasData);
            final String worldUid = getDefaultMapWorld().getUID().toString();
            meta.setMapView(view);
            map.setItemMeta(meta);
            saveMapToFile(canvasData, view.getId());

            // Set the map view ID in NBT
            NBT.modify(map, editable -> {
                Objects.requireNonNull(editable.getCompound(MAP_VIEW_ID_MAPPINGS_KEY),
                                "Map view ID mappings compound is null")
                        .setInteger(worldUid, view.getId());
            });
            getPlugin().debug(String.format("Generated view (#%s) and updated map (UID: %s)", view.getId(), worldUid));
        });
        return map;
    }

    default void renderMapFromFile(@NotNull MapView view) {
        final File mapFile = new File(getMapCacheFolder(), view.getId() + ".dat");
        if (!mapFile.exists()) {
            return;
        }

        final MapData canvasData;
        try {
            canvasData = MapData.fromNbt(mapFile);
        } catch (Throwable e) {
            getPlugin().log(Level.WARNING, "Failed to deserialize map data from file", e);
            return;
        }

        // Create a new map view renderer with the map data color at each pixel
        view.getRenderers().clear();
        view.addRenderer(new PersistentMapRenderer(canvasData));
        view.setLocked(true);
        view.setScale(MapView.Scale.NORMAL);
        view.setTrackingPosition(false);
        view.setUnlimitedTracking(false);

        // Set the view to the map
        setMapView(view);
    }

    default void saveMapToFile(@NotNull MapData data, int id) {
        getPlugin().runAsync(() -> {
            final File mapFile = new File(getMapCacheFolder(), id + ".dat");
            if (mapFile.exists()) {
                return;
            }

            try {
                final CompoundTag rootTag = new CompoundTag();
                rootTag.put("data", data.toNBT().getTag());
                NBTUtil.write(rootTag, mapFile);
            } catch (Throwable e) {
                getPlugin().log(Level.WARNING, "Failed to serialize map data to file", e);
            }
        });
    }

    @NotNull
    private File getMapCacheFolder() {
        final File mapCache = new File(getPlugin().getDataFolder(), "maps");
        if (!mapCache.exists() && !mapCache.mkdirs()) {
            getPlugin().log(Level.WARNING, "Failed to create maps folder");
        }
        return mapCache;
    }

    // Sets the renderer of a map, and returns the generated MapView
    @NotNull
    private MapView generateRenderedMap(@NotNull MapData canvasData) {
        final MapView view = Bukkit.createMap(getDefaultMapWorld());
        view.getRenderers().clear();

        // Create a new map view renderer with the map data color at each pixel
        view.addRenderer(new PersistentMapRenderer(canvasData));
        view.setLocked(true);
        view.setScale(MapView.Scale.NORMAL);
        view.setTrackingPosition(false);
        view.setUnlimitedTracking(false);

        // Set the view to the map and return it
        setMapView(view);
        return view;
    }

    @NotNull
    private static World getDefaultMapWorld() {
        final World world = Bukkit.getWorlds().get(0);
        if (world == null) {
            throw new IllegalStateException("No worlds are loaded on the server!");
        }
        return world;
    }

    default Optional<MapView> getMapView(int id) {
        return getMapViews().containsKey(id) ? Optional.of(getMapViews().get(id)) : Optional.empty();
    }

    default void setMapView(@NotNull MapView view) {
        getMapViews().put(view.getId(), view);
    }

    /**
     * A {@link MapRenderer} that can be used to render persistently serialized {@link MapData} to a {@link MapView}
     */
    class PersistentMapRenderer extends MapRenderer {

        private final MapData canvasData;

        private PersistentMapRenderer(@NotNull MapData canvasData) {
            super(false);
            this.canvasData = canvasData;
        }

        @Override
        public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
            // We set the pixels in this order to avoid the map being rendered upside down
            for (int i = 0; i < 128; i++) {
                for (int j = 0; j < 128; j++) {
                    canvas.setPixel(j, i, (byte) canvasData.getColorAt(i, j));
                }
            }

            // Set the map banners and markers
            final MapCursorCollection cursors = canvas.getCursors();
            canvasData.getBanners().forEach(banner -> cursors.addCursor(createBannerCursor(banner)));
            canvas.setCursors(cursors);
        }
    }

    @NotNull
    private static MapCursor createBannerCursor(@NotNull MapBanner banner) {
        return new MapCursor(
                (byte) banner.getPosition().getX(),
                (byte) banner.getPosition().getZ(),
                (byte) 0,
                switch (banner.getColor().toLowerCase(Locale.ENGLISH)) {
                    case "white" -> MapCursor.Type.BANNER_WHITE;
                    case "orange" -> MapCursor.Type.BANNER_ORANGE;
                    case "magenta" -> MapCursor.Type.BANNER_MAGENTA;
                    case "light_blue" -> MapCursor.Type.BANNER_LIGHT_BLUE;
                    case "yellow" -> MapCursor.Type.BANNER_YELLOW;
                    case "lime" -> MapCursor.Type.BANNER_LIME;
                    case "pink" -> MapCursor.Type.BANNER_PINK;
                    case "gray" -> MapCursor.Type.BANNER_GRAY;
                    case "light_gray" -> MapCursor.Type.BANNER_LIGHT_GRAY;
                    case "cyan" -> MapCursor.Type.BANNER_CYAN;
                    case "purple" -> MapCursor.Type.BANNER_PURPLE;
                    case "blue" -> MapCursor.Type.BANNER_BLUE;
                    case "brown" -> MapCursor.Type.BANNER_BROWN;
                    case "green" -> MapCursor.Type.BANNER_GREEN;
                    case "red" -> MapCursor.Type.BANNER_RED;
                    default -> MapCursor.Type.BANNER_BLACK;
                },
                true,
                banner.getText().isEmpty() ? null : banner.getText()
        );
    }

    /**
     * A {@link MapCanvas} implementation used for pre-rendering maps to be converted into {@link MapData}
     */
    class PersistentMapCanvas implements MapCanvas {

        private final MapView mapView;
        private final int[][] pixels = new int[128][128];
        private MapCursorCollection cursors;

        private PersistentMapCanvas(@NotNull MapView mapView) {
            this.mapView = mapView;
        }

        @NotNull
        @Override
        public MapView getMapView() {
            return mapView;
        }

        @NotNull
        @Override
        public MapCursorCollection getCursors() {
            return cursors == null ? (cursors = new MapCursorCollection()) : cursors;
        }

        @Override
        public void setCursors(@NotNull MapCursorCollection cursors) {
            this.cursors = cursors;
        }

        @Override
        public void setPixel(int x, int y, byte color) {
            pixels[x][y] = color;
        }

        @Override
        public byte getPixel(int x, int y) {
            return (byte) pixels[x][y];
        }

        @Override
        public byte getBasePixel(int x, int y) {
            return getPixel(x, y);
        }

        @Override
        public void drawImage(int x, int y, @NotNull Image image) {
            // Not implemented
        }

        @Override
        public void drawText(int x, int y, @NotNull MapFont font, @NotNull String text) {
            // Not implemented
        }

        @NotNull
        private String getDimension() {
            return mapView.getWorld() != null ? switch (mapView.getWorld().getEnvironment()) {
                case NETHER -> "minecraft:the_nether";
                case THE_END -> "minecraft:the_end";
                default -> "minecraft:overworld";
            } : "minecraft:overworld";
        }

        /**
         * Extract the map data from the canvas. Must be rendered first
         *
         * @return the extracted map data
         */
        @NotNull
        private MapData extractMapData() {
            final List<MapBanner> banners = new ArrayList<>();
            for (int i = 0; i < getCursors().size(); i++) {
                final MapCursor cursor = getCursors().getCursor(i);
                final String type = cursor.getType().name().toLowerCase(Locale.ENGLISH);
                if (type.startsWith("banner_")) {
                    banners.add(new MapBanner(
                            type.replaceAll("banner_", ""),
                            cursor.getCaption() == null ? "" : cursor.getCaption(),
                            cursor.getX(),
                            mapView.getWorld() != null ? mapView.getWorld().getSeaLevel() : 128,
                            cursor.getY()
                    ));
                }
            }
            return MapData.fromPixels(pixels, getDimension(), (byte) 2, banners, List.of());
        }
    }

    @NotNull
    Map<Integer, MapView> getMapViews();

    @ApiStatus.Internal
    @NotNull
    HuskSync getPlugin();

}
