package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.world.AttributeMineFeature;

/** Constructs, assigns, collects and post-initializes Attribute Mine. */
public final class AttributeMineDefaults {
    private AttributeMineDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(AttributeMineFeature feature) {


        feature.settings.clear();
        feature.postInitialize();
    }
}
