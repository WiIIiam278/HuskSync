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

package net.william278.husksync.maps;

import com.google.common.collect.Lists;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import net.william278.husksync.BukkitHuskSync;
import net.william278.husksync.redis.RedisManager;
import net.william278.mapdataapi.MapBanner;
import net.william278.mapdataapi.MapData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;

public interface BukkitMapHandler {

    // The map used to store HuskSync data in ItemStack NBT
    String MAP_DATA_KEY = "husksync:persisted_locked_map";
    // Name of server the map originates from
    String MAP_ORIGIN_KEY = "origin";
    // Original map id
    String MAP_ID_KEY = "id";

    /**
     * Persist locked maps in an array of {@link ItemStack}s
     *
     * @param items            the array of {@link ItemStack}s to persist locked maps in
     * @param delegateRenderer the player to delegate the rendering of map pixel canvases to
     * @return the array of {@link ItemStack}s with locked maps persisted to serialized NBT
     */
    @NotNull
    default ItemStack[] persistLockedMaps(@NotNull ItemStack[] items, @NotNull Player delegateRenderer) {
        if (!getPlugin().getSettings().getSynchronization().isPersistLockedMaps()) {
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
    @Nullable
    default ItemStack @NotNull [] setMapViews(@Nullable ItemStack @NotNull [] items) {
        if (!getPlugin().getSettings().getSynchronization().isPersistLockedMaps()) {
            return items;
        }
        return forEachMap(items, this::applyMapView);
    }

    // Perform an operation on each map in an array of ItemStacks
    @NotNull
    private ItemStack[] forEachMap(ItemStack[] items, @NotNull Function<ItemStack, ItemStack> function) {
        for (int i = 0; i < items.length; i++) {
            final ItemStack item = items[i];
            if (item == null) {
                continue;
            }
            if (item.getType() == Material.FILLED_MAP && item.hasItemMeta()) {
                items[i] = function.apply(item);
            } else if (item.getItemMeta() instanceof BlockStateMeta b && b.getBlockState() instanceof Container box) {
                forEachMap(box.getInventory().getContents(), function);
                b.setBlockState(box);
                item.setItemMeta(b);
            } else if (item.getItemMeta() instanceof BundleMeta bundle) {
                bundle.setItems(List.of(forEachMap(bundle.getItems().toArray(ItemStack[]::new), function)));
                item.setItemMeta(bundle);
            }
        }
        return items;
    }

    @Blocking
    private void writeMapData(@NotNull String serverName, int mapId, MapData data) {
        final byte[] dataBytes = getPlugin().getDataAdapter().toBytes(new AdaptableMapData(data));
        getRedisManager().setMapData(serverName, mapId, dataBytes);
        getPlugin().getDatabase().saveMapData(serverName, mapId, dataBytes);
    }

    @Nullable
    @Blocking
    private Map.Entry<MapData, Boolean> readMapData(@NotNull String serverName, int mapId) {
        final Map.Entry<byte[], Boolean> readData = fetchMapData(serverName, mapId);
        if (readData == null) {
            return null;
        }
        return deserializeMapData(readData);
    }

    @Nullable
    @Blocking
    private Map.Entry<byte[], Boolean> fetchMapData(@NotNull String serverName, int mapId) {
        return fetchMapData(serverName, mapId, true);
    }

    @Nullable
    @Blocking
    private Map.Entry<byte[], Boolean> fetchMapData(@NotNull String serverName, int mapId, boolean doReverseLookup) {
        // Read from Redis cache
        final byte[] redisData = getRedisManager().getMapData(serverName, mapId);
        if (redisData != null) {
            return new AbstractMap.SimpleImmutableEntry<>(redisData, true);
        }

        // Read from database and set to Redis
        @Nullable Map.Entry<byte[], Boolean> databaseData = getPlugin().getDatabase().getMapData(serverName, mapId);
        if (databaseData != null) {
            getRedisManager().setMapData(serverName, mapId, databaseData.getKey());
            return databaseData;
        }

        // Otherwise, lookup a reverse map binding
        if (doReverseLookup) {
            return fetchReversedMapData(serverName, mapId);
        }
        return null;
    }

    @Nullable
    private Map.Entry<byte[], Boolean> fetchReversedMapData(@NotNull String serverName, int mapId) {
        // Lookup binding from Redis cache, then fetch data if found
        Map.Entry<String, Integer> binding = getRedisManager().getReversedMapBound(serverName, mapId);
        if (binding != null) {
            return fetchMapData(binding.getKey(), binding.getValue(), false);
        }

        // Lookup binding from database, then set to Redis & fetch data if found
        binding = getPlugin().getDatabase().getMapBinding(serverName, mapId);
        if (binding != null) {
            getRedisManager().bindMapIds(binding.getKey(), binding.getValue(), serverName, mapId);
            return fetchMapData(binding.getKey(), binding.getValue(), false);
        }
        return null;
    }

    @Nullable
    private Map.Entry<MapData, Boolean> deserializeMapData(@NotNull Map.Entry<byte[], Boolean> data) {
        try {
            return new AbstractMap.SimpleImmutableEntry<>(
                    getPlugin().getDataAdapter().fromBytes(data.getKey(), AdaptableMapData.class)
                            .getData(getPlugin().getDataVersion(getPlugin().getMinecraftVersion())),
                    data.getValue()
            );
        } catch (IOException e) {
            getPlugin().log(Level.WARNING, "Failed to deserialize map data", e);
            return null;
        }
    }

    // Get the bound map ID
    private int getBoundMapId(@NotNull String fromServerName, int fromMapId, @NotNull String toServerName) {
        // Get the map ID from Redis, if set
        final Optional<Integer> redisId = getRedisManager().getBoundMapId(fromServerName, fromMapId, toServerName);
        if (redisId.isPresent()) {
            return redisId.get();
        }

        // Get from the database; if found, set to Redis
        final int result = getPlugin().getDatabase().getBoundMapId(fromServerName, fromMapId, toServerName);
        if (result != -1) {
            getPlugin().getRedisManager().bindMapIds(fromServerName, fromMapId, toServerName, result);
        }
        return result;
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
            final int dataVersion = getPlugin().getDataVersion(getPlugin().getMinecraftVersion());
            final PersistentMapCanvas canvas = new PersistentMapCanvas(view, dataVersion);
            for (MapRenderer renderer : view.getRenderers()) {
                renderer.render(view, canvas, delegateRenderer);
                getPlugin().debug(String.format("Rendered locked map canvas to view (#%s)", view.getId()));
            }

            // Persist map data
            final ReadWriteNBT mapData = nbt.getOrCreateCompound(MAP_DATA_KEY);
            final String serverName = getPlugin().getServerName();
            mapData.setString(MAP_ORIGIN_KEY, serverName);
            mapData.setInteger(MAP_ID_KEY, meta.getMapId());
            if (readMapData(serverName, meta.getMapId()) == null) {
                writeMapData(serverName, meta.getMapId(), canvas.extractMapData());
            }
            getPlugin().debug(String.format("Saved data for locked map (#%s, server: %s)", view.getId(), serverName));
        });
        return map;
    }

