package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.render.ChinaHatFeature;

/** Constructs, assigns, collects and post-initializes China Hat. */
public final class ChinaHatDefaults {
    private ChinaHatDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(ChinaHatFeature feature) {
        dev.echo.client.setting.FloatSetting preparedHeight = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Height"), 0.31f, 0.1f, 0.8f, 0.01f, CatalogText.literal(" blocks"));
        dev.echo.client.setting.ChoiceSetting preparedMode = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("Rainbow"), new java.lang.CharSequence[] {CatalogText.literal("Static"), CatalogText.literal("Rainbow")});
        dev.echo.client.setting.FloatSetting preparedSpinSpeed = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Spin Speed"), 0.25f, 0.0f, 2.0f, 0.05f);
        dev.echo.client.setting.ColorSetting preparedColor = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Color"), 235, 160, 220, 185);
        dev.echo.client.setting.BooleanSetting preparedOutline = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Outline"), false);
        dev.echo.client.setting.FloatSetting preparedRadius = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Radius"), 0.55f, 0.3f, 1.2f, 0.05f, CatalogText.literal(" blocks"));
        dev.echo.client.setting.FloatSetting preparedOpacity = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Opacity"), 0.9f, 0.0f, 1.0f, 0.05f);

        ChinaHatFeature.assignBootstrapSettings(feature, preparedHeight, preparedMode, preparedSpinSpeed, preparedColor, preparedOutline, preparedRadius, preparedOpacity);

        feature.settings.clear();
        dev.echo.client.features.impl.render.ChinaHatFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
