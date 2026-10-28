package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.combat.TotemHitFeature;

/** Constructs, assigns, collects and post-initializes Totem Hit. */
public final class TotemHitDefaults {
    private TotemHitDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(TotemHitFeature feature) {
        dev.echo.client.setting.FloatRangeSetting preparedDelay = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("Delay"), 1.0f, 1.0f, 1.0f, 20.0f, 1.0f);
        dev.echo.client.setting.BooleanSetting preparedSilentSwap = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Silent Swap"), false);

        TotemHitFeature.assignBootstrapSettings(feature, preparedDelay, preparedSilentSwap);

        feature.settings.clear();
        dev.echo.client.features.impl.combat.TotemHitFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
