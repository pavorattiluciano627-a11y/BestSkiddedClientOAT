package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.utility.MLGFeature;

/** Constructs, assigns, collects and post-initializes MLG. */
public final class MLGDefaults {
    private MLGDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(MLGFeature feature) {
        dev.echo.client.setting.IntSetting preparedDistanceToGround = new dev.echo.client.setting.IntSetting(CatalogText.literal("Distance To Ground"), 24, 1, 24, CatalogText.literal(" blocks"));
        dev.echo.client.setting.MultiChoiceSetting preparedItems = new dev.echo.client.setting.MultiChoiceSetting(CatalogText.literal("Items"), dev.echo.client.features.impl.utility.MLGFeature.bootstrapItemsOptionSet());
        dev.echo.client.setting.IntSetting preparedFallDistanceThreshold = new dev.echo.client.setting.IntSetting(CatalogText.literal("Fall Distance Threshold"), 5, 0, 20, CatalogText.literal(" blocks"));
        dev.echo.client.setting.BooleanSetting preparedAutoAim = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Auto Aim"), true);
        dev.echo.client.setting.BooleanSetting preparedSilent = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Silent"), true);

        MLGFeature.assignBootstrapSettings(feature, preparedDistanceToGround, preparedItems, preparedFallDistanceThreshold, preparedAutoAim, preparedSilent);

        feature.settings.clear();
        dev.echo.client.features.impl.utility.MLGFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
