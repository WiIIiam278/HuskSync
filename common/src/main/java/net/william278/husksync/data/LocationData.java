/*
 * This file is part of HuskSync by William278. Do not redistribute!
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  All rights reserved.
 *
 *  This source code is provided as reference to licensed individuals that have purchased the HuskSync
 *  plugin once from any of the official sources it is provided. The availability of this code does
 *  not grant you the rights to modify, re-distribute, compile or redistribute this source code or
 *  "plugin" outside this intended purpose. This license does not cover libraries developed by third
 *  parties that are utilised in the plugin.
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
