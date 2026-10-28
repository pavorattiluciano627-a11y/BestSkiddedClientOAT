package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.movement.AutoWalkFeature;

/** Constructs, assigns, collects and post-initializes Auto Walk. */
public final class AutoWalkDefaults {
    private AutoWalkDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(AutoWalkFeature feature) {


        feature.settings.clear();
        feature.postInitialize();
    }
}
