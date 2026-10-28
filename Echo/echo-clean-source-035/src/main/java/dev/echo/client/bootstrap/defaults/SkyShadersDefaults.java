package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.render.SkyShadersFeature;

/** Constructs, assigns, collects and post-initializes Sky Shaders. */
public final class SkyShadersDefaults {
    private SkyShadersDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(SkyShadersFeature feature) {
        dev.echo.client.setting.FloatSetting preparedStars = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Stars"), 1.0f, 0.0f, 2.0f, 0.05f, CatalogText.literal("x"));
        dev.echo.client.setting.ChoiceSetting preparedMode = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("Nebula"), new java.lang.CharSequence[] {CatalogText.literal("Nebula"), CatalogText.literal("Aurora"), CatalogText.literal("Twilight")});
        dev.echo.client.setting.ColorSetting preparedSecondaryColor = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Secondary color"), dev.echo.client.ui.ThemePalette.bootstrapSecondaryColor(), false);
        dev.echo.client.setting.BooleanSetting preparedEndDimension = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("End dimension"), false);
        dev.echo.client.setting.FloatSetting preparedBrightness = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Brightness"), 1.0f, 0.1f, 2.0f, 0.05f, CatalogText.literal("x"));
        dev.echo.client.setting.BooleanSetting preparedCustomColors = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Custom colors"), false);
        dev.echo.client.setting.BooleanSetting preparedSunAndMoon = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Sun & moon"), true);
        dev.echo.client.setting.FloatSetting preparedSpeed = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Speed"), 1.0f, 0.0f, 3.0f, 0.05f, CatalogText.literal("x"));
        dev.echo.client.setting.ColorSetting preparedPrimaryColor = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Primary color"), dev.echo.client.ui.ThemePalette.bootstrapPrimaryColor(), false);

        SkyShadersFeature.assignBootstrapSettings(feature, preparedStars, preparedMode, preparedSecondaryColor, preparedEndDimension, preparedBrightness, preparedCustomColors, preparedSunAndMoon, preparedSpeed, preparedPrimaryColor);

        feature.settings.clear();
        dev.echo.client.features.impl.render.SkyShadersFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
