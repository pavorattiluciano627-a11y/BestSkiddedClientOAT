package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.world.NoBreakDelayFeature;

/** Constructs, assigns, collects and post-initializes No Break Delay. */
public final class NoBreakDelayDefaults {
    private NoBreakDelayDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(NoBreakDelayFeature feature) {


        feature.settings.clear();
        feature.postInitialize();
    }
}
