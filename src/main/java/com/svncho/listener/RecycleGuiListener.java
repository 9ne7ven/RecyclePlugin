package com.svncho.listener;

import com.svncho.config.GuiConfig;
import com.svncho.config.LangManager;
import com.svncho.gui.GuiAction;
import com.svncho.gui.GuiSessionManager;
import com.svncho.gui.RecycleGui;
import com.svncho.recycle.RecycleCalculator;
import com.svncho.recycle.RecycleData;
import com.svncho.recycle.RecycleManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class RecycleGuiListener implements Listener {

    private final JavaPlugin plugin;
    private final LangManager lang;
    private final RecycleManager recycleManager;
    private final GuiConfig guiConfig;
    private final GuiSessionManager sessionManager;
    private final RecycleGui recycleGui;
    private final RecycleCalculator calculator;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    public RecycleGuiListener(JavaPlugin plugin, LangManager lang, RecycleManager recycleManager, GuiConfig guiConfig, GuiSessionManager sessionManager) {
        this.plugin = plugin;
        this.lang = lang;
        this.recycleManager = recycleManager;
        this.guiConfig = guiConfig;
        this.sessionManager = sessionManager;
        this.recycleGui = new RecycleGui(plugin, lang, recycleManager, guiConfig, sessionManager);
        this.calculator = new RecycleCalculator(plugin);
    }

    @EventHandler
    public void onRecycleClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();

        Inventory recycleInv = sessionManager.get(uuid);
        if (recycleInv == null) return;
        if (!e.getView().getTopInventory().equals(recycleInv)) return;

        boolean clickedTop = e.getClickedInventory() != null && e.getClickedInventory().equals(recycleInv);
        boolean clickedBottom = e.getClickedInventory() != null && e.getClickedInventory().equals(e.getView().getBottomInventory());

        if (clickedBottom && e.isShiftClick()) {
            e.setCancelled(true);
            handleShiftDeposit(e, player, recycleInv);
            return;
        }

        if (!clickedTop) return;

        int slot = e.getSlot();

        if (!guiConfig.isDepositSlot(slot) && guiConfig.getItemBySlot(slot) == null) {
            e.setCancelled(true);
            return;
        }

        GuiConfig.GuiItemConfig clickedConfig = guiConfig.getItemBySlot(slot);
        if (clickedConfig != null) {
            handleConfiguredGuiAction(e, player, recycleInv, clickedConfig);
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> recycleGui.refresh(player, recycleInv));
    }

    private void handleShiftDeposit(InventoryClickEvent e, Player player, Inventory recycleInv) {
        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        if (!recycleManager.isRecyclable(item.getType())) {
            sendLang(player, "messages.non-recyclable-key", "recycle-non-recyclable");
            playSounds(player, "effects.error.sounds", Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
            return;
        }

        ItemStack clone = item.clone();
        int before = clone.getAmount();
        int leftover = addToDepositSlots(recycleInv, clone);
        int moved = before - leftover;

        if (moved > 0) {
            item.setAmount(item.getAmount() - moved);
            if (item.getAmount() <= 0 && e.getClickedInventory() != null) e.getClickedInventory().setItem(e.getSlot(), null);
            recycleGui.refresh(player, recycleInv);
        }
    }

    private void handleConfiguredGuiAction(InventoryClickEvent e, Player player, Inventory recycleInv, GuiConfig.GuiItemConfig clickedConfig) {
        GuiAction action = clickedConfig.action();

        if (action != GuiAction.NONE) e.setCancelled(true);

        switch (action) {
            case CONFIRM -> handleRecycle(player, recycleInv);
            case CLOSE_RETURN -> {
                returnDepositItems(player, recycleInv);
                sessionManager.markConfirmingClose(player.getUniqueId());
                player.closeInventory();
            }
            case INFO, NONE -> e.setCancelled(true);
        }
    }

    @EventHandler
    public void onRecycleDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();

        Inventory recycleInv = sessionManager.get(uuid);
        if (recycleInv == null) return;
        if (!e.getInventory().equals(recycleInv)) return;

        for (int rawSlot : e.getRawSlots()) {
            if (rawSlot < guiConfig.getSize() && !guiConfig.isDepositSlot(rawSlot)) {
                e.setCancelled(true);
                return;
            }
        }

        Bukkit.getScheduler().runTask(plugin, () -> recycleGui.refresh(player, recycleInv));
    }

    @EventHandler
    public void onRecycleClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();

        Inventory recycleInv = sessionManager.get(uuid);
        if (recycleInv == null) return;
        if (!e.getInventory().equals(recycleInv)) return;

        sessionManager.remove(uuid);
        if (sessionManager.consumeConfirmingClose(uuid)) return;

        returnDepositItems(player, recycleInv);
    }

    private void handleRecycle(Player player, Inventory inv) {
        boolean hasValid = false;
        boolean hasInvalid = false;

        for (int slot : guiConfig.getDepositSlots()) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;
            if (recycleManager.isRecyclable(item.getType())) hasValid = true;
            else hasInvalid = true;
        }

        if (!hasValid) {
            sendLang(player, "messages.non-recyclable-key", "recycle-non-recyclable");
            playSounds(player, "effects.error.sounds", Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
            return;
        }

        if (hasInvalid) {
            sendLang(player, "messages.invalid-item-present-key", "recycle-invalid-item-present");
            playSounds(player, "effects.error.sounds", Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
            return;
        }

        long cooldownMs = plugin.getConfig().getLong("recycle.cooldown-seconds", plugin.getConfig().getLong("cooldown-seconds", 0)) * 1000L;
        long lastUse = recycleManager.getLastUse(player.getUniqueId());
        long remaining = (lastUse + cooldownMs - System.currentTimeMillis()) / 1000L;
        if (remaining > 0) {
            String key = plugin.getConfig().getString("messages.cooldown-key", "recycle-cooldown");
            player.sendMessage(lang.component(key, "seconds", String.valueOf(remaining)));
            playSounds(player, "effects.error.sounds", Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
            return;
        }

        double luckyChance = plugin.getConfig().getDouble("recycle.lucky-chance", plugin.getConfig().getDouble("lucky-chance", 0.0));
        boolean lucky = luckyChance > 0 && ThreadLocalRandom.current().nextDouble() * 100 < luckyChance;
        if (lucky && plugin.getConfig().getBoolean("messages.send-lucky-message", true)) {
            sendLang(player, "messages.lucky-key", "recycle-lucky");
        }

        int totalXP = 0;
        int totalItemsRecycled = 0;
        Map<Material, Integer> gained = new LinkedHashMap<>();

        for (int slot : guiConfig.getDepositSlots()) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;

            RecycleData data = recycleManager.get(item.getType());
            if (data == null) continue;

            int amount = calculator.calculateOutputAmount(item, data);
            if (lucky) amount *= plugin.getConfig().getInt("recycle.lucky-multiplier", 2);

            if (amount > 0) {
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(new ItemStack(data.result(), amount));
                leftover.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
                gained.put(data.result(), gained.getOrDefault(data.result(), 0) + amount);
            }

            totalXP += calculator.calculateXP(item, data);
            totalItemsRecycled += item.getAmount();
            inv.setItem(slot, null);
        }

        if (cooldownMs > 0) recycleManager.setLastUse(player.getUniqueId(), System.currentTimeMillis());
        recycleManager.addRecycledStats(player.getUniqueId(), totalItemsRecycled);

        sendRecap(player, gained, totalXP);

        if (totalXP > 0) player.giveExp(totalXP);
        playSounds(player, "effects.success.sounds", Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        spawnParticles(player, "effects.success.particles");

        sessionManager.markConfirmingClose(player.getUniqueId());
        player.closeInventory();
    }

    private void sendRecap(Player player, Map<Material, Integer> gained, int totalXP) {
        if (!plugin.getConfig().getBoolean("messages.send-recap", true)) return;

        StringBuilder recap = new StringBuilder(lang.get(plugin.getConfig().getString("messages.recap-title-key", "recycle-recap-title"))).append("\n");
        String lineFormat = plugin.getConfig().getString("messages.recap-line-format", "§7- {material} x{amount}");

        for (Map.Entry<Material, Integer> entry : gained.entrySet()) {
            recap.append(lineFormat
                    .replace("{material}", entry.getKey().name())
                    .replace("{amount}", String.valueOf(entry.getValue()))
            ).append("\n");
        }

        recap.append("\n")
                .append(lang.get(plugin.getConfig().getString("messages.total-xp-key", "recycle-total-xp")))
                .append(totalXP);

        player.sendMessage(serializer.deserialize(recap.toString()));
    }

    private int addToDepositSlots(Inventory inv, ItemStack item) {
        for (int slot : guiConfig.getDepositSlots()) {
            ItemStack existing = inv.getItem(slot);
            if (existing != null && existing.isSimilar(item) && existing.getAmount() < existing.getMaxStackSize()) {
                int space = existing.getMaxStackSize() - existing.getAmount();
                int toAdd = Math.min(space, item.getAmount());
                existing.setAmount(existing.getAmount() + toAdd);
                item.setAmount(item.getAmount() - toAdd);
                if (item.getAmount() <= 0) return 0;
            }
        }

        for (int slot : guiConfig.getDepositSlots()) {
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
        for (int slot : guiConfig.getDepositSlots()) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
            leftover.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
            inv.setItem(slot, null);
        }
    }

    private void sendLang(Player player, String configPath, String fallbackKey) {
        player.sendMessage(lang.component(plugin.getConfig().getString(configPath, fallbackKey)));
    }

    private Sound resolveSound(String name, Sound fallback) {
        try {
            if (name == null || name.isBlank()) return fallback;

            String normalized = name.toLowerCase().replace("_", ".");
            NamespacedKey key = name.contains(":")
                    ? NamespacedKey.fromString(normalized)
                    : NamespacedKey.minecraft(normalized);

            Sound sound = key != null ? Registry.SOUNDS.get(key) : null;
            return sound != null ? sound : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private Particle resolveParticle(String name, Particle fallback) {
        try {
            if (name == null || name.isBlank()) return fallback;

            String normalized = name.toLowerCase().replace("_", ".");
            NamespacedKey key = name.contains(":")
                    ? NamespacedKey.fromString(normalized)
                    : NamespacedKey.minecraft(normalized);

            Particle particle = key != null ? Registry.PARTICLE_TYPE.get(key) : null;
            return particle != null ? particle : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private String soundKey(Sound sound, String fallback) {
        NamespacedKey key = Registry.SOUNDS.getKey(sound);
        return key != null ? key.asString() : fallback;
    }

    private String particleKey(Particle particle, String fallback) {
        NamespacedKey key = Registry.PARTICLE_TYPE.getKey(particle);
        return key != null ? key.asString() : fallback;
    }

    private float parseFloat(Object value, float fallback) {
        if (value == null) return fallback;
        try {
            return Float.parseFloat(String.valueOf(value));
        } catch (Exception e) {
            return fallback;
        }
    }

    private double parseDouble(Object value, double fallback) {
        if (value == null) return fallback;
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return fallback;
        }
    }

    private int parseInt(Object value, int fallback) {
        if (value == null) return fallback;
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return fallback;
        }
    }

    private void playSounds(Player player, String path, Sound fallback, float fallbackVolume, float fallbackPitch) {
        if (!plugin.getConfig().isList(path)) {
            player.playSound(player.getLocation(), fallback, fallbackVolume, fallbackPitch);
            return;
        }

        String fallbackSound = soundKey(fallback, "minecraft:entity.villager.no");

        for (Map<?, ?> soundConfig : plugin.getConfig().getMapList(path)) {
            String soundName = String.valueOf(
                    soundConfig.containsKey("sound") ? soundConfig.get("sound") : fallbackSound
            );

            float volume = parseFloat(soundConfig.get("volume"), fallbackVolume);
            float pitch = parseFloat(soundConfig.get("pitch"), fallbackPitch);

            Sound sound = resolveSound(soundName, fallback);
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    private void spawnParticles(Player player, String path) {
        if (!plugin.getConfig().isList(path)) {
            player.getWorld().spawnParticle(
                    Particle.HAPPY_VILLAGER,
                    player.getLocation().add(0, 1, 0),
                    20,
                    0.4,
                    0.4,
                    0.4,
                    0.05
            );
            return;
        }

        String fallbackParticle = particleKey(Particle.HAPPY_VILLAGER, "minecraft:happy_villager");

        for (Map<?, ?> particleConfig : plugin.getConfig().getMapList(path)) {
            String particleName = String.valueOf(
                    particleConfig.containsKey("particle") ? particleConfig.get("particle") : fallbackParticle
            );

            Particle particle = resolveParticle(particleName, Particle.HAPPY_VILLAGER);

            int count = parseInt(particleConfig.get("count"), 20);
            double offsetX = parseDouble(particleConfig.get("offset-x"), 0.4);
            double offsetY = parseDouble(particleConfig.get("offset-y"), 0.4);
            double offsetZ = parseDouble(particleConfig.get("offset-z"), 0.4);
            double speed = parseDouble(particleConfig.get("speed"), 0.05);
            double addX = parseDouble(particleConfig.get("add-x"), 0.0);
            double addY = parseDouble(particleConfig.get("add-y"), 1.0);
            double addZ = parseDouble(particleConfig.get("add-z"), 0.0);

            player.getWorld().spawnParticle(
                    particle,
                    player.getLocation().add(addX, addY, addZ),
                    count,
                    offsetX,
                    offsetY,
                    offsetZ,
                    speed
            );
        }
    }
}
