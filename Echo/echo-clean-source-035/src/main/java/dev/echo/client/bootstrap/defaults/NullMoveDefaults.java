package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.movement.NullMoveFeature;

/** Constructs, assigns, collects and post-initializes Null Move. */
public final class NullMoveDefaults {
    private NullMoveDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(NullMoveFeature feature) {


        feature.settings.clear();
        feature.postInitialize();
    }
}
