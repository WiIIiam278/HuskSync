/*
 * This file is part of HuskSync, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.husksync.config;


import de.exlll.configlib.NameFormatters;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurationStore;
import de.exlll.configlib.YamlConfigurations;
import net.william278.husksync.HuskSync;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream; // XMine - подстановка переменных среды в конфиге
import java.io.IOException; // XMine - подстановка переменных среды в конфиге
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

/**
 * Interface for getting and setting data from plugin configuration files
 *
 * @since 1.0
 */
public interface ConfigProvider {

    @NotNull
    YamlConfigurationProperties.Builder<?> YAML_CONFIGURATION_PROPERTIES = YamlConfigurationProperties.newBuilder()
            .charset(StandardCharsets.UTF_8)
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
        setSettings(loadWithEnvironment( // XMine - подстановка переменных среды в конфиге
                getConfigDirectory().resolve("config.yml"),
                Settings.class,
                YAML_CONFIGURATION_PROPERTIES.header(Settings.CONFIG_HEADER).build()
        ));
    }

    // XMine start - подстановка переменных среды в конфиге
    /**
     * Loads a configuration file, expanding the {@code ${NAME}} environment variable references in
     * it - see {@link EnvironmentYaml} for how, and {@link EnvironmentSubstitutor} for why the
     * syntax is what it is.
     * <p>
     * The path splits in two, and which branch is taken depends only on whether the file holds any
     * references at all:
     * <ul>
     *     <li><b>It does not.</b> {@link YamlConfigurations#update} runs exactly as upstream calls
     *     it: the file is read, then written back with any keys new to this version merged in.</li>
     *     <li><b>It does.</b> The file is read through the expansion and <em>not</em> written back.
     *     What the plugin holds after loading is the resolved value, so a write-back would replace
     *     {@code password: "${DATABASE_PASSWORD}"} with the database password in plain text, in a
     *     file that is otherwise safe to read - defeating the entire point. Losing the merge costs
     *     XMine nothing: these configs ship inside the node image and are laid out afresh on every
     *     start, so new keys arrive with the image, not at runtime.</li>
     * </ul>
     * A file that does not exist yet is created from the defaults either way; without that the
     * plugin does not come up at all on a clean install.
     *
     * @param file       the configuration file
     * @param type       the configuration class
     * @param properties the ConfigLib properties to read it with
     * @param <T>        the configuration type
     * @return the loaded configuration
     */
    @NotNull
    private static <T> T loadWithEnvironment(@NotNull Path file, @NotNull Class<T> type,
                                             @NotNull YamlConfigurationProperties properties) {
        if (Files.exists(file)) {
            try {
                final String expanded = EnvironmentYaml.expand(
                        Files.readString(file, properties.getCharset()), System::getenv
                );
                if (expanded != null) {
                    return new YamlConfigurationStore<>(type, properties).read(
                            new ByteArrayInputStream(expanded.getBytes(properties.getCharset()))
                    );
                }
            } catch (IOException e) {
                // Fall through to upstream's path, which reports an unreadable file its own way.
                // Note what does not happen here: nothing about the file's content is logged,
                // because that content is where the secrets are.
            }
        }
        return YamlConfigurations.update(file, type, properties);
        // XMine end - подстановка переменных среды в конфиге
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
        final Path path = getConfigDirectory().resolve(String.format("messages-%s.yml", getSettings().getLanguage()));

        // Re-extract locales if the format version has changed
        if (Files.exists(path)) {
            try {
                final Locales existing = store.load(path);
                if (existing.locale_format_version != null
                        && existing.locale_format_version >= Locales.LOCALE_FORMAT_VERSION) {
                    setLocales(existing);
                    return;
                }
                getPlugin().log(Level.INFO, "Locale format updated; re-extracting locale file.");
            } catch (Throwable e) {
                getPlugin().log(Level.WARNING, "Could not read existing locale file; re-extracting.", e);
            }
        }

        // Save and read the default locales from the JAR
        try (InputStream input = getResource(String.format("locales/%s.yml", getSettings().getLanguage()))) {
            final Locales locales = store.read(input);
            locales.locale_format_version = Locales.LOCALE_FORMAT_VERSION;
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
                Server.class,
                YAML_CONFIGURATION_PROPERTIES.header(Server.CONFIG_HEADER).build()
        ));

        String envServerName = System.getenv("HUSKSYNC_SERVER_NAME");
        if (envServerName != null && !envServerName.isEmpty()) {
            Server server = Server.of(envServerName);
            setServerName(server);
            getPlugin().log(Level.INFO, "Server name set to: " + envServerName);
        }
    }

    default void validateConfigFiles() {
        // Validate server name is default
        if (getServerName().equals("server")) {
            getPlugin().log(Level.WARNING, "The server name set in ~/plugins/HuskSync/server.yml appears to" +
                    "be unchanged from the default (currently set to: \"server\"). Please check that this value has" +
                    "been updated to match the case-sensitive ID of this server in your proxy config file!");
        }
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
