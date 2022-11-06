package net.william278.husksync.player;

import de.themoep.minedown.adventure.MineDown;
import de.themoep.minedown.adventure.MineDownParser;
import dev.triumphteam.gui.builder.gui.StorageBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.StorageGui;
import net.kyori.adventure.audience.Audience;
import net.william278.desertwell.Version;
import net.william278.husksync.BukkitHuskSync;
import net.william278.husksync.config.Settings;
import net.william278.husksync.data.*;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * Bukkit implementation of an {@link OnlineUser}
 */
public class BukkitPlayer extends OnlineUser {

    private static final PersistentDataType<?, ?>[] PRIMITIVE_PERSISTENT_DATA_TYPES = new PersistentDataType<?, ?>[]{
            PersistentDataType.BYTE,
            PersistentDataType.SHORT,
            PersistentDataType.INTEGER,
            PersistentDataType.LONG,
            PersistentDataType.FLOAT,
            PersistentDataType.DOUBLE,
            PersistentDataType.STRING,
            PersistentDataType.BYTE_ARRAY,
            PersistentDataType.INTEGER_ARRAY,
            PersistentDataType.LONG_ARRAY,
            PersistentDataType.TAG_CONTAINER_ARRAY,
            PersistentDataType.TAG_CONTAINER};

    private final Player player;
    private final Audience audience;

    private BukkitPlayer(@NotNull Player player) {
        super(player.getUniqueId(), player.getName());
        this.player = player;
        this.audience = BukkitHuskSync.getInstance().getAudiences().player(player);
    }

