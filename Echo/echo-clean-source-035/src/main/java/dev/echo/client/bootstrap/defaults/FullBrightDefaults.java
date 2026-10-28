package dev.echo.client.bootstrap.defaults;

import dev.echo.client.features.impl.render.FullBrightFeature;

/** Full Bright has no setting constructions in the original metadata vector. */
public final class FullBrightDefaults {
    private FullBrightDefaults() {
    }

    public static void initialize(FullBrightFeature feature) {
        feature.settings.clear();
        feature.postInitialize();
    }
}
