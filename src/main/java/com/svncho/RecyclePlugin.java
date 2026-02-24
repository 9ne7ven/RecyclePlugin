package com.svncho;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class RecyclePlugin extends JavaPlugin implements Listener {

    private final Map<Material, RecycleData> recycleMap = new HashMap<>();
    private final Map<UUID, Boolean> recycleConfirmed = new HashMap<>();
    private FileConfiguration messages;
    private String activeLanguage;

    private final LegacyComponentSerializer serializer =
            LegacyComponentSerializer.legacySection();

    // =========================================================
    // CONSOLE COLOR SYSTEM (ANSI)
    // =========================================================

    private static final String RESET = "\u001B[0m";
    private static final String DARK_BLUE = "\u001B[34m";
    private static final String DARK_PURPLE = "\u001B[35m";
    private static final String PINK = "\u001B[95m";

    private boolean supportsAnsi() {
        return System.console() != null;
    }

    private String color(String color, String message) {
        if (supportsAnsi()) {
            return color + message + RESET;
        }
        return message;
    }
    
    @Override
    public void onEnable() {

        Bukkit.getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
        loadMessages();
        loadRecycleData();
        this.getCommand("recycle").setExecutor(this);
        this.getCommand("rpadmin").setExecutor(this);
        logStartup();
    }

    @Override
    public void onDisable() {
        logShutdown();
    }

    // =========================================================
    // LANGUAGE SYSTEM
    // =========================================================

    private void loadMessages() {

        String configuredLang = getConfig().getString("language", "en");
        String fallbackLang = getConfig().getString("fallback-language", "en");

        File langFolder = new File(getDataFolder(), "lang");
        if (!langFolder.exists()) langFolder.mkdirs();

        String langToLoad = configuredLang;

        if (getResource("lang/messages_" + configuredLang + ".yml") == null) {

            getLogger().warning(color(DARK_PURPLE,
                    "Unknown language detected: " + configuredLang));

            if (getResource("lang/messages_" + fallbackLang + ".yml") != null) {

                getLogger().warning(color(DARK_BLUE,
                        "Falling back to: " + fallbackLang));

                langToLoad = fallbackLang;

            } else {

                getLogger().warning(color(DARK_BLUE,
                        "Fallback language invalid. Defaulting to: en"));

                langToLoad = "en";
            }

            getConfig().set("language", langToLoad);
            saveConfig();

            getLogger().info(color(PINK,
                    "Configuration updated automatically to language: " + langToLoad));
        }

        File file = new File(langFolder, "messages_" + langToLoad + ".yml");

        if (!file.exists()) {
            saveResource("lang/messages_" + langToLoad + ".yml", false);
        }

        messages = YamlConfiguration.loadConfiguration(file);
        activeLanguage = langToLoad;
    }

    private String msg(String key) {
        return messages.getString(key, "§cMissing: " + key);
    }

    private Component msgComponent(String key) {
        return serializer.deserialize(msg(key));
    }

    // =========================================================
    // CONFIG LOADING
    // =========================================================

    private void loadRecycleData() {
        recycleMap.clear();

        if (!getConfig().contains("items")) return;

        for (String key : getConfig().getConfigurationSection("items").getKeys(false)) {

            if (key.equalsIgnoreCase("xp_rewards")) continue;

            try {
                String path = "items." + key;

                Material input = Material.valueOf(key.toUpperCase());
                Material result = Material.valueOf(
                        getConfig().getString(path + ".material").toUpperCase());

                int min = getConfig().getInt(path + ".min");
                int max = getConfig().getInt(path + ".max");

                String rarity = switch (result) {
                    case IRON_INGOT -> "IRON";
                    case COPPER_INGOT -> "COPPER";
                    case GOLD_INGOT -> "GOLD";
                    case DIAMOND -> "DIAMOND";
                    case NETHERITE_INGOT -> "NETHERITE";
                    default -> "COMMON";
                };

                recycleMap.put(input, new RecycleData(result, min, max, rarity));

            } 
            catch (Exception e) {
                getLogger().warning("Invalid item in config: " + key);
            }
        }
    }

    // =========================================================
    // COMMANDS
    // =========================================================

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (command.getName().equalsIgnoreCase("recycle")) {

            if (!(sender instanceof Player player)) return true;

            if (!player.hasPermission("recycleplugin.use")) {
                player.sendMessage(
                        Component.text("You do not have permission to use this command.")
                );
                return true;
            }

            openRecycleGUI(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("rpadmin")
                && args.length == 1
                && args[0].equalsIgnoreCase("reload")) {

            if (!sender.hasPermission("recycleplugin.reload")) {
                sender.sendMessage(msgComponent("no-permission"));
                return true;
            }

            reloadConfig();
            loadMessages();
            loadRecycleData();

            logReload();

            sender.sendMessage(msgComponent("plugin-reloaded"));
            return true;
        }
        return false;
    }

    // =========================================================
    // GUI
    // =========================================================

    private void openRecycleGUI(Player player) {

        Inventory inv = Bukkit.createInventory(
                new RecycleHolder(),
                18,
                msgComponent("gui-title")
        );

        ItemStack accept = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta aMeta = accept.getItemMeta();
        aMeta.displayName(msgComponent("accept"));
        accept.setItemMeta(aMeta);

        ItemStack refuse = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta rMeta = refuse.getItemMeta();
        rMeta.displayName(msgComponent("refuse"));
        refuse.setItemMeta(rMeta);

        inv.setItem(0, accept);
        inv.setItem(8, refuse);

        for (int i = 9; i <= 17; i++) {
            inv.setItem(i, createEmptyGlass());
        }

        player.openInventory(inv);
        recycleConfirmed.put(player.getUniqueId(), false);
    }

    // =========================================================
    // LISTENERS
    // =========================================================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (!(event.getView().getTopInventory().getHolder() instanceof RecycleHolder)) return;

        Player player = (Player) event.getWhoClicked();
        if (event.getClickedInventory() == null) return;

        if (event.isShiftClick()) {
            event.setCancelled(true);
            return;
        }

        if (event.getClickedInventory().equals(event.getView().getTopInventory())) {

            int slot = event.getSlot();

            if (slot >= 9 && slot <= 17) {
                event.setCancelled(true);
                return;
            }

            if (slot == 0) {
                event.setCancelled(true);

                boolean success = handleRecycle(player);

                if (success) {
                    recycleConfirmed.put(player.getUniqueId(), true);
                    player.closeInventory();
                }

                return;
            }

            if (slot == 8) {
                event.setCancelled(true);
                player.closeInventory();
                return;
            }
        }

        Bukkit.getScheduler().runTask(this,
                () -> updatePreview(event.getView().getTopInventory()));
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {

        if (!(event.getView().getTopInventory().getHolder() instanceof RecycleHolder)) return;

        Player player = (Player) event.getPlayer();
        Inventory inv = event.getView().getTopInventory();

        boolean confirmed = recycleConfirmed.getOrDefault(player.getUniqueId(), false);

        if (!confirmed) {
            for (int i = 1; i <= 7; i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    player.getInventory().addItem(item);
                }
            }
        }

        recycleConfirmed.remove(player.getUniqueId());
    }

    // =========================================================
    // PREVIEW UPDATE
    // =========================================================

    private void updatePreview(Inventory inv) {

        for (int i = 1; i <= 7; i++) {

            ItemStack item = inv.getItem(i);
            int previewSlot = i + 9;

            if (item == null || item.getType() == Material.AIR) {
                inv.setItem(previewSlot, createEmptyGlass());
                continue;
            }

            RecycleData data = recycleMap.get(item.getType());

            if (data == null) {
                ItemStack barrier = new ItemStack(Material.BARRIER);
                ItemMeta meta = barrier.getItemMeta();
                meta.displayName(msgComponent("non-recyclable"));
                barrier.setItemMeta(meta);
                inv.setItem(previewSlot, barrier);
                continue;
            }

            int amount = calculateOutputAmount(item, data);
            int xp = calculateXP(item, data);

            ItemStack paper = new ItemStack(Material.PAPER);
            ItemMeta meta = paper.getItemMeta();
            meta.displayName(msgComponent("reward-title"));

            List<Component> lore = new ArrayList<>();
            lore.add(msgComponent("material-label"));
            lore.add(serializer.deserialize(" §7- " + data.result.name() + " x" + amount));
            lore.add(Component.empty());
            lore.add(serializer.deserialize(msg("xp-estimated") + xp));

            meta.lore(lore);
            paper.setItemMeta(meta);

            inv.setItem(previewSlot, paper);
        }
    }

    // =========================================================
    // RECYCLAGE
    // =========================================================

    private boolean handleRecycle(Player player) {

        InventoryView view = player.getOpenInventory();
        if (view == null) return false;

        Inventory inv = view.getTopInventory();
        if (inv == null) return false;

        int totalXP = 0;
        Map<Material, Integer> materials = new HashMap<>();

        boolean hasValidItem = false;
        boolean hasInvalidItem = false;

        for (int i = 1; i <= 7; i++) {

            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;

            RecycleData data = recycleMap.get(item.getType());

            if (data == null) {
                hasInvalidItem = true;
            } else {
                hasValidItem = true;
            }
        }

        if (hasInvalidItem) {
            player.sendMessage(msgComponent("non-recyclable"));
            return false;
        }

        if (!hasValidItem) {
            player.sendMessage(msgComponent("non-recyclable"));
            return false;
        }

        Component recap = msgComponent("recap-title").append(Component.newline());

        for (int i = 1; i <= 7; i++) {

            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;

            RecycleData data = recycleMap.get(item.getType());
            if (data == null) continue;

            int amount = calculateOutputAmount(item, data);

            if (amount <= 0) continue;

            player.getInventory().addItem(new ItemStack(data.result, amount));

            int xp = calculateXP(item, data);
            totalXP += xp;

            materials.put(data.result,
                    materials.getOrDefault(data.result, 0) + amount);

            recap = recap.append(
                    serializer.deserialize("§7- §e" + item.getType().name())
            ).append(Component.newline());

            inv.setItem(i, null);
        }

        recap = recap.append(Component.newline());

        for (Map.Entry<Material, Integer> entry : materials.entrySet()) {
            recap = recap.append(
                    serializer.deserialize("§7- " + entry.getKey().name() + " x" + entry.getValue())
            ).append(Component.newline());
        }

        recap = recap.append(Component.newline())
                .append(serializer.deserialize(msg("total-xp") + totalXP));

        player.sendMessage(recap);

        if (totalXP > 0) player.giveExp(totalXP);

        player.playSound(player.getLocation(),
                Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);

        player.getWorld().spawnParticle(
                Particle.HAPPY_VILLAGER,
                player.getLocation().add(0, 1, 0),
                20, 0.4, 0.4, 0.4, 0.05);

        player.getWorld().playSound(
                player.getLocation(),
                Sound.BLOCK_ANVIL_USE, 1f, 1f);

        return true;
    }

    private int calculateXP(ItemStack item, RecycleData data) {

        int baseXP = switch (data.rarity) {
            case "IRON" -> getConfig().getInt("items.xp_rewards.iron_xp", 20);
            case "COPPER" -> getConfig().getInt("items.xp_rewards.copper_xp", 15);
            case "GOLD" -> getConfig().getInt("items.xp_rewards.gold_xp", 25);
            case "DIAMOND" -> getConfig().getInt("items.xp_rewards.diamond_xp", 40);
            case "NETHERITE" -> getConfig().getInt("items.xp_rewards.netherite_xp", 60);
            default -> getConfig().getInt("items.xp_rewards.common_xp", 10);
        };

        int enchantBonus = item.getEnchantments().size() > 0
                ? getConfig().getInt("enchanted-xp", 50)
                : 0;

        return baseXP + enchantBonus;
    }

    private int calculateOutputAmount(ItemStack item, RecycleData data) {

        int max = data.maxAmount;

        if (!(item.getItemMeta() instanceof Damageable damageable)) {
            return ThreadLocalRandom.current()
                    .nextInt(data.minAmount, data.maxAmount + 1);
        }

        int maxDurability = item.getType().getMaxDurability();
        if (maxDurability <= 0) {
            return ThreadLocalRandom.current()
                    .nextInt(data.minAmount, data.maxAmount + 1);
        }

        int currentDamage = damageable.getDamage();

        double durabilityPercent =
                (double) (maxDurability - currentDamage) / maxDurability;

        if (durabilityPercent < 0.25) {
            return ThreadLocalRandom.current().nextInt(0, 2);
        }

        double minScale;
        double maxScale;

        if (durabilityPercent < 0.50) {
            minScale = 0.25;
            maxScale = 0.50;
        } else if (durabilityPercent < 0.75) {
            minScale = 0.50;
            maxScale = 0.75;
        } else {
            minScale = 0.75;
            maxScale = 1.00;
        }

        double randomScale = ThreadLocalRandom.current()
                .nextDouble(minScale, maxScale);

        int result = (int) Math.round(max * randomScale);

        return Math.max(result, 0);
    }

    // =========================================================
    // UTIL
    // =========================================================

    private ItemStack createEmptyGlass() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(Component.text(" "));
        glass.setItemMeta(meta);
        return glass;
    }

    private void logStartup() {

        String version = getPluginMeta().getVersion();
        String lang = activeLanguage;
        int items = recycleMap.size();

        getLogger().info("");
        getLogger().info(color(PINK, "RecyclePlugin v" + version));
        getLogger().info(color(DARK_PURPLE, "    | Author: ⟡ svncho_ ⟡"));
        getLogger().info(color(DARK_BLUE, "    | Running on Paper 1.21"));
        getLogger().info(color(DARK_BLUE, "    | Loaded items: " + items));
        getLogger().info(color(DARK_BLUE, "    | Language: " + lang));
        getLogger().info("");
    }

    private void logReload() {

        String version = getPluginMeta().getVersion();
        int items = recycleMap.size();
        String lang = activeLanguage;

        getLogger().info("");
        getLogger().info(color(PINK, "RecyclePlugin v" + version + " reloaded"));
        getLogger().info(color(DARK_PURPLE, "    | Configuration reloaded"));
        getLogger().info(color(DARK_BLUE, "    | Loaded items: " + items));
        getLogger().info(color(DARK_BLUE, "    | Language: " + lang));
        getLogger().info("");
    }

    private void logShutdown() {

        getLogger().info("");
        getLogger().info(color(DARK_PURPLE, "RecyclePlugin disabled"));
        getLogger().info(color(DARK_BLUE, "    | Shutting down safely"));
        getLogger().info("");
    }

    // =========================================================
    // INNER CLASSES
    // =========================================================

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

    private static class RecycleHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}