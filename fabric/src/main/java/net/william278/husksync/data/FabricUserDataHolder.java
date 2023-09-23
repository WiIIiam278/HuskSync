package net.william278.husksync.data;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

public interface FabricUserDataHolder extends UserDataHolder {

    @Override
    default Optional<? extends Data> getData(@NotNull Identifier id) {
        if (!id.isCustom()) {
            return switch (id.getKeyValue()) {
                case "inventory" -> getInventory();
                case "ender_chest" -> getEnderChest();
                case "potion_effects" -> getPotionEffects();
                case "advancements" -> getAdvancements();
                case "location" -> getLocation();
                case "statistics" -> getStatistics();
                case "health" -> getHealth();
                case "hunger" -> getHunger();
                case "experience" -> getExperience();
                case "game_mode" -> getGameMode();
                case "persistent_data" -> Optional.ofNullable(getCustomDataStore().get(id));
                default -> throw new IllegalStateException(String.format("Unexpected data type: %s", id));
            };
        }
        return Optional.ofNullable(getCustomDataStore().get(id));
    }
    @Override
    default void setData(@NotNull Identifier id, @NotNull Data data) {
        if (id.isCustom()) {
            getCustomDataStore().put(id, data);
        }
        UserDataHolder.super.setData(id, data);
    }

    @NotNull
    @Override
    default Optional<Data.Items.Inventory> getInventory() {
        if ((isDead() && !getPlugin().getSettings().doSynchronizeDeadPlayersChangingServer())) {
            return Optional.of(FabricData.Items.Inventory.empty());
        }
        final PlayerInventory inventory = getPlayer().getInventory();
        return Optional.of(FabricData.Items.Inventory.from(
                inventory,
                inventory.selectedSlot
        ));
    }

    @NotNull
    @Override
    default Optional<Data.Items.EnderChest> getEnderChest() {
        return Optional.of(FabricData.Items.EnderChest.adapt(
                getPlayer().getEnderChestInventory()
        ));
    }

    @NotNull
    @Override
    default Optional<Data.PotionEffects> getPotionEffects() {
        return Optional.of(FabricData.PotionEffects.from(getPlayer().getActiveStatusEffects()));
    }

    @NotNull
    @Override
    default Optional<Data.Advancements> getAdvancements() {
        return Optional.of(FabricData.Advancements.adapt(getPlayer()));
    }

    @NotNull
    @Override
    default Optional<Data.Location> getLocation() {
        return Optional.of(FabricData.Location.adapt(getPlayer()));
    }

    @NotNull
    @Override
    default Optional<Data.Statistics> getStatistics() {
        return Optional.of(FabricData.Statistics.adapt(getPlayer()));
    }

    @NotNull
    @Override
    default Optional<Data.Health> getHealth() {
        return Optional.of(FabricData.Health.adapt(getPlayer()));
    }

    @NotNull
    @Override
    default Optional<Data.Hunger> getHunger() {
        return Optional.of(FabricData.Hunger.adapt(getPlayer()));
    }

    @NotNull
    @Override
    default Optional<Data.Experience> getExperience() {
        return Optional.of(FabricData.Experience.adapt(getPlayer()));
    }

    @NotNull
    @Override
    default Optional<Data.GameMode> getGameMode() {
        return Optional.of(FabricData.GameMode.adapt(getPlayer()));
    }

    @NotNull
    @Override
    default Optional<Data.PersistentData> getPersistentData() {
        return Optional.empty();
    }

    boolean isDead();
    
    @NotNull
    ServerPlayerEntity getPlayer();

    @NotNull
    @Override
    Map<Identifier, Data> getCustomDataStore();

}
