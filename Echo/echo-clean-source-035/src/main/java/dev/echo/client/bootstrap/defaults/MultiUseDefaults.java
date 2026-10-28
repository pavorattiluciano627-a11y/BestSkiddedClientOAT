package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.feature.utility.MultiUseFeature;

/** Constructs, assigns, collects and post-initializes Multi Use. */
public final class MultiUseDefaults {
    private MultiUseDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(MultiUseFeature feature) {


        feature.settings.clear();
        feature.postInitialize();
    }
}
