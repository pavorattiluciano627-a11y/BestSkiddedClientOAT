package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.utility.AutoTrapFeature;

/** Constructs, assigns, collects and post-initializes Auto Trap. */
public final class AutoTrapDefaults {
    private AutoTrapDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(AutoTrapFeature feature) {
        dev.echo.client.setting.BooleanSetting preparedSilentRotations = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Silent Rotations"), true);
        dev.echo.client.setting.IntSetting preparedPlaceDelay = new dev.echo.client.setting.IntSetting(CatalogText.literal("Place Delay"), 1, 0, 4, CatalogText.literal(" ticks"));
        dev.echo.client.setting.BooleanSetting preparedOnlyStationary = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Only Stationary"), true);
        dev.echo.client.setting.ChoiceSetting preparedMode = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Mode"), dev.echo.client.features.impl.utility.AutoTrapFeature.bootstrapModeOption2(), new java.lang.CharSequence[] {dev.echo.client.features.impl.utility.AutoTrapFeature.bootstrapModeOption2(), dev.echo.client.features.impl.utility.AutoTrapFeature.bootstrapModeOption()});

        AutoTrapFeature.assignBootstrapSettings(feature, preparedSilentRotations, preparedPlaceDelay, preparedOnlyStationary, preparedMode);

        feature.settings.clear();
        dev.echo.client.features.impl.utility.AutoTrapFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
