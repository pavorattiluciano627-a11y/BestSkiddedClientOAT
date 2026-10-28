package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.movement.NoJumpDelayFeature;

/** Constructs, assigns, collects and post-initializes No Jump Delay. */
public final class NoJumpDelayDefaults {
    private NoJumpDelayDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(NoJumpDelayFeature feature) {


        feature.settings.clear();
        feature.postInitialize();
    }
}
