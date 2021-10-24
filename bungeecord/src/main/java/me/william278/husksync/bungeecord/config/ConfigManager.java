package me.william278.husksync.bungeecord.config;

import me.william278.husksync.HuskSyncBungeeCord;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;

public class ConfigManager {

    private static final HuskSyncBungeeCord plugin = HuskSyncBungeeCord.getInstance();

    public static void loadConfig() {
        try {
            if (!plugin.getDataFolder().exists()) {
                if (plugin.getDataFolder().mkdir()) {
                    plugin.getLogger().info("Created HuskSync data folder");
                }
            }
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                Files.copy(plugin.getResourceAsStream("bungee-config.yml"), configFile.toPath());
                plugin.getLogger().info("Created HuskSync config file");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.CONFIG, "An exception occurred loading the configuration file", e);
        }
    }

    public static void loadMessages(String language) {
        try {
            if (!plugin.getDataFolder().exists()) {
                if (plugin.getDataFolder().mkdir()) {
                    plugin.getLogger().info("Created HuskSync data folder");
                }
            }
            File messagesFile = new File(plugin.getDataFolder(), "messages_ " + language + ".yml");
            if (!messagesFile.exists()) {
                Files.copy(plugin.getResourceAsStream("languages" + File.separator + language + ".yml"), messagesFile.toPath());
                plugin.getLogger().info("Created HuskSync messages file");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.CONFIG, "An exception occurred loading the messages file", e);
        }
    }

    public static Configuration getConfig() {
        try {
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            return ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.CONFIG, "An IOException occurred fetching the configuration file", e);
            return null;
        }
    }

    public static Configuration getMessages(String language) {
        try {
            File configFile = new File(plugin.getDataFolder(), "messages-" + language + ".yml");
            return ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.CONFIG, "An IOException occurred fetching the messages file", e);
            return null;
        }
    }

}

