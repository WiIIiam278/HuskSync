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

import net.william278.annotaml.Annotaml;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@DisplayName("Locales Tests")
public class LocalesTests {

    private Locales englishLocales;

    @BeforeEach
    @DisplayName("Test Loading English Locales")
    @Test
    public void testLoadEnglishLocales() {
        try (InputStream locales = LocalesTests.class.getClassLoader().getResourceAsStream("locales/en-gb.yml")) {
            Assertions.assertNotNull(locales, "en-gb.yml is missing from the locales folder");
            englishLocales = Annotaml.create(Locales.class, locales).get();
        } catch (IOException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @ParameterizedTest(name = "{1} Locales")
    @DisplayName("Test All Locale Keys Present")
    @MethodSource("provideLocaleFiles")
    public void testAllLocaleKeysPresent(@NotNull File file, @SuppressWarnings("unused") @NotNull String keyName) {
        try {
            final Set<String> fileKeys = Annotaml.create(file, Locales.class).get().rawLocales.keySet();
            englishLocales.rawLocales.keySet()
                    .forEach(key -> Assertions.assertTrue(fileKeys.contains(key),
                            "Locale key " + key + " is missing from " + file.getName()));
        } catch (IOException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private static Stream<Arguments> provideLocaleFiles() {
        URL url = LocalesTests.class.getClassLoader().getResource("locales");
        Assertions.assertNotNull(url, "locales folder is missing");
        return Stream.of(Objects.requireNonNull(new File(url.getPath())
                        .listFiles(file -> file.getName().endsWith("yml") && !file.getName().equals("en-gb.yml"))))
                .map(file -> Arguments.of(file, file.getName().replace(".yml", "")));
    }

}