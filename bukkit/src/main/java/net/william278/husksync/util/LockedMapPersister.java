package net.william278.husksync.util;

import de.tr7zw.changeme.nbtapi.NBT;
import net.william278.husksync.HuskSync;
import net.william278.mapdataapi.MapData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Stream;

public interface LockedMapPersister {

    String MAP_DATA_KEY = "husksync:locked_map_data";

    @NotNull
    default ItemStack[] persistLockedMaps(@NotNull ItemStack[] items) {
        if (!getPlugin().getSettings().doPersistLockedMaps()) {
            return items;
        }

        return getMapStream(items)
                .filter(map -> {
                    final MapMeta meta = ((MapMeta) Objects.requireNonNull(
                            map.getItemMeta(), "Missing map meta"));
                    return meta.hasMapView() && Objects.requireNonNull(
                            meta.getMapView(), "Missing map view").isLocked();
                })
                .map(this::persistMapView)
                .toList().toArray(new ItemStack[0]);
    }

    @NotNull
    default ItemStack[] setMapViews(@NotNull ItemStack[] items) {
        if (!getPlugin().getSettings().doPersistLockedMaps()) {
            return items;
        }

        return getMapStream(items)
                .map(this::applyMapView)
                .toList().toArray(new ItemStack[0]);
    }

    private Stream<ItemStack> getMapStream(@NotNull ItemStack[] items) {
        return Arrays.stream(items)
                .filter(Objects::nonNull)
                .filter(stack -> stack.getType() == Material.FILLED_MAP)
                .filter(ItemStack::hasItemMeta);
    }

    @NotNull
    private ItemStack persistMapView(@NotNull ItemStack map) {
        final MapMeta meta = Objects.requireNonNull((MapMeta) map.getItemMeta());
        final MapView view = Objects.requireNonNull(meta.getMapView());
        if (view.getWorld() == null) {
            return map;
        }

        final int mapId = view.getId();
        try {
            final MapData data = MapData.getFromFile(view.getWorld().getWorldFolder(), mapId);
            NBT.modify(map, item -> {
                getPlugin().debug("Serializing locked map data to NBT...");
                item.setByteArray(MAP_DATA_KEY, data.toBytes());
            });
        } catch (Throwable e) {
            getPlugin().log(Level.WARNING, String.format("Failed to serialize locked map data (Map ID: %s)", mapId));
        }
        return map;
    }

    @NotNull
    private ItemStack applyMapView(@NotNull ItemStack map) {
        final MapMeta meta = Objects.requireNonNull((MapMeta) map.getItemMeta());
        NBT.get(map, nbt -> {
            if (!nbt.hasTag(MAP_DATA_KEY)) {
                return nbt;
            }

            // Read the data
            final MapData mapData;
            try {
                getPlugin().debug("Deserializing map data from NBT...");
                mapData = MapData.fromByteArray(nbt.getByteArray(MAP_DATA_KEY));
            } catch (Throwable e) {
                getPlugin().log(Level.WARNING, "Failed to deserialize map data from NBT", e);
                return nbt;
            }

            // Add a renderer to the map with the data
            setMapRenderer(meta, mapData);
            map.setItemMeta(meta);
            return nbt;
        });
        return map;
    }

    private void setMapRenderer(@NotNull MapMeta mapMeta, @NotNull MapData mapData) {
        final MapView view = Bukkit.createMap(Bukkit.getWorlds().get(0));
        view.getRenderers().clear();

        // Create a new map view renderer with the map data color at each pixel
        view.addRenderer(new PersistentMapRenderer(mapData));
        view.setLocked(true);
        view.setScale(MapView.Scale.NORMAL);
        view.setTrackingPosition(false);
        view.setUnlimitedTracking(false);

        // Set the MapView
        mapMeta.setMapView(view);
    }

    /**
     * A {@link MapRenderer} that can be used to render persistently serialized {@link MapData} to a {@link MapView}
     */
    class PersistentMapRenderer extends MapRenderer {

        private final MapData mapData;

        private PersistentMapRenderer(@NotNull MapData mapData) {
            super(false);
            this.mapData = mapData;
        }

        @Override
        public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
            for (int i = 0; i < 128; i++) {
                for (int j = 0; j < 128; j++) {
                    // We set the pixels in this order to avoid the map being rendered upside down
                    canvas.setPixel(j, i, (byte) mapData.getColorAt(i, j));
                }
            }
        }
    }

    /**
     * A {@link MapCanvas} implementation used for pre-rendering maps to be converted into {@link MapData}
     */
    class LockedMapCanvas implements MapCanvas {

        private final MapView mapView;
        private final int[][] pixels = new int[128][128];
        private MapCursorCollection cursors;

        private LockedMapCanvas(@NotNull MapView mapView) {
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
            return MapData.fromPixels(pixels, getDimension(), (byte) 2);
        }
    }

    @ApiStatus.Internal
    @NotNull
    HuskSync getPlugin();

}
