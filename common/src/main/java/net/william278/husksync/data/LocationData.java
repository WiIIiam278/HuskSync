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

package net.william278.husksync.data;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Stores information about a player's location
 */
public class LocationData {

    /**
     * Name of the world on the server
     */
    @SerializedName("world_name")
    public String worldName;
    /**
     * Unique id of the world
     */
    @SerializedName("world_uuid")
    public UUID worldUuid;
    /**
     * The environment type of the world (one of "NORMAL", "NETHER", "THE_END")
     */
    @SerializedName("world_environment")
    public String worldEnvironment;

    /**
     * The x coordinate of the location
     */
    @SerializedName("x")
    public double x;
    /**
     * The y coordinate of the location
     */
    @SerializedName("y")
    public double y;
    /**
     * The z coordinate of the location
     */
    @SerializedName("z")
    public double z;

    /**
     * The location's facing yaw angle
     */
    @SerializedName("yaw")
    public float yaw;
    /**
     * The location's facing pitch angle
     */
    @SerializedName("pitch")
    public float pitch;

    public LocationData(@NotNull String worldName, @NotNull UUID worldUuid,
                        @NotNull String worldEnvironment,
                        double x, double y, double z,
                        float yaw, float pitch) {
        this.worldName = worldName;
        this.worldUuid = worldUuid;
        this.worldEnvironment = worldEnvironment;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    @SuppressWarnings("unused")
    protected LocationData() {
    }
}
