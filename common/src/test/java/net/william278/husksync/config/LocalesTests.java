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

import de.exlll.configlib.YamlConfigurations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Locales Tests")
public class LocalesTests {

    private Locales englishLocales;

    @BeforeEach
    @DisplayName("Test Loading English Locales")
    @Test
    public void testLoadEnglishLocales() {
        try (InputStream locales = LocalesTests.class.getClassLoader().getResourceAsStream("locales/en-gb.yml")) {
            assertNotNull(locales, "en-gb.yml is missing from the locales folder");
            englishLocales = YamlConfigurations.read(locales, Locales.class);
        } catch (Throwable e) {
            fail("Failed to load en-gb.yml", e);
        }
    }

    @ParameterizedTest(name = "{1} Locales")
    @DisplayName("Test All Locale Keys Present")
    @MethodSource("provideLocaleFiles")
    public void testAllLocaleKeysPresent(@NotNull File file, @SuppressWarnings("unused") @NotNull String keyName) {
        final Set<String> fileKeys = YamlConfigurations.load(file.toPath(), Locales.class).locales.keySet();
        englishLocales.locales.keySet().forEach(key -> assertTrue(
                fileKeys.contains(key), "Locale key " + key + " is missing from " + file.getName()
        ));
    }

    @NotNull
    private static Stream<Arguments> provideLocaleFiles() {
        final URL url = LocalesTests.class.getClassLoader().getResource("locales");
        assertNotNull(url, "locales folder is missing");

        return Stream.of(Objects.requireNonNull(new File(url.getPath()).listFiles(
                file -> file.getName().endsWith("yml") && !file.getName().equals("en-gb.yml")
        ))).map(file -> Arguments.of(file, file.getName().replace(".yml", "")));
    }

}