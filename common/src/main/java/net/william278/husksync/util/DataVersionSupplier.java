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
import net.william278.husksync.HuskSync;
import org.jetbrains.annotations.NotNull;

public interface DataVersionSupplier {

    int VERSION1_16_5 = 2586;
    int VERSION1_17_1 = 2730;
    int VERSION1_18_2 = 2975;
    int VERSION1_19_2 = 3120;
    int VERSION1_19_4 = 3337;
    int VERSION1_20_1 = 3465;
    int VERSION1_20_2 = 3578;
    int VERSION1_20_4 = 3700;
    int VERSION1_20_5 = 3837;
    int VERSION1_21_1 = 3955;
    int VERSION1_21_3 = 4082;
    int VERSION1_21_4 = 4189;
    int VERSION1_21_5 = 4323;
    int VERSION1_21_6 = 4435;
    int VERSION1_21_7 = 4438;
    int VERSION1_21_8 = 4438;

    /**
     * Returns the data version for a Minecraft version
     *
     * @param mcVersion the Minecraft version
     * @return the data version int
     */
    default int getDataVersion(@NotNull Version mcVersion) {
        return switch (mcVersion.toStringWithoutMetadata()) {
            case "1.16", "1.16.1", "1.16.2", "1.16.3", "1.16.4", "1.16.5" -> VERSION1_16_5;
            case "1.17", "1.17.1" -> VERSION1_17_1;
            case "1.18", "1.18.1", "1.18.2" -> VERSION1_18_2;
            case "1.19", "1.19.1", "1.19.2" -> VERSION1_19_2;
            case "1.19.4" -> VERSION1_19_4;
            case "1.20", "1.20.1" -> VERSION1_20_1;
            case "1.20.2" -> VERSION1_20_2;
            case "1.20.4" -> VERSION1_20_4;
            case "1.20.5", "1.20.6" -> VERSION1_20_5;
            case "1.21", "1.21.1" -> VERSION1_21_1;
            case "1.21.2", "1.21.3" -> VERSION1_21_3;
            case "1.21.4" -> VERSION1_21_4;
            case "1.21.5" -> VERSION1_21_5;
            case "1.21.6" -> VERSION1_21_6;
            case "1.21.7" -> VERSION1_21_7;
            default -> VERSION1_21_8; // Latest supported version
        };
    }

    @NotNull
    HuskSync getPlugin();

}