    public static BukkitPlayer adapt(@NotNull Player player) {
        return new BukkitPlayer(player);
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public CompletableFuture<StatusData> getStatus() {
        return CompletableFuture.supplyAsync(() -> {
            final double maxHealth = getMaxHealth(player);
            return new StatusData(Math.min(player.getHealth(), maxHealth),
                    maxHealth,
                    player.isHealthScaled() ? player.getHealthScale() : 0d,
                    player.getFoodLevel(),
                    player.getSaturation(),
                    player.getExhaustion(),
                    player.getInventory().getHeldItemSlot(),
                    player.getTotalExperience(),
                    player.getLevel(),
                    player.getExp(),
                    player.getGameMode().name(),
                    player.getAllowFlight() && player.isFlying());
        });
    }

    @Override
    public CompletableFuture<Void> setStatus(@NotNull StatusData statusData, @NotNull Settings settings) {
        return CompletableFuture.runAsync(() -> {
            double currentMaxHealth = Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH))
                    .getBaseValue();
            if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.MAX_HEALTH)) {
                if (statusData.maxHealth != 0d) {
                    Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH))
                            .setBaseValue(statusData.maxHealth);
                    currentMaxHealth = statusData.maxHealth;
                }
            }
            if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.HEALTH)) {
                final double currentHealth = player.getHealth();
                if (statusData.health != currentHealth) {
                    final double healthToSet = currentHealth > currentMaxHealth ? currentMaxHealth : statusData.health;
                    if (healthToSet < 1) {
                        Bukkit.getScheduler().runTask(BukkitHuskSync.getInstance(), () -> player.setHealth(healthToSet));
                    } else {
                        player.setHealth(healthToSet);
                    }
                }

                if (statusData.healthScale != 0d) {
                    player.setHealthScale(statusData.healthScale);
                } else {
                    player.setHealthScale(statusData.maxHealth);
                }
                player.setHealthScaled(statusData.healthScale != 0D);
            }
            if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.HUNGER)) {
                player.setFoodLevel(statusData.hunger);
                player.setSaturation(statusData.saturation);
                player.setExhaustion(statusData.saturationExhaustion);
            }
            if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.INVENTORIES)) {
                player.getInventory().setHeldItemSlot(statusData.selectedItemSlot);
            }
            if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.EXPERIENCE)) {
                player.setTotalExperience(statusData.totalExperience);
                player.setLevel(statusData.expLevel);
                player.setExp(statusData.expProgress);
            }
            if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.GAME_MODE)) {
                Bukkit.getScheduler().runTask(BukkitHuskSync.getInstance(), () ->
                        player.setGameMode(GameMode.valueOf(statusData.gameMode)));
            }
            if (settings.getSynchronizationFeature(Settings.SynchronizationFeature.LOCATION)) {
                Bukkit.getScheduler().runTask(BukkitHuskSync.getInstance(), () -> {
                    if (statusData.isFlying) {
                        player.setAllowFlight(true);
                        player.setFlying(true);
                    }
                    player.setFlying(false);
                });
            }
        });
    }

    @Override
    public CompletableFuture<ItemData> getInventory() {
        return BukkitSerializer.serializeItemStackArray(player.getInventory().getContents())
                .thenApply(ItemData::new);
    }

    @Override
    public CompletableFuture<Void> setInventory(@NotNull ItemData itemData) {
        return BukkitSerializer.deserializeInventory(itemData.serializedItems).thenApplyAsync(contents -> {
            final CompletableFuture<Void> inventorySetFuture = new CompletableFuture<>();
            Bukkit.getScheduler().runTask(BukkitHuskSync.getInstance(), () -> {
                player.getInventory().setContents(contents.getContents());
                inventorySetFuture.complete(null);
            });
            return inventorySetFuture.join();
        });
    }

    @Override
    public CompletableFuture<ItemData> getEnderChest() {
        return BukkitSerializer.serializeItemStackArray(player.getEnderChest().getContents())
                .thenApply(ItemData::new);
    }

    @Override
    public CompletableFuture<Void> setEnderChest(@NotNull ItemData enderChestData) {
        return BukkitSerializer.deserializeItemStackArray(enderChestData.serializedItems).thenApplyAsync(contents -> {
            final CompletableFuture<Void> enderChestSetFuture = new CompletableFuture<>();
            Bukkit.getScheduler().runTask(BukkitHuskSync.getInstance(), () -> {
                player.getEnderChest().setContents(contents);
                enderChestSetFuture.complete(null);
            });
            return enderChestSetFuture.join();
        });
    }

    @Override
    public CompletableFuture<PotionEffectData> getPotionEffects() {
        return BukkitSerializer.serializePotionEffectArray(player.getActivePotionEffects()
                .toArray(new PotionEffect[0])).thenApply(PotionEffectData::new);
    }

    @Override
    public CompletableFuture<Void> setPotionEffects(@NotNull PotionEffectData potionEffectData) {
        return BukkitSerializer.deserializePotionEffectArray(potionEffectData.serializedPotionEffects)
                .thenApplyAsync(effects -> {
                    final CompletableFuture<Void> potionEffectsSetFuture = new CompletableFuture<>();
                    Bukkit.getScheduler().runTask(BukkitHuskSync.getInstance(), () -> {
                        for (PotionEffect effect : player.getActivePotionEffects()) {
                            player.removePotionEffect(effect.getType());
                        }
                        for (PotionEffect effect : effects) {
                            player.addPotionEffect(effect);
                        }
                        potionEffectsSetFuture.complete(null);
                    });
                    return potionEffectsSetFuture.join();
                });
    }

    @Override
    public CompletableFuture<List<AdvancementData>> getAdvancements() {
        return CompletableFuture.supplyAsync(() -> {
            final Iterator<Advancement> serverAdvancements = Bukkit.getServer().advancementIterator();
            final ArrayList<AdvancementData> advancementData = new ArrayList<>();

            // Iterate through the server advancement set and add all advancements to the list
            serverAdvancements.forEachRemaining(advancement -> {
                final AdvancementProgress advancementProgress = player.getAdvancementProgress(advancement);
                final Map<String, Date> awardedCriteria = new HashMap<>();

                advancementProgress.getAwardedCriteria().forEach(criteriaKey -> awardedCriteria.put(criteriaKey,
                        advancementProgress.getDateAwarded(criteriaKey)));

                // Only save the advancement if criteria has been completed
                if (!awardedCriteria.isEmpty()) {
                    advancementData.add(new AdvancementData(advancement.getKey().toString(), awardedCriteria));
                }
            });
            return advancementData;
        });
    }

    @Override
    public CompletableFuture<Void> setAdvancements(@NotNull List<AdvancementData> advancementData) {
        return CompletableFuture.runAsync(() -> Bukkit.getScheduler().runTask(BukkitHuskSync.getInstance(), () -> {

            // Temporarily disable advancement announcing if needed
            boolean announceAdvancementUpdate = false;
            if (Boolean.TRUE.equals(player.getWorld().getGameRuleValue(GameRule.ANNOUNCE_ADVANCEMENTS))) {
                player.getWorld().setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
                announceAdvancementUpdate = true;
            }
            final boolean finalAnnounceAdvancementUpdate = announceAdvancementUpdate;

            // Save current experience and level
            final int experienceLevel = player.getLevel();
            final float expProgress = player.getExp();

            // Determines whether the experience might have changed warranting an update
            final AtomicBoolean correctExperience = new AtomicBoolean(false);

            // Run asynchronously as advancement setting is expensive
            CompletableFuture.runAsync(() -> {
                // Apply the advancements to the player
                final Iterator<Advancement> serverAdvancements = Bukkit.getServer().advancementIterator();
                while (serverAdvancements.hasNext()) {
                    // Iterate through all advancements
                    final Advancement advancement = serverAdvancements.next();
                    final AdvancementProgress playerProgress = player.getAdvancementProgress(advancement);

                    advancementData.stream().filter(record -> record.key.equals(advancement.getKey().toString())).findFirst().ifPresentOrElse(
                            // Award all criteria that the player does not have that they do on the cache
                            record -> {
                                record.completedCriteria.keySet().stream()
                                        .filter(criterion -> !playerProgress.getAwardedCriteria().contains(criterion))
                                        .forEach(criterion -> {
                                            Bukkit.getScheduler().runTask(BukkitHuskSync.getInstance(),
                                                    () -> player.getAdvancementProgress(advancement).awardCriteria(criterion));
                                            correctExperience.set(true);
                                        });

                                // Revoke all criteria that the player does have but should not
                                new ArrayList<>(playerProgress.getAwardedCriteria()).stream().filter(criterion -> !record.completedCriteria.containsKey(criterion))
                                        .forEach(criterion -> Bukkit.getScheduler().runTask(BukkitHuskSync.getInstance(),
                                                () -> player.getAdvancementProgress(advancement).revokeCriteria(criterion)));

                            },
                            // Revoke the criteria as the player shouldn't have any
                            () -> new ArrayList<>(playerProgress.getAwardedCriteria()).forEach(criterion ->
                                    Bukkit.getScheduler().runTask(BukkitHuskSync.getInstance(),
                                            () -> player.getAdvancementProgress(advancement).revokeCriteria(criterion))));

                    // Update the player's experience in case the advancement changed that
                    if (correctExperience.get()) {
                        player.setLevel(experienceLevel);
                        player.setExp(expProgress);
                        correctExperience.set(false);
                    }
                }

                // Re-enable announcing advancements (back on main thread again)
                Bukkit.getScheduler().runTask(BukkitHuskSync.getInstance(), () -> {
                    if (finalAnnounceAdvancementUpdate) {
                        player.getWorld().setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, true);
                    }
                });
            });
        }));
    }

    @Override
    public CompletableFuture<StatisticsData> getStatistics() {
        return CompletableFuture.supplyAsync(() -> {
            final Map<String, Integer> untypedStatisticValues = new HashMap<>();
            final Map<String, Map<String, Integer>> blockStatisticValues = new HashMap<>();
            final Map<String, Map<String, Integer>> itemStatisticValues = new HashMap<>();
            final Map<String, Map<String, Integer>> entityStatisticValues = new HashMap<>();

            for (Statistic statistic : Statistic.values()) {
                switch (statistic.getType()) {
                    case ITEM -> {
                        final Map<String, Integer> itemValues = new HashMap<>();
                        Arrays.stream(Material.values()).filter(Material::isItem)
                                .filter(itemMaterial -> (player.getStatistic(statistic, itemMaterial)) != 0)
                                .forEach(itemMaterial -> itemValues.put(itemMaterial.name(),
                                        player.getStatistic(statistic, itemMaterial)));
                        if (!itemValues.isEmpty()) {
                            itemStatisticValues.put(statistic.name(), itemValues);
                        }
                    }
                    case BLOCK -> {
                        final Map<String, Integer> blockValues = new HashMap<>();
                        Arrays.stream(Material.values()).filter(Material::isBlock)
                                .filter(blockMaterial -> (player.getStatistic(statistic, blockMaterial)) != 0)
                                .forEach(blockMaterial -> blockValues.put(blockMaterial.name(),
                                        player.getStatistic(statistic, blockMaterial)));
                        if (!blockValues.isEmpty()) {
                            blockStatisticValues.put(statistic.name(), blockValues);
                        }
                    }
                    case ENTITY -> {
                        final Map<String, Integer> entityValues = new HashMap<>();
                        Arrays.stream(EntityType.values()).filter(EntityType::isAlive)
                                .filter(entityType -> (player.getStatistic(statistic, entityType)) != 0)
                                .forEach(entityType -> entityValues.put(entityType.name(),
                                        player.getStatistic(statistic, entityType)));
                        if (!entityValues.isEmpty()) {
                            entityStatisticValues.put(statistic.name(), entityValues);
                        }
                    }
                    case UNTYPED -> {
                        if (player.getStatistic(statistic) != 0) {
                            untypedStatisticValues.put(statistic.name(), player.getStatistic(statistic));
                        }
                    }
                }
            }

            return new StatisticsData(untypedStatisticValues, blockStatisticValues,
                    itemStatisticValues, entityStatisticValues);
        });
    }

    @Override
    public CompletableFuture<Void> setStatistics(@NotNull StatisticsData statisticsData) {
        return CompletableFuture.runAsync(() -> {
            // Set untyped statistics
            for (String statistic : statisticsData.untypedStatistics.keySet()) {
                player.setStatistic(Statistic.valueOf(statistic), statisticsData.untypedStatistics.get(statistic));
            }

            // Set block statistics
            for (String statistic : statisticsData.blockStatistics.keySet()) {
                for (String blockMaterial : statisticsData.blockStatistics.get(statistic).keySet()) {
                    player.setStatistic(Statistic.valueOf(statistic), Material.valueOf(blockMaterial),
                            statisticsData.blockStatistics.get(statistic).get(blockMaterial));
                }
            }

            // Set item statistics
            for (String statistic : statisticsData.itemStatistics.keySet()) {
                for (String itemMaterial : statisticsData.itemStatistics.get(statistic).keySet()) {
                    player.setStatistic(Statistic.valueOf(statistic), Material.valueOf(itemMaterial),
                            statisticsData.itemStatistics.get(statistic).get(itemMaterial));
                }
            }

            // Set entity statistics
            for (String statistic : statisticsData.entityStatistics.keySet()) {
                for (String entityType : statisticsData.entityStatistics.get(statistic).keySet()) {
                    player.setStatistic(Statistic.valueOf(statistic), EntityType.valueOf(entityType),
                            statisticsData.entityStatistics.get(statistic).get(entityType));
                }
            }
        });
    }

    @Override
    public CompletableFuture<LocationData> getLocation() {
        return CompletableFuture.supplyAsync(() ->
                new LocationData(player.getWorld().getName(), player.getWorld().getUID(), player.getWorld().getEnvironment().name(),
                        player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(),
                        player.getLocation().getYaw(), player.getLocation().getPitch()));
    }

    @Override
    public CompletableFuture<Void> setLocation(@NotNull LocationData locationData) {
        final CompletableFuture<Void> teleportFuture = new CompletableFuture<>();
        AtomicReference<World> bukkitWorld = new AtomicReference<>(Bukkit.getWorld(locationData.worldName));
        if (bukkitWorld.get() == null) {
            bukkitWorld.set(Bukkit.getWorld(locationData.worldUuid));
        }
        if (bukkitWorld.get() == null) {
            Bukkit.getWorlds().stream().filter(world -> world.getEnvironment() == World.Environment
                    .valueOf(locationData.worldEnvironment)).findFirst().ifPresent(bukkitWorld::set);
        }
        if (bukkitWorld.get() != null) {
            Bukkit.getScheduler().runTask(BukkitHuskSync.getInstance(), () -> {
                player.teleport(new Location(bukkitWorld.get(),
                        locationData.x, locationData.y, locationData.z,
                        locationData.yaw, locationData.pitch), PlayerTeleportEvent.TeleportCause.PLUGIN);
                teleportFuture.complete(null);
            });
        }
        return teleportFuture;
    }

    @Override
    public CompletableFuture<PersistentDataContainerData> getPersistentDataContainer() {
        return CompletableFuture.supplyAsync(() -> {
            final PersistentDataContainer container = player.getPersistentDataContainer();
            if (container.isEmpty()) {
                return new PersistentDataContainerData(new HashMap<>());
            }
            final HashMap<String, PersistentDataTag<?>> persistentDataMap = new HashMap<>();
            for (final NamespacedKey key : container.getKeys()) {
                PersistentDataType<?, ?> type = null;
                for (PersistentDataType<?, ?> dataType : PRIMITIVE_PERSISTENT_DATA_TYPES) {
                    if (container.has(key, dataType)) {
                        type = dataType;
                        break;
                    }
                }
                if (type != null) {
                    // This is absolutely disgusting code and needs to be swiftly put out of its misery with a refactor
                    final Class<?> primitiveType = type.getPrimitiveType();
                    if (String.class.equals(primitiveType)) {
                        persistentDataMap.put(key.toString(), new PersistentDataTag<>(BukkitPersistentDataTagType.STRING,
                                Objects.requireNonNull(container.get(key, PersistentDataType.STRING))));
                    } else if (int.class.equals(primitiveType)) {
                        persistentDataMap.put(key.toString(), new PersistentDataTag<>(BukkitPersistentDataTagType.INTEGER,
                                Objects.requireNonNull(container.get(key, PersistentDataType.INTEGER))));
                    } else if (double.class.equals(primitiveType)) {
                        persistentDataMap.put(key.toString(), new PersistentDataTag<>(BukkitPersistentDataTagType.DOUBLE,
                                Objects.requireNonNull(container.get(key, PersistentDataType.DOUBLE))));
                    } else if (float.class.equals(primitiveType)) {
                        persistentDataMap.put(key.toString(), new PersistentDataTag<>(BukkitPersistentDataTagType.FLOAT,
                                Objects.requireNonNull(container.get(key, PersistentDataType.FLOAT))));
                    } else if (long.class.equals(primitiveType)) {
                        persistentDataMap.put(key.toString(), new PersistentDataTag<>(BukkitPersistentDataTagType.LONG,
                                Objects.requireNonNull(container.get(key, PersistentDataType.LONG))));
                    } else if (short.class.equals(primitiveType)) {
                        persistentDataMap.put(key.toString(), new PersistentDataTag<>(BukkitPersistentDataTagType.SHORT,
                                Objects.requireNonNull(container.get(key, PersistentDataType.SHORT))));
                    } else if (byte.class.equals(primitiveType)) {
                        persistentDataMap.put(key.toString(), new PersistentDataTag<>(BukkitPersistentDataTagType.BYTE,
                                Objects.requireNonNull(container.get(key, PersistentDataType.BYTE))));
                    } else if (byte[].class.equals(primitiveType)) {
                        persistentDataMap.put(key.toString(), new PersistentDataTag<>(BukkitPersistentDataTagType.BYTE_ARRAY,
                                Objects.requireNonNull(container.get(key, PersistentDataType.BYTE_ARRAY))));
                    } else if (int[].class.equals(primitiveType)) {
                        persistentDataMap.put(key.toString(), new PersistentDataTag<>(BukkitPersistentDataTagType.INTEGER_ARRAY,
                                Objects.requireNonNull(container.get(key, PersistentDataType.INTEGER_ARRAY))));
                    } else if (long[].class.equals(primitiveType)) {
                        persistentDataMap.put(key.toString(), new PersistentDataTag<>(BukkitPersistentDataTagType.LONG_ARRAY,
                                Objects.requireNonNull(container.get(key, PersistentDataType.LONG_ARRAY))));
                    } else if (PersistentDataContainer.class.equals(primitiveType)) {
                        persistentDataMap.put(key.toString(), new PersistentDataTag<>(BukkitPersistentDataTagType.TAG_CONTAINER,
                                Objects.requireNonNull(container.get(key, PersistentDataType.TAG_CONTAINER))));
                    } else if (PersistentDataContainer[].class.equals(primitiveType)) {
                        persistentDataMap.put(key.toString(), new PersistentDataTag<>(BukkitPersistentDataTagType.TAG_CONTAINER_ARRAY,
                                Objects.requireNonNull(container.get(key, PersistentDataType.TAG_CONTAINER_ARRAY))));
                    }
                }
            }
            return new PersistentDataContainerData(persistentDataMap);
        }).exceptionally(throwable -> {
            BukkitHuskSync.getInstance().getLoggingAdapter().log(Level.WARNING,
                    "Could not read " + player.getName() + "'s persistent data map, skipping!");
            throwable.printStackTrace();
            return new PersistentDataContainerData(new HashMap<>());
        });
    }

    @Override
    public CompletableFuture<Void> setPersistentDataContainer(@NotNull PersistentDataContainerData persistentDataContainerData) {
        return CompletableFuture.runAsync(() -> {
            player.getPersistentDataContainer().getKeys().forEach(namespacedKey ->
                    player.getPersistentDataContainer().remove(namespacedKey));
            persistentDataContainerData.getTags().forEach(keyString -> {
                final NamespacedKey key = NamespacedKey.fromString(keyString);
                if (key != null) {
                    // Set a tag with the given key and value. This is crying out for a refactor.
                    persistentDataContainerData.getTagType(keyString).ifPresentOrElse(dataType -> {
                        switch (dataType) {
                            case BYTE -> persistentDataContainerData.getTagValue(keyString, byte.class).ifPresent(
                                    value -> player.getPersistentDataContainer().set(key,
                                            PersistentDataType.BYTE, value));
                            case SHORT -> persistentDataContainerData.getTagValue(keyString, short.class).ifPresent(
                                    value -> player.getPersistentDataContainer().set(key,
                                            PersistentDataType.SHORT, value));
                            case INTEGER -> persistentDataContainerData.getTagValue(keyString, int.class).ifPresent(
                                    value -> player.getPersistentDataContainer().set(key,
                                            PersistentDataType.INTEGER, value));
                            case LONG -> persistentDataContainerData.getTagValue(keyString, long.class).ifPresent(
                                    value -> player.getPersistentDataContainer().set(key,
                                            PersistentDataType.LONG, value));
                            case FLOAT -> persistentDataContainerData.getTagValue(keyString, float.class).ifPresent(
                                    value -> player.getPersistentDataContainer().set(key,
                                            PersistentDataType.FLOAT, value));
                            case DOUBLE -> persistentDataContainerData.getTagValue(keyString, double.class).ifPresent(
                                    value -> player.getPersistentDataContainer().set(key,
                                            PersistentDataType.DOUBLE, value));
                            case STRING -> persistentDataContainerData.getTagValue(keyString, String.class).ifPresent(
                                    value -> player.getPersistentDataContainer().set(key,
                                            PersistentDataType.STRING, value));
                            case BYTE_ARRAY ->
                                    persistentDataContainerData.getTagValue(keyString, byte[].class).ifPresent(
                                            value -> player.getPersistentDataContainer().set(key,
                                                    PersistentDataType.BYTE_ARRAY, value));
                            case INTEGER_ARRAY ->
                                    persistentDataContainerData.getTagValue(keyString, int[].class).ifPresent(
                                            value -> player.getPersistentDataContainer().set(key,
                                                    PersistentDataType.INTEGER_ARRAY, value));
                            case LONG_ARRAY ->
                                    persistentDataContainerData.getTagValue(keyString, long[].class).ifPresent(
                                            value -> player.getPersistentDataContainer().set(key,
                                                    PersistentDataType.LONG_ARRAY, value));
                            case TAG_CONTAINER ->
                                    persistentDataContainerData.getTagValue(keyString, PersistentDataContainer.class).ifPresent(
                                            value -> player.getPersistentDataContainer().set(key,
                                                    PersistentDataType.TAG_CONTAINER, value));
                            case TAG_CONTAINER_ARRAY ->
                                    persistentDataContainerData.getTagValue(keyString, PersistentDataContainer[].class).ifPresent(
                                            value -> player.getPersistentDataContainer().set(key,
                                                    PersistentDataType.TAG_CONTAINER_ARRAY, value));
                        }
                    }, () -> BukkitHuskSync.getInstance().getLoggingAdapter().log(Level.WARNING,
                            "Could not set " + player.getName() + "'s persistent data key " + keyString +
                            " as it has an invalid type. Skipping!"));
                }
            });
        }).exceptionally(throwable -> {
            BukkitHuskSync.getInstance().getLoggingAdapter().log(Level.WARNING,
                    "Could not write " + player.getName() + "'s persistent data map, skipping!");
            throwable.printStackTrace();
            return null;
        });
    }

    @Override
    public boolean isOffline() {
        try {
            return player == null;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @NotNull
    @Override
    public Version getMinecraftVersion() {
        return Version.fromMinecraftVersionString(Bukkit.getBukkitVersion());
    }

    @Override
    public boolean hasPermission(@NotNull String node) {
        return player.hasPermission(node);
    }

    @Override
    public CompletableFuture<Optional<ItemData>> showMenu(@NotNull ItemData itemData, boolean editable,
                                                          int minimumRows, @NotNull MineDown title) {
        final CompletableFuture<Optional<ItemData>> updatedData = new CompletableFuture<>();

        // Deserialize the item data to be shown and show it in a triumph GUI
        BukkitSerializer.deserializeItemStackArray(itemData.serializedItems).thenAccept(items -> {
            // Build the GUI and populate with items
            final int itemCount = items.length;
            final StorageBuilder guiBuilder = Gui.storage()
                    .title(title.toComponent())
                    .rows(Math.max(minimumRows, (int) Math.ceil(itemCount / 9.0)))
                    .disableAllInteractions()
                    .enableOtherActions();
            final StorageGui gui = editable ? guiBuilder.enableAllInteractions().create() : guiBuilder.create();
            for (int i = 0; i < itemCount; i++) {
                if (items[i] != null) {
                    gui.getInventory().setItem(i, items[i]);
                }
            }

            // Complete the future with updated data (if editable) when the GUI is closed
            gui.setCloseGuiAction(event -> {
                if (!editable) {
                    updatedData.complete(Optional.empty());
                    return;
                }

                // Get and save the updated items
                final ItemStack[] updatedItems = Arrays.copyOf(event.getPlayer().getOpenInventory()
                        .getTopInventory().getContents().clone(), itemCount);
                BukkitSerializer.serializeItemStackArray(updatedItems).thenAccept(serializedItems -> {
                    if (serializedItems.equals(itemData.serializedItems)) {
                        updatedData.complete(Optional.empty());
                        return;
                    }
                    updatedData.complete(Optional.of(new ItemData(serializedItems)));
                });
            });

            // Display the GUI (synchronously; on the main server thread)
            Bukkit.getScheduler().runTask(BukkitHuskSync.getInstance(), () -> gui.open(player));
        }).exceptionally(throwable -> {
            // Handle exceptions
            updatedData.completeExceptionally(throwable);
            return null;
        });
        return updatedData;
    }

    @Override
    public boolean isDead() {
        return player.getHealth() <= 0;
    }

    @Override
    public void sendActionBar(@NotNull MineDown mineDown) {
        audience.sendActionBar(mineDown
                .disable(MineDownParser.Option.SIMPLE_FORMATTING)
                .replace().toComponent());
    }

    @Override
    public void sendMessage(@NotNull MineDown mineDown) {
        audience.sendMessage(mineDown
                .disable(MineDownParser.Option.SIMPLE_FORMATTING)
                .replace().toComponent());
    }

    /**
     * Returns a {@link Player}'s maximum health, minus any health boost effects
     *
     * @param player The {@link Player} to get the maximum health of
     * @return The {@link Player}'s max health
     */
    private static double getMaxHealth(@NotNull Player player) {
        double maxHealth = Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue();

        // If the player has additional health bonuses from synchronised potion effects, subtract these from this number as they are synchronised separately
        if (player.hasPotionEffect(PotionEffectType.HEALTH_BOOST) && maxHealth > 20D) {
            PotionEffect healthBoostEffect = player.getPotionEffect(PotionEffectType.HEALTH_BOOST);
            assert healthBoostEffect != null;
            double healthBoostBonus = 4 * (healthBoostEffect.getAmplifier() + 1);
            maxHealth -= healthBoostBonus;
        }
        return maxHealth;
    }

}
