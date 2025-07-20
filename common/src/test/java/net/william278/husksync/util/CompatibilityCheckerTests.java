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

package net.william278.husksync.util;

import net.william278.desertwell.util.Version;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Compatibility Checker Tests")
public class CompatibilityCheckerTests {

    @ParameterizedTest(name = "Ver: {0}, Range: {1}")
    @DisplayName("Test Compatibility Checker")
    @CsvSource({
            "1.20.1, 1.21.1, false",
            "1.21.1, 1.20.1, false",
            "1.7.2, 1.21.5, false",
            "1.19.4, 1.21.1, false",
            "1.21.3, 1.21.3, true",
            "1.20.1, 1.20.1, true",
            "1.21.7, 1.21.7, true",
            "1.21.8, >=1.21.7, true",
            "1.21.8, >1.21.7, true",
            "1.0, <1.21.7, true",
            "1.17.1, !1.17.1, false",
            "1.21.7, '>=1.21.7 <=1.21.8', true",
            "1.21.8, '>=1.21.7 <=1.21.8', true",
            "1.21.5, '>=1.21.7 <=1.21.8', false",
    })
    public void testCompatibilityChecker(@NotNull String mcVer, @NotNull String range, boolean exp) {
        final Version version = Version.fromString(mcVer);
        Assertions.assertNotNull(version, "Version should not be null");

        final CompatibilityChecker.CompatibilityConfig config = new CompatibilityChecker.CompatibilityConfig(range);
        Assertions.assertEquals(exp, config.isCompatibleWith(version), "Checker should return " + exp);
    }


}
