package net.william278.husksync.hook;

import com.djrapitops.plan.extension.extractor.ExtensionExtractor;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link PlanHook} and {@link PlanDataExtension} implementation
 */
public class PlanHookTests {

    /**
     * Throws IllegalArgumentException if there is an implementation error or warning.
     */
    @Test
    public void testExtensionImplementationErrors() {
        new ExtensionExtractor(new PlanDataExtension()).validateAnnotations();
    }

}
