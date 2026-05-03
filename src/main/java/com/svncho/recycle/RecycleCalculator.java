package com.svncho.recycle;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

public class RecycleCalculator {

    private final JavaPlugin plugin;

    public RecycleCalculator(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public int calculateXP(ItemStack item, RecycleData data) {
        String prefix = plugin.getConfig().contains("recycle.xp-rewards") ? "recycle.xp-rewards." : "items.xp_rewards.";
        String rarityKey = data.rarity().toLowerCase(Locale.ROOT);

        int baseXP = plugin.getConfig().getInt(
                prefix + rarityKey + "_xp",
                plugin.getConfig().getInt(prefix + rarityKey, plugin.getConfig().getInt(prefix + "common_xp", 10))
        );

        int enchantXpPerLevel = plugin.getConfig().getInt("recycle.enchanted-xp-per-level", -1);
        if (enchantXpPerLevel >= 0) {
            int enchantLevels = item.getEnchantments().values().stream().mapToInt(Integer::intValue).sum();
            return baseXP + enchantLevels * enchantXpPerLevel;
        }

        int oldEnchantBonus = item.getEnchantments().isEmpty() ? 0 : plugin.getConfig().getInt("enchanted-xp", 50);
        return baseXP + oldEnchantBonus;
    }

    public int calculateOutputAmount(ItemStack item, RecycleData data) {
        int min = Math.min(data.minAmount(), data.maxAmount());
        int max = Math.max(data.minAmount(), data.maxAmount());

        if (min == max) return min;

        int maxDurability = item.getType().getMaxDurability();

        if (maxDurability <= 0 || !(item.getItemMeta() instanceof Damageable damageable)) {
            return max;
        }

        double durabilityPct = (double) (maxDurability - damageable.getDamage()) / maxDurability;
        double range = max - min;

        int result = (int) Math.round(min + durabilityPct * range);

        return Math.max(min, Math.min(max, result));
    }
}
