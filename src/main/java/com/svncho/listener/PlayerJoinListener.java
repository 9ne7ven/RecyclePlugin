package com.svncho.listener;

import com.svncho.update.UpdateChecker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final UpdateChecker updateChecker;

    public PlayerJoinListener(UpdateChecker updateChecker) {
        this.updateChecker = updateChecker;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        updateChecker.notifyPlayer(e.getPlayer());
    }
}
