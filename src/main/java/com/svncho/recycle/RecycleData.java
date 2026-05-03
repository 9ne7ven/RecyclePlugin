package com.svncho.recycle;

import org.bukkit.Material;

public record RecycleData(Material result, int minAmount, int maxAmount, String rarity) {}
