package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.render.LightESPFeature;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ColorSetting;
import dev.echo.client.setting.IntSetting;

/** Original construction vector, publication barrier, and declaring-class membership order. */
public final class LightESPDefaults {
    private LightESPDefaults() {
    }

    public static void initialize(LightESPFeature feature) {
        IntSetting preparedMaxY = new IntSetting(CatalogText.literal("Max Y"), 0, -64, 319, CatalogText.literal(""));
        IntSetting preparedMinLight = new IntSetting(CatalogText.literal("Min Light"), 5, 0, 15, CatalogText.literal(""));
        BooleanSetting preparedTracers = new BooleanSetting(CatalogText.literal("Tracers"), false);
        BooleanSetting preparedOutline = new BooleanSetting(CatalogText.literal("Outline"), true);
        ColorSetting preparedColor = new ColorSetting(CatalogText.literal("Color"), 255, 128, 0, 200, false);
        LightESPFeature.assignBootstrapSettings(feature, preparedMaxY, preparedMinLight,
                preparedTracers, preparedOutline, preparedColor);
        feature.settings.clear();
        LightESPFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
