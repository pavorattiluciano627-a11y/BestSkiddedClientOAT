package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.utility.PotRefillFeature;

/** Constructs, assigns, collects and post-initializes Pot Refill. */
public final class PotRefillDefaults {
    private PotRefillDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(PotRefillFeature feature) {
        dev.echo.client.setting.ChoiceSetting preparedMode = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Mode"), dev.echo.client.features.impl.utility.PotRefillFeature.bootstrapModeOption3(), new java.lang.CharSequence[] {dev.echo.client.features.impl.utility.PotRefillFeature.bootstrapModeOption3(), dev.echo.client.features.impl.utility.PotRefillFeature.bootstrapModeOption(), dev.echo.client.features.impl.utility.PotRefillFeature.bootstrapModeOption2()});
        dev.echo.client.setting.BooleanSetting preparedRefillBuffPotions = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Refill Buff Potions"), true);
        dev.echo.client.setting.FloatRangeSetting preparedDelay = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("Delay"), 10.0f, 50.0f, 0.0f, 400.0f, 5.0f, CatalogText.literal("ms"));

        PotRefillFeature.assignBootstrapSettings(feature, preparedMode, preparedRefillBuffPotions, preparedDelay);

        feature.settings.clear();
        dev.echo.client.features.impl.utility.PotRefillFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
