package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.world.TunnelerFeature;

/** Constructs, assigns, collects and post-initializes Tunneler. */
public final class TunnelerDefaults {
    private TunnelerDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(TunnelerFeature feature) {
        dev.echo.client.setting.BooleanSetting preparedSilent = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Silent"), true);
        dev.echo.client.setting.ChoiceSetting preparedMode = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("2x1"), new java.lang.CharSequence[] {CatalogText.literal("2x1"), CatalogText.literal("1x1")});

        TunnelerFeature.assignBootstrapSettings(feature, preparedSilent, preparedMode);

        feature.settings.clear();
        dev.echo.client.features.impl.world.TunnelerFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
