package me.william278.husksync.velocity.config;

import me.william278.husksync.HuskSyncVelocity;
import me.william278.husksync.Settings;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.yaml.snakeyaml.DumperOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.logging.Level;

public class ConfigManager {

    private static final HuskSyncVelocity plugin = HuskSyncVelocity.getInstance();

    public static void loadConfig() {
        try {
            if (!plugin.getDataFolder().exists()) {
                if (plugin.getDataFolder().mkdir()) {
                    plugin.getVelocityLogger().info("Created HuskSync data folder");
                }
            }
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                Files.copy(Objects.requireNonNull(HuskSyncVelocity.class.getClassLoader().getResourceAsStream("proxy-config.yml")), configFile.toPath());
                plugin.getVelocityLogger().info("Created HuskSync config file");
            }
        } catch (Exception e) {
            plugin.getVelocityLogger().log(Level.CONFIG, "An exception occurred loading the configuration file", e);
        }
    }

    public static void saveConfig(ConfigurationNode rootNode) {
        try {
            getConfigLoader().save(rootNode);
        } catch (IOException e) {
            plugin.getVelocityLogger().log(Level.CONFIG, "An exception occurred loading the configuration file", e);
        }
    }

    public static void loadMessages() {
        try {
            if (!plugin.getDataFolder().exists()) {
                if (plugin.getDataFolder().mkdir()) {
                    plugin.getVelocityLogger().info("Created HuskSync data folder");
                }
            }
            File messagesFile = new File(plugin.getDataFolder(), "messages_" + Settings.language + ".yml");
            if (!messagesFile.exists()) {
                Files.copy(Objects.requireNonNull(HuskSyncVelocity.class.getClassLoader().getResourceAsStream("languages/" + Settings.language + ".yml")),
                        messagesFile.toPath());
                plugin.getVelocityLogger().info("Created HuskSync messages file");
            }
        } catch (IOException e) {
            plugin.getVelocityLogger().log(Level.CONFIG, "An exception occurred loading the messages file", e);
        }
    }

    private static YAMLConfigurationLoader getConfigLoader() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        return YAMLConfigurationLoader.builder()
                .setPath(configFile.toPath())
                .setFlowStyle(DumperOptions.FlowStyle.BLOCK)
                .setIndent(2)
                .build();
    }

    public static ConfigurationNode getConfig() {
        try {
            return getConfigLoader().load();
        } catch (IOException e) {
            plugin.getVelocityLogger().log(Level.CONFIG, "An IOException has occurred loading the plugin config.");
            return null;
        }
    }

    public static ConfigurationNode getMessages() {
        try {
            File configFile = new File(plugin.getDataFolder(), "messages_" + Settings.language + ".yml");
            return YAMLConfigurationLoader.builder()
                    .setPath(configFile.toPath())
                    .build()
                    .load();
        } catch (IOException e) {
            plugin.getVelocityLogger().log(Level.CONFIG, "An IOException has occurred loading the plugin messages.");
            return null;
        }
    }

}

