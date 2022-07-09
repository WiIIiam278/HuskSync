package net.william278.husksync.hook;

import com.djrapitops.plan.capability.CapabilityService;
import com.djrapitops.plan.extension.ExtensionService;
import net.william278.husksync.database.Database;
import net.william278.husksync.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public class PlanHook {

    private final Database database;
    private final Logger logger;

    public PlanHook(@NotNull Database database, @NotNull Logger logger) {
        this.database = database;
        this.logger = logger;
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
            ExtensionService.getInstance().register(new PlanDataExtension(database));
        } catch (IllegalStateException planIsNotEnabled) {
            logger.log(Level.SEVERE, "Plan extension hook failed to register. Plan is not enabled.", planIsNotEnabled);
            // Plan is not enabled, handle exception
        } catch (IllegalArgumentException dataExtensionImplementationIsInvalid) {
            logger.log(Level.SEVERE, "Plan extension hook failed to register. Data hook implementation is invalid.", dataExtensionImplementationIsInvalid);
            // The DataExtension implementation has an implementation error, handle exception
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
