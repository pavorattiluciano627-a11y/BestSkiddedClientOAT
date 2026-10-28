package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.render.ChamsFeature;

/** Constructs, assigns, collects and post-initializes Chams. */
public final class ChamsDefaults {
    private ChamsDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(ChamsFeature feature) {
        dev.echo.client.setting.FloatSetting preparedSpeed = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Speed"), 0.5f, 0.0f, 3.0f, 0.1f, CatalogText.literal("x"));
        dev.echo.client.setting.ColorSetting preparedColor1 = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Color 1"), dev.echo.client.ui.ThemePalette.bootstrapPrimaryColor());
        dev.echo.client.setting.BooleanSetting preparedRenderSelf = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Render Self"), false);
        dev.echo.client.setting.FloatSetting preparedGradientScale = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Gradient Scale"), 1.0f, 0.25f, 4.0f, 0.25f, CatalogText.literal("x"));
        dev.echo.client.setting.FloatSetting preparedGlowRadius = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Glow Radius"), 12.0f, 0.0f, 32.0f, 1.0f);
        dev.echo.client.setting.FloatSetting preparedLineWidth = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Line Width"), 2.0f, 1.0f, 8.0f, 0.5f);
        dev.echo.client.setting.ColorSetting preparedColor2 = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Color 2"), dev.echo.client.ui.ThemePalette.bootstrapSecondaryColor());
        dev.echo.client.setting.FloatSetting preparedOutlineOpacity = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Outline Opacity"), 1.0f, 0.0f, 1.0f, 0.05f);
        dev.echo.client.setting.BooleanSetting preparedTeamColors = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Team Colors"), false);
        dev.echo.client.setting.FloatSetting preparedFillOpacity = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Fill Opacity"), 0.25f, 0.0f, 1.0f, 0.05f);
        dev.echo.client.setting.FloatSetting preparedGlowIntensity = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Glow Intensity"), 0.75f, 0.0f, 1.0f, 0.05f);

        ChamsFeature.assignBootstrapSettings(feature, preparedSpeed, preparedColor1, preparedRenderSelf, preparedGradientScale, preparedGlowRadius, preparedLineWidth, preparedColor2, preparedOutlineOpacity, preparedTeamColors, preparedFillOpacity, preparedGlowIntensity);

        feature.settings.clear();
        dev.echo.client.features.impl.render.ChamsFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
