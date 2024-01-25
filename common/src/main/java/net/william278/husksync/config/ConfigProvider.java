package net.william278.husksync.config;


import de.exlll.configlib.NameFormatters;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurationStore;
import de.exlll.configlib.YamlConfigurations;
import net.william278.cloplib.operation.OperationType;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.trust.TrustLevel;
import net.william278.husksync.HuskSync;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Interface for getting and setting data from plugin configuration files
 *
 * @since 1.0
 */
public interface ConfigProvider {

    @NotNull
    YamlConfigurationProperties.Builder<?> YAML_CONFIGURATION_PROPERTIES = YamlConfigurationProperties.newBuilder()
            .setNameFormatter(NameFormatters.LOWER_UNDERSCORE);

    /**
     * Get the plugin settings, read from the config file
     *
     * @return the plugin settings
     * @since 1.0
     */
    @NotNull
    Settings getSettings();

    /**
     * Set the plugin settings
     *
     * @param settings The settings to set
     * @since 1.0
     */
    void setSettings(@NotNull Settings settings);

    /**
     * Load the plugin settings from the config file
     *
     * @since 1.0
     */
    default void loadSettings() {
        setSettings(YamlConfigurations.update(
                getConfigDirectory().resolve("config.yml"),
                Settings.class,
                YAML_CONFIGURATION_PROPERTIES.header(Settings.CONFIG_HEADER).build()
        ));
    }

    /**
     * Get the locales for the plugin
     *
     * @return the locales for the plugin
     * @since 1.0
     */
    @NotNull
    Locales getLocales();

    /**
     * Set the locales for the plugin
     *
     * @param locales The locales to set
     * @since 1.0
     */
    void setLocales(@NotNull Locales locales);

    /**
     * Load the locales from the config file
     *
     * @since 1.0
     */
    default void loadLocales() {
        final YamlConfigurationStore<Locales> store = new YamlConfigurationStore<>(
                Locales.class, YAML_CONFIGURATION_PROPERTIES.header(Locales.CONFIG_HEADER).build()
        );
        // Read existing locales if present
        final Path path = getConfigDirectory().resolve(String.format("messages-%s.yml", getSettings().getLanguage()));
        if (!Files.exists(path)) {
            setLocales(store.load(path));
            return;
        }

        // Otherwise, save and read the default locales
        try (InputStream input = getResource(String.format("locales/%s.yml", getSettings().getLanguage()))) {
            final Locales locales = store.read(input);
            store.save(locales, path);
            setLocales(locales);
        } catch (Throwable e) {
            getPlugin().log(Level.SEVERE, "An error occurred loading the locales (invalid lang code?)", e);
        }
    }

    @NotNull
    String getServerName();

    void setServerName(@NotNull Server server);

    default void loadServer() {
        setServerName(YamlConfigurations.update(
                getConfigDirectory().resolve("server.yml"),
                Server.getDefault(getPlugin()),
                YAML_CONFIGURATION_PROPERTIES.header(Server.CONFIG_HEADER).build()
        ));
    }

    /**
     * Get a plugin resource
     *
     * @param name The name of the resource
     * @return the resource, if found
     * @since 1.0
     */
    InputStream getResource(@NotNull String name);

    /**
     * Get the plugin config directory
     *
     * @return the plugin config directory
     * @since 1.0
     */
    @NotNull
    Path getConfigDirectory();

    @NotNull
    HuskSync getPlugin();

}
