package com.svncho.command;

import com.svncho.config.LangManager;
import com.svncho.gui.RecycleGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class RecycleCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final LangManager lang;
    private final RecycleGui recycleGui;

    public RecycleCommand(JavaPlugin plugin, LangManager lang, RecycleGui recycleGui) {
        this.plugin = plugin;
        this.lang = lang;
        this.recycleGui = recycleGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(lang.get(plugin.getConfig().getString("messages.player-only-key", "player-only")));
            return true;
        }

        if (!player.hasPermission("recycleplugin.use")) {
            player.sendMessage(lang.component(plugin.getConfig().getString("messages.no-permission-key", "no-permission")));
            return true;
        }

        recycleGui.open(player);
        return true;
    }
}
