package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.render.ParticlesFeature;

/** Constructs, assigns, collects and post-initializes Particles. */
public final class ParticlesDefaults {
    private ParticlesDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(ParticlesFeature feature) {
        dev.echo.client.setting.ColorSetting preparedColor = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Color"), 255, 92, 20, 220);
        dev.echo.client.setting.IntSetting preparedRadius = new dev.echo.client.setting.IntSetting(CatalogText.literal("Radius"), 24, 8, 64, CatalogText.literal(" blocks"));
        dev.echo.client.setting.FloatSetting preparedSpeed = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Speed"), 1.0f, 0.1f, 3.0f, 0.1f, CatalogText.literal("x"));
        dev.echo.client.setting.IntSetting preparedDensity = new dev.echo.client.setting.IntSetting(CatalogText.literal("Density"), 120, 10, 400, CatalogText.literal(" particles"));
        dev.echo.client.setting.BooleanSetting preparedTwinkle = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Twinkle"), true);
        dev.echo.client.setting.FloatSetting preparedSize = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Size"), 1.0f, 0.2f, 3.0f, 0.1f, CatalogText.literal("x"));

        ParticlesFeature.assignBootstrapSettings(feature, preparedColor, preparedRadius, preparedSpeed, preparedDensity, preparedTwinkle, preparedSize);

        feature.settings.clear();
        dev.echo.client.features.impl.render.ParticlesFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
