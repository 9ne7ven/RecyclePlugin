package com.svncho.util;

import org.bukkit.Material;

import java.util.Locale;
import java.util.Optional;

public final class MaterialResolver {

    private MaterialResolver() {}

    public static Optional<Material> resolve(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();

        String normalized = raw.trim()
                .toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_')
                .replace("MINECRAFT:", "");

        normalized = switch (normalized) {
            case "WOOD_SWORD" -> "WOODEN_SWORD";
            case "WOOD_PICKAXE" -> "WOODEN_PICKAXE";
            case "WOOD_AXE" -> "WOODEN_AXE";
            case "WOOD_SHOVEL" -> "WOODEN_SHOVEL";
            case "WOOD_HOE" -> "WOODEN_HOE";
            case "GOLD_SWORD" -> "GOLDEN_SWORD";
            case "GOLD_PICKAXE" -> "GOLDEN_PICKAXE";
            case "GOLD_AXE" -> "GOLDEN_AXE";
            case "GOLD_SHOVEL" -> "GOLDEN_SHOVEL";
            case "GOLD_HOE" -> "GOLDEN_HOE";
            case "GOLD_HELMET" -> "GOLDEN_HELMET";
            case "GOLD_CHESTPLATE" -> "GOLDEN_CHESTPLATE";
            case "GOLD_LEGGINGS" -> "GOLDEN_LEGGINGS";
            case "GOLD_BOOTS" -> "GOLDEN_BOOTS";
            default -> normalized;
        };

        Material material = Material.matchMaterial(normalized);
        return material == null ? Optional.empty() : Optional.of(material);
    }
}
