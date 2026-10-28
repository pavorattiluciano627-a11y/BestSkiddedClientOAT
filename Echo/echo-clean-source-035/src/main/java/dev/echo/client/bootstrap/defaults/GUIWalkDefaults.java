package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.movement.GUIWalkFeature;

/** Constructs, assigns, collects and post-initializes GUI Walk. */
public final class GUIWalkDefaults {
    private GUIWalkDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(GUIWalkFeature feature) {
        dev.echo.client.setting.ChoiceSetting preparedMode = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("Vanilla"), new java.lang.CharSequence[] {CatalogText.literal("Vanilla"), CatalogText.literal("Grim")});

        GUIWalkFeature.assignBootstrapSettings(feature, preparedMode);

        feature.settings.clear();
        dev.echo.client.features.impl.movement.GUIWalkFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
