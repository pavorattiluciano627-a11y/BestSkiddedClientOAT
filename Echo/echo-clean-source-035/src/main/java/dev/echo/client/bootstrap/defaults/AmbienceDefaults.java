package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.render.AmbienceFeature;

/** Constructs, assigns, collects and post-initializes Ambience. */
public final class AmbienceDefaults {
    private AmbienceDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(AmbienceFeature feature) {
        dev.echo.client.setting.ColorSetting preparedGradientColor = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Gradient color"), dev.echo.client.ui.ThemePalette.bootstrapSecondaryColor(), false);
        dev.echo.client.setting.ColorSetting preparedFogColorColor = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Fog color"), dev.echo.client.ui.ThemePalette.bootstrapPrimaryColor(), false);
        dev.echo.client.setting.IntSetting preparedTime = new dev.echo.client.setting.IntSetting(CatalogText.literal("Time"), 18000, 0, 24000, CatalogText.literal(" ticks"));
        dev.echo.client.setting.BooleanSetting preparedFogColorBoolean = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Fog color"), false);
        dev.echo.client.setting.BooleanSetting preparedGradient = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Gradient"), true);
        dev.echo.client.setting.ChoiceSetting preparedWeather = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Weather"), CatalogText.literal("Clear"), new java.lang.CharSequence[] {CatalogText.literal("Clear"), CatalogText.literal("Rain"), CatalogText.literal("Thunder")});
        dev.echo.client.setting.IntSetting preparedFogDistance = new dev.echo.client.setting.IntSetting(CatalogText.literal("Fog distance"), 16, 0, 48, CatalogText.literal(" chunks"));
        dev.echo.client.setting.BooleanSetting preparedCustomTime = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Custom time"), false);
        dev.echo.client.setting.BooleanSetting preparedModifyWeather = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Modify weather"), false);

        AmbienceFeature.assignBootstrapSettings(feature, preparedGradientColor, preparedFogColorColor, preparedTime, preparedFogColorBoolean, preparedGradient, preparedWeather, preparedFogDistance, preparedCustomTime, preparedModifyWeather);

        feature.settings.clear();
        dev.echo.client.features.impl.render.AmbienceFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
