package com.svncho;

import com.svncho.command.RecycleAdminCommand;
import com.svncho.command.RecycleCommand;
import com.svncho.command.RecycleTabCompleter;
import com.svncho.config.GuiConfig;
import com.svncho.config.LangManager;
import com.svncho.gui.GuiSessionManager;
import com.svncho.gui.RecycleGui;
import com.svncho.listener.PlayerJoinListener;
import com.svncho.listener.RecycleGuiListener;
import com.svncho.recycle.RecycleManager;
import com.svncho.update.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class RecyclePlugin extends JavaPlugin {

    private LangManager langManager;
    private RecycleManager recycleManager;
    private GuiConfig guiConfig;
    private GuiSessionManager guiSessionManager;
    private UpdateChecker updateChecker;

    private static final String RESET       = "\u001B[0m";
    private static final String DARK_BLUE   = "\u001B[34m";
    private static final String DARK_PURPLE = "\u001B[35m";
    private static final String PINK        = "\u001B[95m";
    private static final String GREEN       = "\u001B[32m";
    private static final String YELLOW      = "\u001B[33m";

    private boolean supportsAnsi() { return System.console() != null; }
    private String color(String c, String msg) { return supportsAnsi() ? c + msg + RESET : msg; }


    @Override
    public void onEnable() {
        saveDefaultConfig();

        langManager = new LangManager(this);
        recycleManager = new RecycleManager(this);
        guiConfig = new GuiConfig(this);
        guiSessionManager = new GuiSessionManager();
        updateChecker = new UpdateChecker(this);

        langManager.load();
        recycleManager.load();
        recycleManager.loadStats();
        guiConfig.load();

        RecycleGui recycleGui = new RecycleGui(this, langManager, recycleManager, guiConfig, guiSessionManager);

        if (getCommand("recycle") != null) {
            getCommand("recycle").setExecutor(
                    new RecycleCommand(this, langManager, recycleGui)
            );
        }

        if (getCommand("rpadmin") != null) {
            getCommand("rpadmin").setExecutor(
                    new RecycleAdminCommand(this, langManager, recycleManager, guiConfig, updateChecker)
            );
        }

        RecycleTabCompleter tabCompleter = new RecycleTabCompleter();

        if (getCommand("recycle") != null) {
            getCommand("recycle").setTabCompleter(tabCompleter);
        }

        if (getCommand("rpadmin") != null) {
            getCommand("rpadmin").setTabCompleter(tabCompleter);
        }

        Bukkit.getPluginManager().registerEvents(
                new RecycleGuiListener(this, langManager, recycleManager, guiConfig, guiSessionManager),
                this
        );

        Bukkit.getPluginManager().registerEvents(
                new PlayerJoinListener(updateChecker),
                this
        );

        updateChecker.check();
        logStartup();
    }

    @Override
    public void onDisable() {
        if (recycleManager != null) {
            recycleManager.saveStats();
        }
        logShutdown();
    }

    public LangManager getLangManager() {
        return langManager;
    }

    public RecycleManager getRecycleManager() {
        return recycleManager;
    }

    public GuiConfig getGuiConfig() {
        return guiConfig;
    }

    private void logStartup() {
        String version = getPluginMeta().getVersion();
        String serverName = Bukkit.getName();
        String mcVersion = Bukkit.getMinecraftVersion();

        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(color(PINK, "            ██████╗ ██████╗ "));
        Bukkit.getConsoleSender().sendMessage(color(PINK, "            ██╔══██╗██╔══██╗"));
        Bukkit.getConsoleSender().sendMessage(color(PINK, "            ██████╔╝██████╔╝"));
        Bukkit.getConsoleSender().sendMessage(color(PINK, "            ██╔══██╗██╔═══╝ "));
        Bukkit.getConsoleSender().sendMessage(color(PINK, "            ██║  ██║██║     "));
        Bukkit.getConsoleSender().sendMessage(color(PINK, "            ╚═╝  ╚═╝╚═╝     "));
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(color(PINK, "◆ ─────────── RecyclePlugin ─────────── ◆"));
        Bukkit.getConsoleSender().sendMessage("");

        Bukkit.getConsoleSender().sendMessage(color(DARK_BLUE, "    | Author      ") + color(PINK, ": ") + color(DARK_PURPLE, "svncho_"));
        Bukkit.getConsoleSender().sendMessage(color(DARK_BLUE, "    | Version     ") + color(PINK, ": ") + "🎫  v" + version);
        Bukkit.getConsoleSender().sendMessage(color(DARK_BLUE, "    | Server      ") + color(PINK, ": ") + "📦  " + serverName + " " + mcVersion);
        Bukkit.getConsoleSender().sendMessage(color(DARK_BLUE, "    | Items       ") + color(PINK, ": ") + "♻   " + recycleManager.size() + " items");
        Bukkit.getConsoleSender().sendMessage(color(DARK_BLUE, "    | Language    ") + color(PINK, ": ") + "🌍  " + langManager.getActiveLanguage());
        Bukkit.getConsoleSender().sendMessage(color(DARK_BLUE, "    | GUI         ") + color(PINK, ": ") + "🖥   " + guiConfig.getSize() + " slots");
        Bukkit.getConsoleSender().sendMessage(color(DARK_BLUE, "    | Updates     ") + color(PINK, ": ") + (getConfig().getBoolean("update-checker.enabled", true) ? color(GREEN, "✅  Enabled") : color(YELLOW, "⚠️  Disabled")));

        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(color(PINK, "◆ ──────────── ") + color(GREEN, "Enabled ✅") + color(PINK, " ──────────── ◆"));
        Bukkit.getConsoleSender().sendMessage("");
    }

    private void logShutdown() {
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(color(DARK_PURPLE, "◆ ───────────── RecyclePlugin - shutting down ───────────── ◆"));
        Bukkit.getConsoleSender().sendMessage(color(DARK_BLUE,   "                   | All data saved safely."));
        Bukkit.getConsoleSender().sendMessage("");
    }
}
