package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.movement.FastStairsFeature;

/** Constructs, assigns, collects and post-initializes Fast Stairs. */
public final class FastStairsDefaults {
    private FastStairsDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(FastStairsFeature feature) {


        feature.settings.clear();
        feature.postInitialize();
    }
}
