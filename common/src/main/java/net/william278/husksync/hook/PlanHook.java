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

package net.william278.husksync.hook;

import com.djrapitops.plan.capability.CapabilityService;
import com.djrapitops.plan.extension.ExtensionService;
import net.william278.husksync.HuskSync;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public class PlanHook {

    private final HuskSync plugin;

    public PlanHook(@NotNull HuskSync plugin) {
        this.plugin = plugin;
    }

    public void hookIntoPlan() {
        if (!areAllCapabilitiesAvailable()) {
            return;
        }
        registerDataExtension();
        handlePlanReload();
    }

    private boolean areAllCapabilitiesAvailable() {
        CapabilityService capabilities = CapabilityService.getInstance();
        return capabilities.hasCapability("DATA_EXTENSION_VALUES");
    }

    private void registerDataExtension() {
        try {
            ExtensionService.getInstance().register(new PlanDataExtension(plugin));
        } catch (IllegalStateException | IllegalArgumentException e) {
            plugin.log(Level.WARNING, "Failed to register Plan data extension: " + e.getMessage(), e);
        }
    }

    // Re-register the extension when plan enables
    private void handlePlanReload() {
        CapabilityService.getInstance().registerEnableListener(isPlanEnabled -> {
            if (isPlanEnabled) {
                registerDataExtension();
            }
        });
    }

}
