package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.movement.StepFeature;

/** Constructs, assigns, collects and post-initializes Step. */
public final class StepDefaults {
    private StepDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(StepFeature feature) {
        dev.echo.client.setting.BooleanSetting preparedDisableOnSneak = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Disable on sneak"), false);
        dev.echo.client.setting.FloatSetting preparedHeight = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Height"), 1.0f, 1.0f, 3.0f, 0.1f, CatalogText.literal(" blocks"));

        StepFeature.assignBootstrapSettings(feature, preparedDisableOnSneak, preparedHeight);

        feature.settings.clear();
        dev.echo.client.features.impl.movement.StepFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
