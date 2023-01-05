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
