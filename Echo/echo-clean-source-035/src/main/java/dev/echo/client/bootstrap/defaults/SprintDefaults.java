package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.movement.SprintFeature;

/** Constructs, assigns, collects and post-initializes Sprint. */
public final class SprintDefaults {
    private SprintDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(SprintFeature feature) {
        dev.echo.client.setting.BooleanSetting preparedOmni = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Omni"), false);

        SprintFeature.assignBootstrapSettings(feature, preparedOmni);

        feature.settings.clear();
        dev.echo.client.features.impl.movement.SprintFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
