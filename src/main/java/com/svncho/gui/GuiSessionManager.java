package com.svncho.gui;

import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GuiSessionManager {

    private final Map<UUID, Inventory> recycleGuiMap = new HashMap<>();
    private final Set<UUID> confirmingClose = new HashSet<>();

    public void put(UUID uuid, Inventory inventory) {
        recycleGuiMap.put(uuid, inventory);
    }

    public Inventory get(UUID uuid) {
        return recycleGuiMap.get(uuid);
    }

    public void remove(UUID uuid) {
        recycleGuiMap.remove(uuid);
    }

    public void markConfirmingClose(UUID uuid) {
        confirmingClose.add(uuid);
    }

    public boolean consumeConfirmingClose(UUID uuid) {
        return confirmingClose.remove(uuid);
    }
}
