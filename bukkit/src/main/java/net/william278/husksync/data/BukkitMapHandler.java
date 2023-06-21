/*
 * This file is part of HuskSync by William278. Do not redistribute!
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  All rights reserved.
 *
 *  This source code is provided as reference to licensed individuals that have purchased the HuskSync
 *  plugin once from any of the official sources it is provided. The availability of this code does
 *  not grant you the rights to modify, re-distribute, compile or redistribute this source code or
 *  "plugin" outside this intended purpose. This license does not cover libraries developed by third
 *  parties that are utilised in the plugin.
 */

package net.william278.husksync.data;

import net.william278.husksync.BukkitHuskSync;
import net.william278.mapdataapi.MapData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.*;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * Handles the persistence of {@link MapData} into {@link ItemStack}s.
 */
public class BukkitMapHandler {

    private static final BukkitHuskSync plugin = BukkitHuskSync.getInstance();
    private static final NamespacedKey MAP_DATA_KEY = new NamespacedKey(plugin, "map_data");

    /**
     * Get the {@link MapData} from the given {@link ItemStack} and persist it in its' data container
     *
     * @param itemStack the {@link ItemStack} to get the {@link MapData} from
     */
    @SuppressWarnings("ConstantConditions")
    public static void persistMapData(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != Material.FILLED_MAP) {
            return;
        }
        final MapMeta mapMeta = (MapMeta) itemStack.getItemMeta();
        if (mapMeta == null || !mapMeta.hasMapView()) {
            return;
        }

        // Get the map view from the map
        final MapView mapView;
        try {
            mapView = Bukkit.getScheduler().callSyncMethod(plugin, mapMeta::getMapView).get();
            if (mapView == null || !mapView.isLocked() || mapView.isVirtual()) {
                return;
            }
        } catch (InterruptedException | ExecutionException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save map data for a player", e);
            return;
        }

        // Get the map data
        plugin.debug("Rendering map view onto canvas for locked map");
        final LockedMapCanvas canvas = new LockedMapCanvas(mapView);
        for (MapRenderer renderer : mapView.getRenderers()) {
            renderer.render(mapView, canvas, Bukkit.getServer()
                    .getOnlinePlayers().stream()
                    .findAny()
                    .orElse(null));
        }

        // Save the extracted rendered map data
        plugin.debug("Saving pixel canvas data for locked map");
        if (!mapMeta.getPersistentDataContainer().has(MAP_DATA_KEY, PersistentDataType.BYTE_ARRAY)) {
            mapMeta.getPersistentDataContainer().set(MAP_DATA_KEY, PersistentDataType.BYTE_ARRAY,
                    canvas.extractMapData().toBytes());
            itemStack.setItemMeta(mapMeta);
        }
    }

    /**
     * Set the map data of the given {@link ItemStack} to the given {@link MapData}, applying a map view to the item stack
     *
     * @param itemStack the {@link ItemStack} to set the map data of
     */
    public static void setMapRenderer(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != Material.FILLED_MAP) {
            return;
        }

        final MapMeta mapMeta = (MapMeta) itemStack.getItemMeta();
        if (mapMeta == null) {
            return;
        }

        if (!itemStack.getItemMeta().getPersistentDataContainer().has(MAP_DATA_KEY, PersistentDataType.BYTE_ARRAY)) {
            return;
        }

        try {
            final byte[] serializedData = itemStack.getItemMeta().getPersistentDataContainer()
                    .get(MAP_DATA_KEY, PersistentDataType.BYTE_ARRAY);
            final MapData mapData = MapData.fromByteArray(Objects.requireNonNull(serializedData));
            plugin.debug("Setting deserialized map data for an item stack");

            // Create a new map view renderer with the map data color at each pixel
            final MapView view = Bukkit.createMap(Bukkit.getWorlds().get(0));
            view.getRenderers().clear();
            view.addRenderer(new PersistentMapRenderer(mapData));
            view.setLocked(true);
            view.setScale(MapView.Scale.NORMAL);
            view.setTrackingPosition(false);
            view.setUnlimitedTracking(false);
            mapMeta.setMapView(view);
            itemStack.setItemMeta(mapMeta);
            plugin.debug("Successfully applied renderer to map item stack");
        } catch (IOException | NullPointerException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to deserialize map data for a player", e);
        }
    }

    /**
     * A {@link MapRenderer} that can be used to render persistently serialized {@link MapData} to a {@link MapView}
     */
    public static class PersistentMapRenderer extends MapRenderer {

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
    public static class LockedMapCanvas implements MapCanvas {

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
            return mapView.getWorld() == null ? "minecraft:overworld"
                    : switch (mapView.getWorld().getEnvironment()) {
                case NETHER -> "minecraft:the_nether";
                case THE_END -> "minecraft:the_end";
                default -> "minecraft:overworld";
            };
        }

        /**
         * Extract the map data from the canvas. Must be rendered first
         * @return the extracted map data
         */
        @NotNull
        private MapData extractMapData() {
            return MapData.fromPixels(pixels, getDimension(), (byte) 2);
        }
    }
}
