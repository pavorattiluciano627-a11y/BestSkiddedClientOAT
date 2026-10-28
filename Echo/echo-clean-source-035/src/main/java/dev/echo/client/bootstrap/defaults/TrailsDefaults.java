package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.render.TrailsFeature;

/** Constructs, assigns, collects and post-initializes Trails. */
public final class TrailsDefaults {
    private TrailsDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(TrailsFeature feature) {
        dev.echo.client.setting.BooleanSetting preparedGradient = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Gradient"), true);
        dev.echo.client.setting.FloatSetting preparedHeight = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Height"), 1.0f, 0.2f, 2.5f, 0.1f, CatalogText.literal(" blocks"));
        dev.echo.client.setting.FloatSetting preparedLifetime = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Lifetime"), 3.0f, 0.5f, 10.0f, 0.5f, CatalogText.literal("s"));
        dev.echo.client.setting.ColorSetting preparedColor = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Color"), 235, 160, 220, 160);

        TrailsFeature.assignBootstrapSettings(feature, preparedGradient, preparedHeight, preparedLifetime, preparedColor);

        feature.settings.clear();
        dev.echo.client.features.impl.render.TrailsFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
