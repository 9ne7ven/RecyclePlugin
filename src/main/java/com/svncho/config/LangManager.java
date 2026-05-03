package com.svncho.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class LangManager {

    private final JavaPlugin plugin;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    private FileConfiguration messages;
    private String activeLanguage = "en";

    public LangManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        String configuredLang = plugin.getConfig().getString("language", "en");
        String fallbackLang = plugin.getConfig().getString("fallback-language", "en");

        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists() && !langFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create lang folder.");
        }

        saveBundledLangIfMissing("en");
        saveBundledLangIfMissing("fr");
        saveBundledLangIfMissing("de");
        saveBundledLangIfMissing("es");
        saveBundledLangIfMissing("br");
        saveBundledLangIfMissing("zh");

        String langToLoad = resolveLanguage(configuredLang, fallbackLang);
        saveBundledLangIfMissing(langToLoad);
        syncMissingLangKeys(langToLoad);

        File file = new File(langFolder, "messages_" + langToLoad + ".yml");
        messages = YamlConfiguration.loadConfiguration(file);
        activeLanguage = langToLoad;
    }

    public String get(String key, String... replacements) {
        String value = messages != null ? messages.getString(key) : null;
        if (value == null) value = "§cMissing: " + key;

        for (int i = 0; i + 1 < replacements.length; i += 2) {
            value = value.replace("%" + replacements[i] + "%", replacements[i + 1]);
            value = value.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return value;
    }

    public Component component(String key, String... replacements) {
        return serializer.deserialize(get(key, replacements));
    }

    public Component rawComponent(String text) {
        return serializer.deserialize(text == null ? "" : text);
    }

    public String getActiveLanguage() {
        return activeLanguage;
    }

    private void saveBundledLangIfMissing(String lang) {
        File file = new File(plugin.getDataFolder(), "lang/messages_" + lang + ".yml");
        String resourcePath = "lang/messages_" + lang + ".yml";

        if (!file.exists() && plugin.getResource(resourcePath) != null) {
            plugin.saveResource(resourcePath, false);
        }
    }

    private void syncMissingLangKeys(String lang) {
        File file = new File(plugin.getDataFolder(), "lang/messages_" + lang + ".yml");
        String resourcePath = "lang/messages_" + lang + ".yml";

        if (!file.exists() || plugin.getResource(resourcePath) == null) return;

        YamlConfiguration userLang = YamlConfiguration.loadConfiguration(file);

        try (InputStream stream = plugin.getResource(resourcePath);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {

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
                plugin.getLogger().info("Updated missing language keys for: " + lang);
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Could not sync language file: messages_" + lang + ".yml");
            e.printStackTrace();
        }
    }

    private String resolveLanguage(String configuredLang, String fallbackLang) {
        if (hasBundledLang(configuredLang) || hasExternalLang(configuredLang)) return configuredLang;
        plugin.getLogger().warning("Unknown language detected: " + configuredLang);

        if (hasBundledLang(fallbackLang) || hasExternalLang(fallbackLang)) {
            plugin.getLogger().warning("Falling back to: " + fallbackLang);
            plugin.getConfig().set("language", fallbackLang);
            plugin.saveConfig();
            return fallbackLang;
        }

        plugin.getLogger().warning("Fallback language invalid. Defaulting to: en");
        plugin.getConfig().set("language", "en");
        plugin.saveConfig();
        return "en";
    }

    private boolean hasBundledLang(String lang) {
        return plugin.getResource("lang/messages_" + lang + ".yml") != null;
    }

    private boolean hasExternalLang(String lang) {
        return new File(plugin.getDataFolder(), "lang/messages_" + lang + ".yml").exists();
    }
}
