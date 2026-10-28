package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.render.AimVisualizerFeature;

/** Constructs, assigns, collects and post-initializes Aim Visualizer. */
public final class AimVisualizerDefaults {
    private AimVisualizerDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(AimVisualizerFeature feature) {
        dev.echo.client.setting.FloatSetting preparedSize = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Size"), 0.15f, 0.05f, 0.5f, 0.01f, CatalogText.literal(" blocks"));
        dev.echo.client.setting.ColorSetting preparedServerColor = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Server Color"), 255, 0, 0, 200);
        dev.echo.client.setting.ColorSetting preparedClientColor = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Client Color"), 0, 0, 255, 200);

        AimVisualizerFeature.assignBootstrapSettings(feature, preparedSize, preparedServerColor, preparedClientColor);

        feature.settings.clear();
        dev.echo.client.features.impl.render.AimVisualizerFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
