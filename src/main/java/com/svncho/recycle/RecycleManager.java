package com.svncho.recycle;

import com.svncho.util.MaterialResolver;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class RecycleManager {

    private final JavaPlugin plugin;

    private final Map<Material, RecycleData> recycleMap = new HashMap<>();
    private final Map<UUID, Long> cooldownMap = new HashMap<>();
    private final Map<UUID, Integer> recycledStats = new HashMap<>();

    private final File statsFile;
    private final YamlConfiguration statsConfig;

    public RecycleManager(JavaPlugin plugin) {
        this.plugin = plugin;

        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create data folder.");
        }

        this.statsFile = new File(dataFolder, "stats.yml");
        this.statsConfig = YamlConfiguration.loadConfiguration(statsFile);
    }

    public void load() {
        loadRecycleData();
    }

    public void loadRecycleData() {
        recycleMap.clear();

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("recycle.items");
        String basePath = "recycle.items";

        if (section == null) {
            section = plugin.getConfig().getConfigurationSection("items");
            basePath = "items";
        }

        if (section == null) {
            plugin.getLogger().warning("No recyclable items found. Expected 'recycle.items' or legacy 'items'.");
            return;
        }

        for (String key : section.getKeys(false)) {
            if (key.equalsIgnoreCase("xp_rewards")) continue;

            String path = basePath + "." + key;

            Optional<Material> inputOpt = MaterialResolver.resolve(key);
            Optional<Material> outputOpt = MaterialResolver.resolve(plugin.getConfig().getString(path + ".material", ""));

            if (inputOpt.isEmpty()) {
                plugin.getLogger().warning("Invalid input material in config: " + key);
                continue;
            }

            if (outputOpt.isEmpty()) {
                plugin.getLogger().warning("Invalid output material in config: " + path + ".material");
                continue;
            }

            int min = plugin.getConfig().getInt(path + ".min", 0);
            int max = plugin.getConfig().getInt(path + ".max", 1);

            Material output = outputOpt.get();
            String rarity = plugin.getConfig().getString(path + ".rarity", resolveRarity(output));

            recycleMap.put(inputOpt.get(), new RecycleData(output, min, max, normalizeRarity(rarity)));
        }
    }

    public void loadStats() {
        recycledStats.clear();

        ConfigurationSection section = statsConfig.getConfigurationSection("players");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                int value = statsConfig.getInt("players." + key + ".recycled", 0);
                recycledStats.put(uuid, value);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void saveStats() {
        for (Map.Entry<UUID, Integer> entry : recycledStats.entrySet()) {
            statsConfig.set("players." + entry.getKey() + ".recycled", entry.getValue());
        }

        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save data/stats.yml");
            e.printStackTrace();
        }
    }

    public boolean isRecyclable(Material material) {
        return recycleMap.containsKey(material);
    }

    public RecycleData get(Material material) {
        return recycleMap.get(material);
    }

    public int size() {
        return recycleMap.size();
    }

    public long getLastUse(UUID uuid) {
        return cooldownMap.getOrDefault(uuid, 0L);
    }

    public void setLastUse(UUID uuid, long time) {
        cooldownMap.put(uuid, time);
    }

    public int getRecycledStats(UUID uuid) {
        return recycledStats.getOrDefault(uuid, 0);
    }

    public void addRecycledStats(UUID uuid, int amount) {
        recycledStats.put(uuid, getRecycledStats(uuid) + amount);
        saveStats();
    }

    private String resolveRarity(Material output) {
        String configured = plugin.getConfig().getString("recycle.rarity-by-output." + output.name());
        if (configured != null && !configured.isBlank()) return configured;

        return plugin.getConfig().getString("recycle.default-rarity", "COMMON");
    }

    private String normalizeRarity(String rarity) {
        if (rarity == null || rarity.isBlank()) return "COMMON";
        return rarity.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
}