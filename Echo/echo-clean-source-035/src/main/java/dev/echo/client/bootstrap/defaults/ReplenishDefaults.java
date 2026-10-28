package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.utility.ReplenishFeature;

/** Constructs, assigns, collects and post-initializes Replenish. */
public final class ReplenishDefaults {
    private ReplenishDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(ReplenishFeature feature) {
        dev.echo.client.setting.FloatSetting preparedMinPercent = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Min %"), 25.0f, 0.0f, 100.0f, 1.0f);
        dev.echo.client.setting.IntSetting preparedCursorSpeed = new dev.echo.client.setting.IntSetting(CatalogText.literal("Cursor Speed"), 20, 1, 100);

        ReplenishFeature.assignBootstrapSettings(feature, preparedMinPercent, preparedCursorSpeed);

        feature.settings.clear();
        dev.echo.client.features.impl.utility.ReplenishFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
