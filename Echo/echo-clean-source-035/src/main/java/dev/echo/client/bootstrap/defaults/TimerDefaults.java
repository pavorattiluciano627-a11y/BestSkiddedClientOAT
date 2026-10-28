package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.movement.TimerFeature;

/** Constructs, assigns, collects and post-initializes Timer. */
public final class TimerDefaults {
    private TimerDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(TimerFeature feature) {
        dev.echo.client.setting.FloatSetting preparedTimer = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Timer"), 1.0f, 0.5f, 10.0f, 0.05f, CatalogText.literal("x"));

        TimerFeature.assignBootstrapSettings(feature, preparedTimer);

        feature.settings.clear();
        dev.echo.client.features.impl.movement.TimerFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
