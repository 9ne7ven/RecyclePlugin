package com.svncho;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class RecyclePlugin extends JavaPlugin implements Listener {

    // -----------------------------------------------------------------------
    // Slots — 54-slot standalone recycle UI
    // -----------------------------------------------------------------------

    private static final Set<Integer> DEPOSIT_SLOTS = new HashSet<>();
    static {
        for (int i = 0; i <= 44; i++) DEPOSIT_SLOTS.add(i);
    }

    private static final int SLOT_CONFIRM = 46;
    private static final int SLOT_INFO    = 48;
    private static final int SLOT_STATS   = 50;
    private static final int SLOT_BACK    = 52;

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private final Map<Material, RecycleData> recycleMap      = new HashMap<>();
    private final Map<UUID, Inventory>       recycleGUIMap   = new HashMap<>();
    private final Set<UUID>                  confirmingClose = new HashSet<>();
    private final Map<UUID, Long>            cooldownMap     = new HashMap<>();
    private final Map<UUID, Integer>         recycledStats   = new HashMap<>();

    private FileConfiguration messages;
    private String activeLanguage = "en";

    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    // -----------------------------------------------------------------------
    // Plugin lifecycle
    // -----------------------------------------------------------------------

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadMessages();
        loadRecycleData();

        Bukkit.getPluginManager().registerEvents(this, this);

        if (getCommand("recycle") != null) getCommand("recycle").setExecutor(this);
        if (getCommand("rpadmin") != null) getCommand("rpadmin").setExecutor(this);

        logStartup();
    }

    @Override
    public void onDisable() {
        logShutdown();
    }

    // -----------------------------------------------------------------------
    // Commands
    // -----------------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("recycle")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be used by a player.");
                return true;
            }

            if (!player.hasPermission("recycleplugin.use")) {
                player.sendMessage(msgComponent("no-permission"));
                return true;
            }

            openRecycleGUI(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("rpadmin")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("recycleplugin.reload")) {
                    sender.sendMessage(msgComponent("no-permission"));
                    return true;
                }

                reloadConfig();
                loadMessages();
                loadRecycleData();
                sender.sendMessage(msgComponent("plugin-reloaded"));
                logReload();
                return true;
            }

            sender.sendMessage("§cUsage: /rpadmin reload");
            return true;
        }

        return false;
    }

    // -----------------------------------------------------------------------
    // Language system
    // -----------------------------------------------------------------------

    private void loadMessages() {
        String configuredLang = getConfig().getString("language", "en");
        String fallbackLang   = getConfig().getString("fallback-language", "en");

        File langFolder = new File(getDataFolder(), "lang");
        if (!langFolder.exists() && !langFolder.mkdirs()) {
            getLogger().warning("Could not create lang folder.");
        }

        // Always make sure bundled default langs exist in plugin folder
        saveBundledLangIfMissing("en");
        saveBundledLangIfMissing("fr");
        saveBundledLangIfMissing("de");
        saveBundledLangIfMissing("es");
        saveBundledLangIfMissing("br");
        saveBundledLangIfMissing("zh");

        String langToLoad = resolveLanguage(configuredLang, fallbackLang);
        File file = new File(langFolder, "messages_" + langToLoad + ".yml");

        // Create selected lang if bundled and missing
        saveBundledLangIfMissing(langToLoad);

        // Add missing keys from bundled resource without overwriting existing translations
        syncMissingLangKeys(langToLoad);

        messages = YamlConfiguration.loadConfiguration(file);
        activeLanguage = langToLoad;
    }

    private void saveBundledLangIfMissing(String lang) {
        File file = new File(getDataFolder(), "lang/messages_" + lang + ".yml");
        String resourcePath = "lang/messages_" + lang + ".yml";

        if (!file.exists() && getResource(resourcePath) != null) {
            saveResource(resourcePath, false);
        }
    }

    private void syncMissingLangKeys(String lang) {
        File file = new File(getDataFolder(), "lang/messages_" + lang + ".yml");
        String resourcePath = "lang/messages_" + lang + ".yml";

        if (!file.exists() || getResource(resourcePath) == null) return;

        YamlConfiguration userLang = YamlConfiguration.loadConfiguration(file);

        try (java.io.InputStream stream = getResource(resourcePath);
            java.io.InputStreamReader reader = new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)) {

            YamlConfiguration bundledLang = YamlConfiguration.loadConfiguration(reader);
            boolean changed = false;

            for (String key : bundledLang.getKeys(true)) {
                if (!bundledLang.isConfigurationSection(key) && !userLang.contains(key)) {
                    userLang.set(key, bundledLang.get(key));
                    changed = true;
                }
            }

            if (changed) {
                userLang.save(file);
                getLogger().info("Updated missing language keys for: " + lang);
            }

        } catch (Exception e) {
            getLogger().warning("Could not sync language file: messages_" + lang + ".yml");
            e.printStackTrace();
        }
    }

    private String resolveLanguage(String configuredLang, String fallbackLang) {
        if (hasBundledLang(configuredLang) || hasExternalLang(configuredLang)) return configuredLang;
        getLogger().warning("Unknown language detected: " + configuredLang);

        if (hasBundledLang(fallbackLang) || hasExternalLang(fallbackLang)) {
            getLogger().warning("Falling back to: " + fallbackLang);
            getConfig().set("language", fallbackLang);
            saveConfig();
            return fallbackLang;
        }

        getLogger().warning("Fallback language invalid. Defaulting to: en");
        getConfig().set("language", "en");
        saveConfig();
        return "en";
    }

    private boolean hasBundledLang(String lang) {
        return getResource("lang/messages_" + lang + ".yml") != null;
    }

    private boolean hasExternalLang(String lang) {
        return new File(getDataFolder(), "lang/messages_" + lang + ".yml").exists();
    }

    private String msg(String key, String... replacements) {
        String value = messages != null ? messages.getString(key) : null;
        if (value == null) value = "§cMissing: " + key;

        for (int i = 0; i + 1 < replacements.length; i += 2) {
            value = value.replace("%" + replacements[i] + "%", replacements[i + 1]);
            value = value.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return value;
    }

    private Component msgComponent(String key, String... replacements) {
        return serializer.deserialize(msg(key, replacements));
    }

    // -----------------------------------------------------------------------
    // Config loading
    // Supports both old config.yml: items.* and newer config.yml: recycle.items.*
    // -----------------------------------------------------------------------

    public void loadRecycleData() {
        recycleMap.clear();

        ConfigurationSection section = getConfig().getConfigurationSection("recycle.items");
        String basePath = "recycle.items";

        if (section == null) {
            section = getConfig().getConfigurationSection("items");
            basePath = "items";
        }

        if (section == null) {
            getLogger().warning("No recyclable items found. Expected 'recycle.items' or legacy 'items'.");
            return;
        }

        for (String key : section.getKeys(false)) {
            if (key.equalsIgnoreCase("xp_rewards")) continue;

            String path = basePath + "." + key;
            Optional<Material> inputOpt = resolveMaterial(key);
            Optional<Material> outputOpt = resolveMaterial(getConfig().getString(path + ".material", ""));

            if (inputOpt.isEmpty()) {
                getLogger().warning("Invalid input material in config: " + key);
                continue;
            }
            if (outputOpt.isEmpty()) {
                getLogger().warning("Invalid output material in config: " + path + ".material");
                continue;
            }

            int min = getConfig().getInt(path + ".min", 0);
            int max = getConfig().getInt(path + ".max", 1);

            Material output = outputOpt.get();
            recycleMap.put(inputOpt.get(), new RecycleData(output, min, max, rarityOf(output)));
        }
    }

    private Optional<Material> resolveMaterial(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();

        String normalized = raw.trim()
                .toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_')
                .replace("MINECRAFT:", "");

        // Friendly legacy aliases / common mistakes
        normalized = switch (normalized) {
            case "WOOD_SWORD"      -> "WOODEN_SWORD";
            case "WOOD_PICKAXE"    -> "WOODEN_PICKAXE";
            case "WOOD_AXE"        -> "WOODEN_AXE";
            case "WOOD_SHOVEL"     -> "WOODEN_SHOVEL";
            case "WOOD_HOE"        -> "WOODEN_HOE";
            case "GOLD_SWORD"      -> "GOLDEN_SWORD";
            case "GOLD_PICKAXE"    -> "GOLDEN_PICKAXE";
            case "GOLD_AXE"        -> "GOLDEN_AXE";
            case "GOLD_SHOVEL"     -> "GOLDEN_SHOVEL";
            case "GOLD_HOE"        -> "GOLDEN_HOE";
            case "GOLD_HELMET"     -> "GOLDEN_HELMET";
            case "GOLD_CHESTPLATE" -> "GOLDEN_CHESTPLATE";
            case "GOLD_LEGGINGS"   -> "GOLDEN_LEGGINGS";
            case "GOLD_BOOTS"      -> "GOLDEN_BOOTS";
            default                 -> normalized;
        };

        Material material = Material.matchMaterial(normalized);
        return material == null ? Optional.empty() : Optional.of(material);
    }

    private String rarityOf(Material output) {
        return switch (output) {
            case IRON_INGOT      -> "IRON";
            case COPPER_INGOT    -> "COPPER";
            case GOLD_INGOT      -> "GOLD";
            case DIAMOND         -> "DIAMOND";
            case NETHERITE_INGOT -> "NETHERITE";
            default              -> "COMMON";
        };
    }

    // -----------------------------------------------------------------------
    // GUI
    // -----------------------------------------------------------------------

    public void openRecycleGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, msgComponent("recycle-gui-title"));

        ItemStack pane = makePane(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 45; i <= 53; i++) {
            if (i != SLOT_CONFIRM && i != SLOT_INFO && i != SLOT_STATS && i != SLOT_BACK) {
                inv.setItem(i, pane);
            }
        }

        inv.setItem(SLOT_CONFIRM, buildConfirmButton());
        inv.setItem(SLOT_INFO,    buildInfoItem(null));
        inv.setItem(SLOT_STATS,   buildStatsItem(player));
        inv.setItem(SLOT_BACK,    buildBackButton());

        recycleGUIMap.put(player.getUniqueId(), inv);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1f);
    }

    // -----------------------------------------------------------------------
    // Events
    // -----------------------------------------------------------------------

    @EventHandler
    public void onRecycleClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();

        Inventory recycleInv = recycleGUIMap.get(uuid);
        if (recycleInv == null) return;
        if (!e.getView().getTopInventory().equals(recycleInv)) return;

        boolean clickedTop = e.getClickedInventory() != null && e.getClickedInventory().equals(recycleInv);
        boolean clickedBottom = e.getClickedInventory() != null && e.getClickedInventory().equals(e.getView().getBottomInventory());

        if (clickedBottom && e.isShiftClick()) {
            e.setCancelled(true);

            ItemStack item = e.getCurrentItem();
            if (item == null || item.getType() == Material.AIR) return;

            if (!recycleMap.containsKey(item.getType())) {
                player.sendMessage(msgComponent("recycle-non-recyclable"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
                return;
            }

            ItemStack clone = item.clone();
            int before = clone.getAmount();
            int leftover = addToDepositSlots(recycleInv, clone);
            int moved = before - leftover;

            if (moved > 0) {
                item.setAmount(item.getAmount() - moved);
                if (item.getAmount() <= 0) e.getClickedInventory().setItem(e.getSlot(), null);
                refreshInfo(player, recycleInv);
            }
            return;
        }

        if (!clickedTop) return;

        int slot = e.getSlot();

        if (!DEPOSIT_SLOTS.contains(slot) && slot != SLOT_CONFIRM && slot != SLOT_INFO && slot != SLOT_STATS && slot != SLOT_BACK) {
            e.setCancelled(true);
            return;
        }

        if (slot == SLOT_CONFIRM) {
            e.setCancelled(true);
            handleRecycle(player, recycleInv);
            return;
        }

        if (slot == SLOT_INFO || slot == SLOT_STATS) {
            e.setCancelled(true);
            return;
        }

        // Standalone behavior: back just closes and returns deposited items.
        if (slot == SLOT_BACK) {
            e.setCancelled(true);
            returnDepositItems(player, recycleInv);
            confirmingClose.add(uuid);
            player.closeInventory();
            return;
        }

        Bukkit.getScheduler().runTask(this, () -> refreshInfo(player, recycleInv));
    }

    @EventHandler
    public void onRecycleDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();

        Inventory recycleInv = recycleGUIMap.get(uuid);
        if (recycleInv == null) return;
        if (!e.getInventory().equals(recycleInv)) return;

        for (int rawSlot : e.getRawSlots()) {
            if (rawSlot < 54 && !DEPOSIT_SLOTS.contains(rawSlot)) {
                e.setCancelled(true);
                return;
            }
        }

        Bukkit.getScheduler().runTask(this, () -> refreshInfo(player, recycleInv));
    }

    @EventHandler
    public void onRecycleClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();

        Inventory recycleInv = recycleGUIMap.get(uuid);
        if (recycleInv == null) return;
        if (!e.getInventory().equals(recycleInv)) return;

        recycleGUIMap.remove(uuid);
        if (confirmingClose.remove(uuid)) return;

        returnDepositItems(player, recycleInv);
    }

    // -----------------------------------------------------------------------
    // Recycling logic
    // -----------------------------------------------------------------------

    private void handleRecycle(Player player, Inventory inv) {
        boolean hasValid = false;
        boolean hasInvalid = false;

        for (int slot : DEPOSIT_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;
            if (recycleMap.containsKey(item.getType())) hasValid = true;
            else hasInvalid = true;
        }

        if (!hasValid) {
            player.sendMessage(msgComponent("recycle-non-recyclable"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
            return;
        }

        if (hasInvalid) {
            player.sendMessage(msgComponent("recycle-invalid-item-present"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
            return;
        }

        long cooldownMs = getConfig().getLong("recycle.cooldown-seconds", getConfig().getLong("cooldown-seconds", 0)) * 1000L;
        long lastUse = cooldownMap.getOrDefault(player.getUniqueId(), 0L);
        long remaining = (lastUse + cooldownMs - System.currentTimeMillis()) / 1000L;
        if (remaining > 0) {
            player.sendMessage(msgComponent("recycle-cooldown", "seconds", String.valueOf(remaining)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
            return;
        }

        double luckyChance = getConfig().getDouble("recycle.lucky-chance", getConfig().getDouble("lucky-chance", 0.0));
        boolean lucky = luckyChance > 0 && ThreadLocalRandom.current().nextDouble() * 100 < luckyChance;
        if (lucky) player.sendMessage(msgComponent("recycle-lucky"));

        int totalXP = 0;
        int totalItemsRecycled = 0;
        Map<Material, Integer> gained = new LinkedHashMap<>();

        for (int slot : DEPOSIT_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;

            RecycleData data = recycleMap.get(item.getType());
            if (data == null) continue;

            int amount = calculateOutputAmount(item, data);
            if (lucky) amount *= 2;

            if (amount > 0) {
                Map<Integer, ItemStack> leftover = player.getInventory()
                        .addItem(new ItemStack(data.result, amount));

                leftover.values().forEach(stack ->
                        player.getWorld().dropItemNaturally(player.getLocation(), stack));

                gained.put(
                        data.result,
                        gained.getOrDefault(data.result, 0) + amount
                );
            }

            totalXP += calculateXP(item, data);
            totalItemsRecycled += item.getAmount();
            inv.setItem(slot, null);
        }

        if (cooldownMs > 0) cooldownMap.put(player.getUniqueId(), System.currentTimeMillis());
        int current = recycledStats.getOrDefault(player.getUniqueId(), 0);
        recycledStats.put(player.getUniqueId(), current + totalItemsRecycled);

        StringBuilder recap = new StringBuilder(msg("recycle-recap-title")).append("\n");
        for (Map.Entry<Material, Integer> entry : gained.entrySet()) {
            recap.append("§7- ").append(entry.getKey().name()).append(" x").append(entry.getValue()).append("\n");
        }
        recap.append("\n").append(msg("recycle-total-xp")).append(totalXP);
        player.sendMessage(serializer.deserialize(recap.toString()));

        if (totalXP > 0) player.giveExp(totalXP);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 20, 0.4, 0.4, 0.4, 0.05);

        confirmingClose.add(player.getUniqueId());
        player.closeInventory();
    }

    // -----------------------------------------------------------------------
    // Calculations
    // -----------------------------------------------------------------------

    private int calculateXP(ItemStack item, RecycleData data) {
        String prefix = getConfig().contains("recycle.xp-rewards") ? "recycle.xp-rewards." : "items.xp_rewards.";

        int baseXP = switch (data.rarity) {
            case "IRON"      -> getConfig().getInt(prefix + "iron_xp", 20);
            case "COPPER"    -> getConfig().getInt(prefix + "copper_xp", 15);
            case "GOLD"      -> getConfig().getInt(prefix + "gold_xp", 25);
            case "DIAMOND"   -> getConfig().getInt(prefix + "diamond_xp", 40);
            case "NETHERITE" -> getConfig().getInt(prefix + "netherite_xp", 60);
            default           -> getConfig().getInt(prefix + "common_xp", 10);
        };

        int enchantXpPerLevel = getConfig().getInt("recycle.enchanted-xp-per-level", -1);
        if (enchantXpPerLevel >= 0) {
            int enchantLevels = item.getEnchantments().values().stream().mapToInt(Integer::intValue).sum();
            return baseXP + enchantLevels * enchantXpPerLevel;
        }

        int oldEnchantBonus = item.getEnchantments().isEmpty() ? 0 : getConfig().getInt("enchanted-xp", 50);
        return baseXP + oldEnchantBonus;
    }

    private int calculateOutputAmount(ItemStack item, RecycleData data) {
        int min = Math.min(data.minAmount, data.maxAmount);
        int max = Math.max(data.minAmount, data.maxAmount);

        if (min == max) return min;

        int maxDurability = item.getType().getMaxDurability();
        if (maxDurability <= 0 || !(item.getItemMeta() instanceof Damageable damageable)) {
            return ThreadLocalRandom.current().nextInt(min, max + 1);
        }

        double durabilityPct = (double) (maxDurability - damageable.getDamage()) / maxDurability;
        double range = max - min;
        double center = min + durabilityPct * range;
        double noise = (ThreadLocalRandom.current().nextDouble() - 0.5) * range * 0.5;

        int result = (int) Math.round(center + noise);
        return Math.max(min, Math.min(max, result));
    }

    // -----------------------------------------------------------------------
    // Preview helpers
    // -----------------------------------------------------------------------

    private void refreshInfo(Player player, Inventory inv) {
        inv.setItem(SLOT_INFO, buildInfoItem(inv));
        inv.setItem(SLOT_CONFIRM, buildConfirmButton());
        inv.setItem(SLOT_STATS, buildStatsItem(player));
    }

    private Map<Material, Integer> previewGain(Inventory inv) {
        Map<Material, Integer> preview = new LinkedHashMap<>();

        for (int slot : DEPOSIT_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;

            RecycleData data = recycleMap.get(item.getType());
            if (data == null) continue;

            int amount = calculateOutputAmount(item, data);

            preview.put(
                    data.result,
                    preview.getOrDefault(data.result, 0) + amount
            );
        }

        return preview;
    }

    private int previewXP(Inventory inv) {
        int total = 0;
        for (int slot : DEPOSIT_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;
            RecycleData data = recycleMap.get(item.getType());
            if (data != null) total += calculateXP(item, data);
        }
        return total;
    }

    // -----------------------------------------------------------------------
    // Deposit helpers
    // -----------------------------------------------------------------------

    private int addToDepositSlots(Inventory inv, ItemStack item) {
        for (int slot : DEPOSIT_SLOTS) {
            ItemStack existing = inv.getItem(slot);
            if (existing != null && existing.isSimilar(item) && existing.getAmount() < existing.getMaxStackSize()) {
                int space = existing.getMaxStackSize() - existing.getAmount();
                int toAdd = Math.min(space, item.getAmount());
                existing.setAmount(existing.getAmount() + toAdd);
                item.setAmount(item.getAmount() - toAdd);
                if (item.getAmount() <= 0) return 0;
            }
        }

        for (int slot : DEPOSIT_SLOTS) {
            ItemStack existing = inv.getItem(slot);
            if (existing == null || existing.getType() == Material.AIR) {
                int toAdd = Math.min(item.getMaxStackSize(), item.getAmount());
                ItemStack clone = item.clone();
                clone.setAmount(toAdd);
                inv.setItem(slot, clone);
                item.setAmount(item.getAmount() - toAdd);
                if (item.getAmount() <= 0) return 0;
            }
        }

        return item.getAmount();
    }

    private void returnDepositItems(Player player, Inventory inv) {
        for (int slot : DEPOSIT_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
            leftover.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
            inv.setItem(slot, null);
        }
    }

    // -----------------------------------------------------------------------
    // GUI item builders
    // -----------------------------------------------------------------------

    private ItemStack buildInfoItem(Inventory inv) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(msgComponent("recycle-info-title").decoration(TextDecoration.ITALIC, false));

        java.util.List<Component> lore = new java.util.ArrayList<>();
        if (inv != null) {
            Map<Material, Integer> preview = previewGain(inv);
            int xp = previewXP(inv);

            if (!preview.isEmpty()) {
                for (Map.Entry<Material, Integer> entry : preview.entrySet()) {
                    lore.add(Component.text("§7- " + entry.getKey().name() + " x" + entry.getValue()).decoration(TextDecoration.ITALIC, false));
                }
                lore.add(Component.empty());
                lore.add(msgComponent("recycle-xp-estimated").append(Component.text(xp)).decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(msgComponent("recycle-info-empty-1").decoration(TextDecoration.ITALIC, false));
                lore.add(msgComponent("recycle-info-empty-2").decoration(TextDecoration.ITALIC, false));
            }
        } else {
            lore.add(msgComponent("recycle-info-empty-1").decoration(TextDecoration.ITALIC, false));
            lore.add(msgComponent("recycle-info-empty-2").decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildConfirmButton() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(msgComponent("recycle-confirm-title").decoration(TextDecoration.ITALIC, false));
        meta.lore(java.util.List.of(msgComponent("recycle-confirm-lore").decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildStatsItem(Player player) {
        ItemStack item = new ItemStack(Material.ANVIL);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        int recycled = recycledStats.getOrDefault(player.getUniqueId(), 0);
        meta.displayName(msgComponent("recycle-stats-title").color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
        meta.lore(java.util.List.of(
                Component.text("♻ " + recycled, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                msgComponent("recycle-stats-lore").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildBackButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(msgComponent("recycle-back-title").color(NamedTextColor.RED).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
        meta.lore(java.util.List.of(msgComponent("recycle-back-lore").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makePane(Material mat, String name) {
        ItemStack pane = new ItemStack(mat);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
            pane.setItemMeta(meta);
        }
        return pane;
    }

    // -----------------------------------------------------------------------
    // Logs
    // -----------------------------------------------------------------------

    private void logStartup() {
        getLogger().info("");
        getLogger().info("RecyclePlugin v" + getPluginMeta().getVersion());
        getLogger().info("    | Author: svncho_");
        getLogger().info("    | Loaded items: " + recycleMap.size());
        getLogger().info("    | Language: " + activeLanguage);
        getLogger().info("");
    }

    private void logReload() {
        getLogger().info("");
        getLogger().info("RecyclePlugin v" + getPluginMeta().getVersion() + " reloaded");
        getLogger().info("    | Loaded items: " + recycleMap.size());
        getLogger().info("    | Language: " + activeLanguage);
        getLogger().info("");
    }

    private void logShutdown() {
        getLogger().info("RecyclePlugin disabled.");
    }

    // -----------------------------------------------------------------------
    // Data
    // -----------------------------------------------------------------------

    private static class RecycleData {
        final Material result;
        final int minAmount;
        final int maxAmount;
        final String rarity;

        RecycleData(Material result, int minAmount, int maxAmount, String rarity) {
            this.result = result;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.rarity = rarity;
        }
    }
}
