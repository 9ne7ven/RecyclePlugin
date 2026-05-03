package com.svncho.config;

import com.svncho.gui.GuiAction;
import com.svncho.gui.GuiDynamicType;
import com.svncho.util.MaterialResolver;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class GuiConfig {

    private final JavaPlugin plugin;

    private String titleKey;
    private String title;
    private int size;
    private final Set<Integer> depositSlots = new LinkedHashSet<>();
    private final Map<Integer, GuiItemConfig> itemsBySlot = new HashMap<>();
    private final List<GuiItemConfig> items = new ArrayList<>();

    public GuiConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        depositSlots.clear();
        itemsBySlot.clear();
        items.clear();

        titleKey = plugin.getConfig().getString("gui.title-key", "recycle-gui-title");
        title = plugin.getConfig().getString("gui.title", null);
        size = normalizeSize(plugin.getConfig().getInt("gui.size", 54));

        loadDepositSlots();
        loadGuiItems();
    }

    private int normalizeSize(int requestedSize) {
        if (requestedSize <= 0) return 54;
        int normalized = ((requestedSize + 8) / 9) * 9;
        return Math.max(9, Math.min(54, normalized));
    }

    private void loadDepositSlots() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("gui.deposit-slots");

        if (section == null) {
            addSlotRange(0, Math.min(44, size - 1));
            return;
        }

        if (section.isList("slots")) {
            for (int slot : section.getIntegerList("slots")) addDepositSlot(slot);
        } else {
            addSlotRange(section.getInt("from", 0), section.getInt("to", Math.min(44, size - 1)));
        }

        for (String range : section.getStringList("ranges")) addSlotRange(range);
        for (int slot : section.getIntegerList("extra")) addDepositSlot(slot);
        for (int slot : section.getIntegerList("remove")) depositSlots.remove(slot);
    }

    private void addSlotRange(String rawRange) {
        if (rawRange == null || rawRange.isBlank()) return;
        String normalized = rawRange.trim().replace(" ", "");
        String[] parts = normalized.split("-", 2);
        try {
            if (parts.length == 1) {
                addDepositSlot(Integer.parseInt(parts[0]));
                return;
            }
            addSlotRange(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        } catch (NumberFormatException ignored) {
            plugin.getLogger().warning("Invalid GUI deposit slot range: " + rawRange);
        }
    }

    private void addSlotRange(int from, int to) {
        int start = Math.min(from, to);
        int end = Math.max(from, to);
        for (int slot = start; slot <= end; slot++) addDepositSlot(slot);
    }

    private void addDepositSlot(int slot) {
        if (slot >= 0 && slot < size) depositSlots.add(slot);
    }

    private void loadGuiItems() {
        ConfigurationSection itemSection = plugin.getConfig().getConfigurationSection("gui.items");
        if (itemSection == null) return;

        for (String id : itemSection.getKeys(false)) {
            String path = "gui.items." + id;
            int slot = plugin.getConfig().getInt(path + ".slot", -1);
            if (slot < 0 || slot >= size) {
                plugin.getLogger().warning("Ignoring GUI item with invalid slot: " + id);
                continue;
            }

            Material material = MaterialResolver.resolve(plugin.getConfig().getString(path + ".material", "STONE")).orElse(Material.STONE);
            int amount = Math.max(1, Math.min(64, plugin.getConfig().getInt(path + ".amount", 1)));
            String nameKey = plugin.getConfig().getString(path + ".name-key", "");
            String name = plugin.getConfig().getString(path + ".name", null);
            List<String> loreKeys = plugin.getConfig().getStringList(path + ".lore-keys");
            List<String> lore = plugin.getConfig().getStringList(path + ".lore");
            boolean enchanted = plugin.getConfig().getBoolean(path + ".enchanted", false);
            boolean hideAttributes = plugin.getConfig().getBoolean(path + ".hide-attributes", true);
            boolean italic = plugin.getConfig().getBoolean(path + ".italic", false);
            GuiAction action = GuiAction.from(plugin.getConfig().getString(path + ".action", "NONE"));
            GuiDynamicType dynamic = GuiDynamicType.from(plugin.getConfig().getString(path + ".dynamic", "NONE"));
            if (dynamic == GuiDynamicType.NONE && action == GuiAction.INFO) dynamic = GuiDynamicType.INFO;

            GuiItemConfig config = new GuiItemConfig(
                    id, slot, material, amount, nameKey, name, loreKeys, lore,
                    enchanted, hideAttributes, italic, action, dynamic
            );
            items.add(config);
            itemsBySlot.put(slot, config);
        }
    }

    public String getTitleKey() {
        return titleKey;
    }

    public String getTitle() {
        return title;
    }

    public int getSize() {
        return size;
    }

    public Set<Integer> getDepositSlots() {
        return Collections.unmodifiableSet(depositSlots);
    }

    public List<GuiItemConfig> getItems() {
        return Collections.unmodifiableList(items);
    }

    public GuiItemConfig getItemBySlot(int slot) {
        return itemsBySlot.get(slot);
    }

    public boolean isDepositSlot(int slot) {
        return depositSlots.contains(slot);
    }

    public record GuiItemConfig(
            String id,
            int slot,
            Material material,
            int amount,
            String nameKey,
            String name,
            List<String> loreKeys,
            List<String> lore,
            boolean enchanted,
            boolean hideAttributes,
            boolean italic,
            GuiAction action,
            GuiDynamicType dynamic
    ) {}
}
