package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.utility.PearlGrappleFeature;

/** Constructs, assigns, collects and post-initializes Pearl Grapple. */
public final class PearlGrappleDefaults {
    private PearlGrappleDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(PearlGrappleFeature feature) {
        dev.echo.client.setting.IntSetting preparedMaxPredictTicks = new dev.echo.client.setting.IntSetting(CatalogText.literal("Max Predict Ticks"), 60, 5, 80, CatalogText.literal(" ticks"));
        dev.echo.client.setting.FloatSetting preparedDistance = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Distance"), 48.0f, 3.0f, 48.0f, 1.0f, CatalogText.literal(" blocks"));
        dev.echo.client.setting.BooleanSetting preparedSilentRotations = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Silent Rotations"), true);

        PearlGrappleFeature.assignBootstrapSettings(feature, preparedMaxPredictTicks, preparedDistance, preparedSilentRotations);

        feature.settings.clear();
        dev.echo.client.features.impl.utility.PearlGrappleFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
