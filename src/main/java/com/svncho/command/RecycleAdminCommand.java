package com.svncho.command;

import com.svncho.config.GuiConfig;
import com.svncho.config.LangManager;
import com.svncho.recycle.RecycleManager;
import com.svncho.update.UpdateChecker;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class RecycleAdminCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final LangManager lang;
    private final RecycleManager recycleManager;
    private final GuiConfig guiConfig;
    private final UpdateChecker updateChecker;

    public RecycleAdminCommand(JavaPlugin plugin, LangManager lang, RecycleManager recycleManager, GuiConfig guiConfig, UpdateChecker updateChecker) {
        this.plugin = plugin;
        this.lang = lang;
        this.recycleManager = recycleManager;
        this.guiConfig = guiConfig;
        this.updateChecker = updateChecker;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("recycleplugin.reload")) {
                sender.sendMessage(lang.component(plugin.getConfig().getString("messages.no-permission-key", "no-permission")));
                return true;
            }

            plugin.reloadConfig();
            lang.load();
            guiConfig.load();
            recycleManager.load();

            if (updateChecker != null) {
                updateChecker.check();
            }

            sender.sendMessage(lang.component("plugin-reloaded"));
            logReload();
            return true;
        }

        sender.sendMessage(lang.component(plugin.getConfig().getString("messages.reload-usage-key", "reload-usage")));
        return true;
    }

    private void logReload() {
        plugin.getLogger().info("");
        plugin.getLogger().info("RecyclePlugin v" + plugin.getPluginMeta().getVersion() + " reloaded");
        plugin.getLogger().info("    | Loaded items: " + recycleManager.size());
        plugin.getLogger().info("    | Language: " + lang.getActiveLanguage());
        plugin.getLogger().info("");
    }
}
