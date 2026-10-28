package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.render.TargetESPFeature;

/** Constructs, assigns, collects and post-initializes Target ESP. */
public final class TargetESPDefaults {
    private TargetESPDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(TargetESPFeature feature) {
        dev.echo.client.setting.ColorSetting preparedColor = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Color"), dev.echo.client.ui.ThemePalette.bootstrapPrimaryColor().getRed(), dev.echo.client.ui.ThemePalette.bootstrapPrimaryColor().getGreen(), dev.echo.client.ui.ThemePalette.bootstrapPrimaryColor().getBlue(), 200, true);
        dev.echo.client.setting.ChoiceSetting preparedMode = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Mode"), dev.echo.client.features.impl.render.TargetESPFeature.bootstrapModeOption(), new java.lang.CharSequence[] {dev.echo.client.features.impl.render.TargetESPFeature.bootstrapModeOption()});

        TargetESPFeature.assignBootstrapSettings(feature, preparedColor, preparedMode);

        feature.settings.clear();
        dev.echo.client.features.impl.render.TargetESPFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
