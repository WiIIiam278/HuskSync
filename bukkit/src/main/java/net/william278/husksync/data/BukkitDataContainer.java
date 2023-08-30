package net.william278.husksync.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import net.william278.husksync.HuskSync;
import net.william278.husksync.player.BukkitUser;
import org.apache.commons.lang.NotImplementedException;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BukkitDataContainer implements DataContainer {

    public static abstract class Items implements DataContainer.Items {

        private final ItemStack[] contents;

        private Items(@NotNull ItemStack[] contents) {
            this.contents = contents;
        }

        @NotNull
        @Override
        public StackPreview[] getPreview() {
            return Arrays.stream(contents)
                    .map(stack -> stack != null ? new StackPreview(
                            stack.getType().getKey().toString(),
                            stack.getAmount(),
                            stack.hasItemMeta() ? (Objects.requireNonNull(
                                    stack.getItemMeta()).hasDisplayName() ? stack.getItemMeta().getDisplayName() : null)
                                    : null,
                            stack.hasItemMeta() ? (Objects.requireNonNull(
                                    stack.getItemMeta()).hasLore() ? stack.getItemMeta().getLore() : null)
                                    : null,
                            stack.hasItemMeta() && Objects.requireNonNull(stack.getItemMeta()).hasEnchants() ?
                                    stack.getItemMeta().getEnchants().keySet().stream()
                                            .map(enchantment -> enchantment.getKey().getKey())
                                            .toList()
                                    : List.of()
                    ) : null)
                    .toArray(StackPreview[]::new);
        }

        @Override
        public void clear() {
            Arrays.fill(contents, null);
        }

        @Override
        public void setContents(@NotNull Items contents) {
            System.arraycopy(
                    ((BukkitDataContainer.Items) contents).getContents(),
                    0, this.contents,
                    0, this.contents.length
            );
        }

        @NotNull
        public ItemStack[] getContents() {
            return contents;
        }

        public static class Inventory extends BukkitDataContainer.Items implements Items.Inventory {

            public static final int INVENTORY_SLOT_COUNT = 41;
            private int heldItemSlot;

            private Inventory(@NotNull ItemStack[] contents, int heldItemSlot) {
                super(contents);
                this.heldItemSlot = heldItemSlot;
            }

            @NotNull
            public static BukkitDataContainer.Items.Inventory adapt(@NotNull Player player) {
                return new BukkitDataContainer.Items.Inventory(
                        player.getInventory().getContents(),
                        player.getInventory().getHeldItemSlot()
                );
            }

            @NotNull
            public static BukkitDataContainer.Items.Inventory from(@NotNull ItemStack[] contents, int heldItemSlot) {
                return new BukkitDataContainer.Items.Inventory(contents, heldItemSlot);
            }

            @Override
            public void apply(@NotNull DataOwner user) throws IllegalStateException {
                final Player player = ((BukkitUser) user).getPlayer();
                this.clearInventoryCraftingSlots(player);
                player.setItemOnCursor(null);
                player.getInventory().setContents(getContents());
                player.updateInventory();
            }

            private void clearInventoryCraftingSlots(@NotNull Player player) {
                final org.bukkit.inventory.Inventory inventory = player.getOpenInventory().getTopInventory();
                if (inventory.getType() == InventoryType.CRAFTING) {
                    for (int slot = 0; slot < 5; slot++) {
                        inventory.setItem(slot, null);
                    }
                }
            }

            @Override
            public int getHeldItemSlot() {
                return heldItemSlot;
            }

            @Override
            public void setHeldItemSlot(int heldItemSlot) throws IllegalArgumentException {
                if (heldItemSlot < 0 || heldItemSlot > 8) {
                    throw new IllegalArgumentException("Held item slot must be between 0 and 8");
                }
                this.heldItemSlot = heldItemSlot;
            }

        }

        public static class EnderChest extends BukkitDataContainer.Items implements Items.EnderChest {

            private EnderChest(@NotNull ItemStack[] contents) {
                super(contents);
            }

            @NotNull
            public static BukkitDataContainer.Items.EnderChest adapt(@NotNull ItemStack[] items) {
                return new BukkitDataContainer.Items.EnderChest(items);
            }

            @Override
            public void apply(@NotNull DataOwner user) throws IllegalStateException {
                ((BukkitUser) user).getPlayer().getEnderChest().setContents(getContents());
            }

        }

        public static class DeathDrops extends BukkitDataContainer.Items implements Items {

            private DeathDrops(@NotNull ItemStack[] contents) {
                super(contents);
            }

            @NotNull
            public static DeathDrops adapt(@NotNull List<ItemStack> drops) {
                return new BukkitDataContainer.Items.DeathDrops(drops.toArray(ItemStack[]::new));
            }

            @Override
            public void apply(@NotNull DataOwner user) throws IllegalStateException {
                throw new NotImplementedException("Death drops cannot be applied to a player");
            }
        }
    }

    public static class PotionEffects implements DataContainer.PotionEffects {

        private final PotionEffect[] effects;

        private PotionEffects(@NotNull PotionEffect[] effects) {
            this.effects = effects;
        }

        public static BukkitDataContainer.PotionEffects adapt(@NotNull PotionEffect[] effects) {
            return new BukkitDataContainer.PotionEffects(effects);
        }

        @Override
        public void apply(@NotNull DataOwner user) throws IllegalStateException {
            final Player player = ((BukkitUser) user).getPlayer();
            player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
            player.addPotionEffects(List.of(effects));
        }

        @NotNull
        @Override
        public List<EffectPreview> getPreview() {
            return Arrays.stream(effects)
                    .map(potionEffect -> new EffectPreview(
                            potionEffect.getType().getName().toLowerCase(Locale.ENGLISH),
                            potionEffect.getDuration(),
                            potionEffect.getAmplifier(),
                            potionEffect.isAmbient(),
                            potionEffect.hasParticles(),
                            potionEffect.hasIcon()
                    ))
                    .toList();
        }

        @NotNull
        public PotionEffect[] getEffects() {
            return effects;
        }

    }

    public static class Advancements implements DataContainer.Advancements {

        private final HuskSync plugin;
        private List<CompletedAdvancement> completed;

        private Advancements(@NotNull Player player, @NotNull HuskSync plugin) {
            final Iterator<Advancement> serverAdvancements = Bukkit.getServer().advancementIterator();
            final ArrayList<CompletedAdvancement> advancementData = new ArrayList<>();

            // Iterate through the server advancement set and add all advancements to the list
            serverAdvancements.forEachRemaining(advancement -> {
                final AdvancementProgress advancementProgress = player.getAdvancementProgress(advancement);
                final Map<String, Date> awardedCriteria = new HashMap<>();

                advancementProgress.getAwardedCriteria().forEach(criteriaKey -> awardedCriteria.put(criteriaKey,
                        advancementProgress.getDateAwarded(criteriaKey)));

                // Only save the advancement if criteria has been completed
                if (!awardedCriteria.isEmpty()) {
                    advancementData.add(CompletedAdvancement.adapt(advancement.getKey().toString(), awardedCriteria));
                }
            });

            this.completed = advancementData;
            this.plugin = plugin;
        }

        private Advancements(@NotNull List<CompletedAdvancement> advancements, @NotNull HuskSync plugin) {
            this.completed = advancements;
            this.plugin = plugin;
        }

        @NotNull
        public static BukkitDataContainer.Advancements adapt(@NotNull Player player, @NotNull HuskSync plugin) {
            return new BukkitDataContainer.Advancements(player, plugin);
        }

        @NotNull
        public static BukkitDataContainer.Advancements from(@NotNull List<CompletedAdvancement> advancements,
                                                            @NotNull HuskSync plugin) {
            return new BukkitDataContainer.Advancements(advancements, plugin);
        }

        @Override
        public void apply(@NotNull DataOwner user) throws IllegalStateException {
            // Temporarily disable advancement announcing if needed
            final Player player = ((BukkitUser) user).getPlayer();
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
            plugin.runAsync(() -> {

                // Apply the advancements to the player
                final Iterator<Advancement> serverAdvancements = Bukkit.getServer().advancementIterator();
                while (serverAdvancements.hasNext()) {
                    // Iterate through all advancements
                    final Advancement advancement = serverAdvancements.next();
                    final AdvancementProgress playerProgress = player.getAdvancementProgress(advancement);

                    completed.stream().filter(record -> record.getKey().equals(advancement.getKey().toString())).findFirst().ifPresentOrElse(
                            // Award all criteria that the player does not have that they do on the cache
                            record -> {
                                record.getCompletedCriteria().keySet().stream()
                                        .filter(criterion -> !playerProgress.getAwardedCriteria().contains(criterion))
                                        .forEach(criterion -> {
                                            plugin.runAsync(() -> player.getAdvancementProgress(advancement).awardCriteria(criterion));
                                            correctExperience.set(true);
                                        });

                                // Revoke all criteria that the player does have but should not
                                new ArrayList<>(playerProgress.getAwardedCriteria()).stream()
                                        .filter(criterion -> !record.getCompletedCriteria().containsKey(criterion))
                                        .forEach(criterion -> plugin.runSync(
                                                () -> player.getAdvancementProgress(advancement).revokeCriteria(criterion)));

                            },
                            // Revoke the criteria as the player shouldn't have any
                            () -> new ArrayList<>(playerProgress.getAwardedCriteria()).forEach(criterion ->
                                    plugin.runAsync(() -> player.getAdvancementProgress(advancement).revokeCriteria(criterion))));

                    // Update the player's experience in case the advancement changed that
                    if (correctExperience.get()) {
                        player.setLevel(experienceLevel);
                        player.setExp(expProgress);
                        correctExperience.set(false);
                    }
                }

                // Re-enable announcing advancements (back on the main thread again)
                plugin.runSync(() -> {
                    if (finalAnnounceAdvancementUpdate) {
                        player.getWorld().setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, true);
                    }
                });
            });
        }

        @NotNull
        @Override
        public List<CompletedAdvancement> getCompleted() {
            return completed;
        }

        @Override
        public void setCompleted(@NotNull List<CompletedAdvancement> completed) {
            this.completed = completed;
        }

    }

}
