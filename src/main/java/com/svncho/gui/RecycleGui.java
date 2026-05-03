package com.svncho.gui;

import com.svncho.config.GuiConfig;
import com.svncho.config.LangManager;
import com.svncho.recycle.RecycleCalculator;
import com.svncho.recycle.RecycleData;
import com.svncho.recycle.RecycleManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RecycleGui {

    private final JavaPlugin plugin;
    private final LangManager lang;
    private final RecycleManager recycleManager;
    private final GuiConfig guiConfig;
    private final GuiSessionManager sessionManager;
    private final GuiItemBuilder itemBuilder;
    private final RecycleCalculator calculator;

    public RecycleGui(JavaPlugin plugin, LangManager lang, RecycleManager recycleManager, GuiConfig guiConfig, GuiSessionManager sessionManager) {
        this.plugin = plugin;
        this.lang = lang;
        this.recycleManager = recycleManager;
        this.guiConfig = guiConfig;
        this.sessionManager = sessionManager;
        this.itemBuilder = new GuiItemBuilder(lang);
        this.calculator = new RecycleCalculator(plugin);
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, guiConfig.getSize(), titleComponent());

        for (GuiConfig.GuiItemConfig itemConfig : guiConfig.getItems()) {
            inv.setItem(itemConfig.slot(), buildDynamicItem(player, itemConfig, null));
        }

        sessionManager.put(player.getUniqueId(), inv);
        player.openInventory(inv);
        playConfiguredSounds(player, "effects.open.sounds", Sound.UI_BUTTON_CLICK, 0.4f, 1f);
    }

    public void refresh(Player player, Inventory inv) {
        for (GuiConfig.GuiItemConfig itemConfig : guiConfig.getItems()) {
            inv.setItem(itemConfig.slot(), buildDynamicItem(player, itemConfig, inv));
        }
    }

    private Component titleComponent() {
        if (guiConfig.getTitle() != null && !guiConfig.getTitle().isBlank()) {
            return lang.rawComponent(guiConfig.getTitle());
        }
        return lang.component(guiConfig.getTitleKey());
    }

    private ItemStack buildDynamicItem(Player player, GuiConfig.GuiItemConfig config, Inventory inv) {
        if (config.dynamic() == GuiDynamicType.INFO) {
            return buildInfoItem(config, inv);
        }

        if (config.dynamic() == GuiDynamicType.STATS) {
            return itemBuilder.build(config, Map.of("recycled", String.valueOf(recycleManager.getRecycledStats(player.getUniqueId()))));
        }

        return itemBuilder.build(config);
    }

    private ItemStack buildInfoItem(GuiConfig.GuiItemConfig config, Inventory inv) {
        List<Component> lore;
        if (inv != null) {
            Map<Material, Integer> preview = previewGain(inv);
            int xp = previewXP(inv);

            if (!preview.isEmpty()) {
                List<Component> lines = new ArrayList<>();
                String lineFormat = plugin.getConfig().getString("gui.dynamic.info.preview-line-format", "§7- {material} x{amount}");
                for (Map.Entry<Material, Integer> entry : preview.entrySet()) {
                    lines.add(lang.rawComponent(lineFormat
                                    .replace("{material}", entry.getKey().name())
                                    .replace("{amount}", String.valueOf(entry.getValue())))
                            .decoration(TextDecoration.ITALIC, config.italic()));
                }

                for (String line : plugin.getConfig().getStringList("gui.dynamic.info.after-preview-lore")) {
                    lines.add(lang.rawComponent(line.replace("{xp}", String.valueOf(xp))).decoration(TextDecoration.ITALIC, config.italic()));
                }
                for (String key : plugin.getConfig().getStringList("gui.dynamic.info.after-preview-lore-keys")) {
                    lines.add(lang.rawComponent(lang.get(key).replace("{xp}", String.valueOf(xp))).decoration(TextDecoration.ITALIC, config.italic()));
                }
                lore = lines;
            } else {
                lore = buildConfiguredLore("gui.dynamic.info.empty-lore", "gui.dynamic.info.empty-lore-keys", config.italic());
            }
        } else {
            lore = buildConfiguredLore("gui.dynamic.info.empty-lore", "gui.dynamic.info.empty-lore-keys", config.italic());
        }

        return itemBuilder.build(config, Map.of(), lore);
    }

    private List<Component> buildConfiguredLore(String rawPath, String keyPath, boolean italic) {
        List<Component> lines = new ArrayList<>();
        for (String line : plugin.getConfig().getStringList(rawPath)) {
            lines.add(lang.rawComponent(line).decoration(TextDecoration.ITALIC, italic));
        }
        for (String key : plugin.getConfig().getStringList(keyPath)) {
            lines.add(lang.component(key).decoration(TextDecoration.ITALIC, italic));
        }
        return lines;
    }

    private Map<Material, Integer> previewGain(Inventory inv) {
        Map<Material, Integer> preview = new LinkedHashMap<>();

        for (int slot : guiConfig.getDepositSlots()) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;

            RecycleData data = recycleManager.get(item.getType());
            if (data == null) continue;

            int amount = calculator.calculateOutputAmount(item, data);
            preview.put(data.result(), preview.getOrDefault(data.result(), 0) + amount);
        }

        return preview;
    }

    private int previewXP(Inventory inv) {
        int total = 0;
        for (int slot : guiConfig.getDepositSlots()) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;
            RecycleData data = recycleManager.get(item.getType());
            if (data != null) total += calculator.calculateXP(item, data);
        }
        return total;
    }

    private Sound resolveSound(String name, Sound fallback) {
        try {
            NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase());
            Sound sound = Registry.SOUNDS.get(key);
            return sound != null ? sound : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private float parseFloat(Object obj, float fallback) {
        if (obj == null) return fallback;
        try {
            return Float.parseFloat(obj.toString());
        } catch (Exception e) {
            return fallback;
        }
    }

    private void playConfiguredSounds(Player player, String path, Sound fallback, float fallbackVolume, float fallbackPitch) {
        if (!plugin.getConfig().isList(path)) {
            player.playSound(player.getLocation(), fallback, fallbackVolume, fallbackPitch);
            return;
        }

        for (Map<?, ?> soundConfig : plugin.getConfig().getMapList(path)) {
            try {
                String soundName = String.valueOf(
                    soundConfig.containsKey("sound")
                        ? soundConfig.get("sound")
                        : Registry.SOUNDS.getKey(fallback).getKey()
                );

                float volume = parseFloat(soundConfig.get("volume"), fallbackVolume);
                float pitch = parseFloat(soundConfig.get("pitch"), fallbackPitch);

                Sound sound = resolveSound(soundName, fallback);
                player.playSound(player.getLocation(), sound, volume, pitch);

            } catch (Exception ignored) {
            }
        }
    }
}