    @SuppressWarnings("deprecation")
    @NotNull
    private ItemStack applyMapView(@NotNull ItemStack map) {
        final MapMeta meta = Objects.requireNonNull((MapMeta) map.getItemMeta());
        NBT.get(map, nbt -> {
            if (!nbt.hasTag(MAP_DATA_KEY)) {
                return;
            }
            final ReadableNBT mapData = nbt.getCompound(MAP_DATA_KEY);
            if (mapData == null) {
                return;
            }

            // Determine map ID
            final String originServerName = mapData.getString(MAP_ORIGIN_KEY);
            final String currentServerName = getPlugin().getServerName();
            final int originalMapId = mapData.getInteger(MAP_ID_KEY);
            int newId = currentServerName.equals(originServerName)
                    ? originalMapId : getBoundMapId(originServerName, originalMapId, currentServerName);
            if (newId != -1) {
                meta.setMapId(newId);
                map.setItemMeta(meta);
                getPlugin().debug(String.format("Map ID set to %s", newId));
                return;
            }

            // Read the pixel data and generate a map view otherwise
            getPlugin().debug("Deserializing map data from NBT and generating view...");
            final MapData canvasData = Objects.requireNonNull(readMapData(originServerName, originalMapId), "Pixel data null!").getKey();

            // Add a renderer to the map with the data and save to file
            final MapView view = generateRenderedMap(canvasData);
            meta.setMapView(view);
            map.setItemMeta(meta);

            // Bind in the database & Redis
            final int id = view.getId();
            getRedisManager().bindMapIds(originServerName, originalMapId, currentServerName, id);
            getPlugin().getDatabase().setMapBinding(originServerName, originalMapId, currentServerName, id);

            getPlugin().debug(String.format("Bound map to view (#%s) on server %s", id, currentServerName));
        });
        return map;
    }

