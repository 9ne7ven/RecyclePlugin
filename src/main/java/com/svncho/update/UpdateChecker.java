package com.svncho.update;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;

public class UpdateChecker {

    private final JavaPlugin plugin;
    private String latestVersion;

    public UpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private static final String UPDATE_URL =
    "https://gist.githubusercontent.com/9ne7ven/c595e98bf384c510aaa723cc36743299/raw/ffaf8add3860236db028238a4dfc0c17ac7e1c8b/RecyclePlugin";

    public void check() {
        if (!plugin.getConfig().getBoolean("update-checker.enabled", true)) return;

        String url = UPDATE_URL;
        if (url == null || url.isBlank()) {
            plugin.getLogger().warning("UpdateChecker enabled but no URL is configured.");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String latest = fetchLatestVersion(url);
                String current = plugin.getPluginMeta().getVersion();

                if (latest == null || latest.isBlank()) {
                    plugin.getLogger().warning("UpdateChecker could not read latest version.");
                    return;
                }

                latest = latest.trim();
                latestVersion = latest;

                if (!current.equalsIgnoreCase(latest)) {
                    notifyConsole(current, latest);
                } else if (plugin.getConfig().getBoolean("update-checker.notify-console", true)) {
                    plugin.getLogger().info("RecyclePlugin is up to date. Version: v" + current);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not check for updates: " + e.getMessage());
            }
        });
    }

    public void notifyPlayer(Player player) {
        if (!plugin.getConfig().getBoolean("update-checker.enabled", true)) return;
        if (!plugin.getConfig().getBoolean("update-checker.notify-ops-on-join", true)) return;
        if (!player.isOp() && !player.hasPermission("recycleplugin.update")) return;

        String current = plugin.getPluginMeta().getVersion();
        String latest = latestVersion;
        if (latest == null || latest.isBlank() || current.equalsIgnoreCase(latest)) return;

        player.sendMessage("§d§lRecyclePlugin §7» §eNew version available!");
        player.sendMessage("§7Current: §cv" + current + " §8| §7Latest: §av" + latest);
    }

    private String fetchLatestVersion(String urlString) throws Exception {
        URL url = URI.create(urlString).toURL();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            return reader.readLine();
        }
    }

    private void notifyConsole(String current, String latest) {
        if (!plugin.getConfig().getBoolean("update-checker.notify-console", true)) return;
        plugin.getLogger().warning("");
        plugin.getLogger().warning("A new RecyclePlugin version is available!");
        plugin.getLogger().warning("Current: v" + current);
        plugin.getLogger().warning("Latest : v" + latest);
        plugin.getLogger().warning("");
    }
}
