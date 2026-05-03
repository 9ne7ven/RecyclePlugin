package com.svncho.gui;

import com.svncho.config.GuiConfig;
import com.svncho.config.LangManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GuiItemBuilder {

    private final LangManager lang;

    public GuiItemBuilder(LangManager lang) {
        this.lang = lang;
    }

    public ItemStack build(GuiConfig.GuiItemConfig config) {
        return build(config, Map.of(), null);
    }

    public ItemStack build(GuiConfig.GuiItemConfig config, Map<String, String> placeholders) {
        return build(config, placeholders, null);
    }

    public ItemStack build(GuiConfig.GuiItemConfig config, Map<String, String> placeholders, List<Component> overrideLore) {
        ItemStack item = new ItemStack(config.material(), config.amount());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(resolveName(config, placeholders).decoration(TextDecoration.ITALIC, config.italic()));

        List<Component> lore = overrideLore != null ? overrideLore : resolveLore(config, placeholders);
        if (!lore.isEmpty()) meta.lore(lore);

        if (config.enchanted()) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        if (config.hideAttributes()) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_PLACED_ON);
        }

        item.setItemMeta(meta);
        return item;
    }

    public List<Component> resolveLore(GuiConfig.GuiItemConfig config, Map<String, String> placeholders) {
        List<Component> lore = new ArrayList<>();

        for (String line : config.lore()) {
            lore.add(lang.rawComponent(applyPlaceholders(line, placeholders)).decoration(TextDecoration.ITALIC, config.italic()));
        }

        for (String key : config.loreKeys()) {
            lore.add(lang.rawComponent(applyPlaceholders(lang.get(key), placeholders)).decoration(TextDecoration.ITALIC, config.italic()));
        }

        return lore;
    }

    private Component resolveName(GuiConfig.GuiItemConfig config, Map<String, String> placeholders) {
        if (config.nameKey() != null && !config.nameKey().isBlank()) {
            return lang.rawComponent(applyPlaceholders(lang.get(config.nameKey()), placeholders));
        }
        return lang.rawComponent(applyPlaceholders(config.name() == null ? " " : config.name(), placeholders));
    }

    private String applyPlaceholders(String text, Map<String, String> placeholders) {
        if (text == null || placeholders == null || placeholders.isEmpty()) return text == null ? "" : text;

        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() == null ? "" : entry.getValue();
            result = result.replace("{" + key + "}", value);
            result = result.replace("%" + key + "%", value);
        }
        return result;
    }
}