    default void renderPersistedMap(@NotNull MapView view) {
        if (getMapView(view.getId()).isPresent()) {
            return;
        }

        @Nullable final Map.Entry<MapData, Boolean> data = readMapData(getPlugin().getServerName(), view.getId());
        if (data == null) {
            final World world = view.getWorld() == null ? getDefaultMapWorld() : view.getWorld();
            getPlugin().debug("Not rendering map: no data in DB for world %s, map #%s."
                    .formatted(world.getName(), view.getId()));
            return;
        }

        if (data.getValue()) {
            // from this server, doesn't need tweaking
            return;
        }

        final MapData canvasData = data.getKey();

        // Create a new map view renderer with the map data color at each pixel
        // use view.removeRenderer() to remove all this maps renderers
        view.getRenderers().forEach(view::removeRenderer);
        view.addRenderer(new PersistentMapRenderer(canvasData));
        view.setLocked(true);
        view.setScale(MapView.Scale.NORMAL);
        view.setTrackingPosition(false);
        view.setUnlimitedTracking(false);

        // Set the view to the map
        setMapView(view);
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
    @SuppressWarnings("deprecation")
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
            while (cursors.size() > 0) {
                cursors.removeCursor(cursors.getCursor(0));
            }

            canvasData.getBanners().forEach(banner -> cursors.addCursor(createBannerCursor(banner)));
            canvas.setCursors(cursors);
        }
    }

    @NotNull
    private static MapCursor createBannerCursor(@NotNull MapBanner banner) {
        return new MapCursor(
                (byte) banner.getPosition().getX(),
                (byte) banner.getPosition().getZ(),
                (byte) 8, // Always rotate banners upright
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
    @SuppressWarnings({"deprecation", "removal"})
    class PersistentMapCanvas implements MapCanvas {

        private static final String BANNER_PREFIX = "banner_";

        private final int mapDataVersion;
        private final MapView mapView;
        private final int[][] pixels = new int[128][128];
        private MapCursorCollection cursors;

        private PersistentMapCanvas(@NotNull MapView mapView, int mapDataVersion) {
            this.mapDataVersion = mapDataVersion;
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
        @Deprecated
        public void setPixel(int x, int y, byte color) {
            pixels[x][y] = color;
        }

        @Override
        @Deprecated
        public byte getPixel(int x, int y) {
            return (byte) pixels[x][y];
        }

        @Override
        @Deprecated
        public byte getBasePixel(int x, int y) {
            return (byte) pixels[x][y];
        }

        @Override
        public void setPixelColor(int x, int y, @Nullable Color color) {
            pixels[x][y] = color == null ? -1 : MapPalette.matchColor(color);
        }

        @Nullable
        @Override
        public Color getPixelColor(int x, int y) {
            return MapPalette.getColor((byte) pixels[x][y]);
        }

        @NotNull
        @Override
        public Color getBasePixelColor(int x, int y) {
            return MapPalette.getColor((byte) pixels[x][y]);
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
            final List<MapBanner> banners = Lists.newArrayList();
            for (int i = 0; i < getCursors().size(); i++) {
                final MapCursor cursor = getCursors().getCursor(i);
                //#if MC==12001
                //$$ final String type = cursor.getType().name().toLowerCase(Locale.ENGLISH);
                //#else
                final String type = cursor.getType().getKey().getKey();
                //#endif
                if (type.startsWith(BANNER_PREFIX)) {
                    banners.add(new MapBanner(
                            type.replaceAll(BANNER_PREFIX, ""),
                            cursor.getCaption() == null ? "" : cursor.getCaption(),
                            cursor.getX(),
                            mapView.getWorld() != null ? mapView.getWorld().getSeaLevel() : 128,
                            cursor.getY()
                    ));
                }

            }
            return MapData.fromPixels(mapDataVersion, pixels, getDimension(), (byte) 2, banners, List.of());
        }
    }

    @NotNull
    Map<Integer, MapView> getMapViews();

    @ApiStatus.Internal
    RedisManager getRedisManager();

    @ApiStatus.Internal
    @NotNull
    BukkitHuskSync getPlugin();

}
