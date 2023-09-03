package net.william278.husksync.data;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface BukkitDataOwner extends DataOwner {

    //todo check if dead and apply special rule
    @NotNull
    @Override
    default Optional<DataContainer.Items.Inventory> getInventory() {
        return Optional.of(BukkitDataContainer.Items.Inventory.adapt(getBukkitPlayer()));
    }

    @NotNull
    @Override
    default Optional<DataContainer.Items.EnderChest> getEnderChest() {
        return Optional.of(BukkitDataContainer.Items.EnderChest.adapt(getBukkitPlayer().getEnderChest().getContents()));
    }

    @NotNull
    @Override
    default Optional<DataContainer.PotionEffects> getPotionEffects() {
        return Optional.of(BukkitDataContainer.PotionEffects.adapt(getBukkitPlayer().getActivePotionEffects()));
    }

    @NotNull
    @Override
    default Optional<DataContainer.Advancements> getAdvancements() {
        return Optional.of(BukkitDataContainer.Advancements.adapt(getBukkitPlayer(), getPlugin()));
    }

    @NotNull
    @Override
    default Optional<DataContainer.Location> getLocation() {
        return Optional.of(BukkitDataContainer.Location.adapt(getBukkitPlayer().getLocation()));
    }

    @NotNull
    @Override
    default Optional<DataContainer.Statistics> getStatistics() {
        return DataOwner.super.getStatistics();
    }

    @NotNull
    @Override
    default Optional<DataContainer.Health> getHealth() {
        return DataOwner.super.getHealth();
    }

    @NotNull
    @Override
    default Optional<DataContainer.Food> getFood() {
        return DataOwner.super.getFood();
    }

    @NotNull
    @Override
    default Optional<DataContainer.Experience> getExperience() {
        return DataOwner.super.getExperience();
    }

    @NotNull
    @Override
    default Optional<DataContainer.GameMode> getGameMode() {
        return DataOwner.super.getGameMode();
    }

    @NotNull
    @Override
    default Optional<DataContainer.PersistentData> getPersistentData() {
        return DataOwner.super.getPersistentData();
    }

    @NotNull
    Player getBukkitPlayer();

}
