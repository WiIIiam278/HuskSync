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

/**
 * Stores status information about a player
 */
public class StatusData {

    /**
     * The player's health points
     */
    @SerializedName("health")
    public double health;

    /**
     * The player's maximum health points
     */
    @SerializedName("max_health")
    public double maxHealth;

    /**
     * The player's health scaling factor
     */
    @SerializedName("health_scale")
    public double healthScale;

    /**
     * The player's hunger points
     */
    @SerializedName("hunger")
    public int hunger;

    /**
     * The player's saturation points
     */
    @SerializedName("saturation")
    public float saturation;

    /**
     * The player's saturation exhaustion points
     */
    @SerializedName("saturation_exhaustion")
    public float saturationExhaustion;

    /**
     * The player's currently selected item slot
     */
    @SerializedName("selected_item_slot")
    public int selectedItemSlot;

    /**
     * The player's total experience points<p>
     * (not to be confused with <i>experience level</i> - this is the "points" value shown on the death screen)
     */
    @SerializedName("total_experience")
    public int totalExperience;

    /**
     * The player's experience level (shown on the exp bar)
     */
    @SerializedName("experience_level")
    public int expLevel;

    /**
     * The player's progress to their next experience level
     */
    @SerializedName("experience_progress")
    public float expProgress;

    /**
     * The player's game mode string (one of "SURVIVAL", "CREATIVE", "ADVENTURE", "SPECTATOR")
     */
    @SerializedName("game_mode")
    public String gameMode;

    /**
     * If the player is currently flying
     */
    @SerializedName("is_flying")
    public boolean isFlying;

    public StatusData(final double health, final double maxHealth, final double healthScale,
                      final int hunger, final float saturation, final float saturationExhaustion,
                      final int selectedItemSlot, final int totalExperience, final int expLevel,
                      final float expProgress, final String gameMode, final boolean isFlying) {
        this.health = health;
        this.maxHealth = maxHealth;
        this.healthScale = healthScale;
        this.hunger = hunger;
        this.saturation = saturation;
        this.saturationExhaustion = saturationExhaustion;
        this.selectedItemSlot = selectedItemSlot;
        this.totalExperience = totalExperience;
        this.expLevel = expLevel;
        this.expProgress = expProgress;
        this.gameMode = gameMode;
        this.isFlying = isFlying;
    }

    @SuppressWarnings("unused")
    protected StatusData() {
    }
}
