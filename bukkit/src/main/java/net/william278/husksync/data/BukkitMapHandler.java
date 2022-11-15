package net.william278.husksync.data;

import net.william278.husksync.BukkitHuskSync;
import net.william278.mapdataapi.MapData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;
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
    public static void persistMapData(@NotNull ItemStack itemStack) {
        if (itemStack.getType() != Material.FILLED_MAP) {
            return;
        }
        final MapMeta mapMeta = (MapMeta) itemStack.getItemMeta();
        if (mapMeta == null || !mapMeta.hasMapView()) {
            return;
        }

        // Get the map view
        final MapView mapView = mapMeta.getMapView();
        if (mapView == null || !mapView.isLocked() || mapView.isVirtual()) {
            return;
        }
        final int mapId = mapView.getId();
        if (mapId < 0) {
            return;
        }

        // Get the map data
        try {
            if (!itemStack.getItemMeta().getPersistentDataContainer().has(MAP_DATA_KEY, PersistentDataType.STRING)) {
                itemStack.getItemMeta().getPersistentDataContainer().set(MAP_DATA_KEY, PersistentDataType.STRING,
                        MapData.getFromFile(Bukkit.getWorlds().get(0).getWorldFolder(), mapId).toString());
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to serialize map data for map " + mapId + ")");
        }
    }

    /**
     * Set the map data of the given {@link ItemStack} to the given {@link MapData}, applying a map view to the item stack
     *
     * @param itemStack the {@link ItemStack} to set the map data of
     */
    public static void setMapRenderer(@NotNull ItemStack itemStack) {
        if (itemStack.getType() != Material.FILLED_MAP) {
            return;
        }

        final MapMeta mapMeta = (MapMeta) itemStack.getItemMeta();
        if (mapMeta == null) {
            return;
        }

        if (!itemStack.getItemMeta().getPersistentDataContainer().has(MAP_DATA_KEY, PersistentDataType.STRING)) {
            return;
        }

        try {
            final String serializedData = Objects.requireNonNull(itemStack
                    .getItemMeta().getPersistentDataContainer().get(MAP_DATA_KEY, PersistentDataType.STRING));
            final MapData mapData = MapData.fromString(serializedData);

            // Create a new map view renderer with the map data color at each pixel
            final MapView mapView = mapMeta.getMapView();
            if (mapView == null) {
                return;
            }
            mapView.getRenderers().forEach(mapView::removeRenderer);
            mapView.addRenderer(new BukkitMapDataRenderer(mapData));
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to deserialize map data for a player");
        }
    }

    /**
     * Renders {@link MapData} to a bukkit {@link MapView}.
     */
    public static class BukkitMapDataRenderer extends MapRenderer {

        private final MapData mapData;

        protected BukkitMapDataRenderer(@NotNull MapData mapData) {
            this.mapData = mapData;
        }

        @Override
        public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
            for (int i = 0; i < 128; i++) {
                for (int j = 0; j < 128; j++) {
                    canvas.setPixel(i, j, (byte) mapData.getColorAt(i, j).intValue());
                }
            }
            map.setLocked(true);
        }
    }
}
