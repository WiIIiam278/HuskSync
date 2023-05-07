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

/**
 * Stores potion effect data
 */
public class PotionEffectData {

    @SerializedName("serialized_potion_effects")
    public String serializedPotionEffects;

    public PotionEffectData(@NotNull final String serializedPotionEffects) {
        this.serializedPotionEffects = serializedPotionEffects;
    }

    @SuppressWarnings("unused")
    protected PotionEffectData() {
    }

}
