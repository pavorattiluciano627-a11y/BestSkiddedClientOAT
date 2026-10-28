package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.render.FreecamFeature;

/** Constructs, assigns, collects and post-initializes Freecam. */
public final class FreecamDefaults {
    private FreecamDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(FreecamFeature feature) {
        dev.echo.client.setting.FloatSetting preparedSpeed = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Speed"), 1.0f, 1.0f, 5.0f, 0.1f);

        FreecamFeature.assignBootstrapSettings(feature, preparedSpeed);

        feature.settings.clear();
        dev.echo.client.features.impl.render.FreecamFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
