package net.william278.husksync.util;

import de.tr7zw.changeme.nbtapi.NBT;
import net.william278.husksync.HuskSync;
import net.william278.mapdataapi.MapData;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

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
                item.setByteArray(MAP_DATA_KEY, data.toBytes());
            });
        } catch (Throwable e) {
            getPlugin().log(Level.WARNING, String.format("Failed to serialize locked map data (Map ID: %s)", mapId));
        }
        return map;
    }

    @NotNull
    private ItemStack applyMapView(@NotNull ItemStack map) {
        NBT.get(map, nbt -> {
            if (!nbt.hasTag(MAP_DATA_KEY)) {
                return nbt;
            }

            //todo render map

            return nbt;
        });
        return map;
    }

    @ApiStatus.Internal
    @NotNull
    HuskSync getPlugin();

}
