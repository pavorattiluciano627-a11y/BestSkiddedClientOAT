package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.utility.PearlCatchFeature;

/** Constructs, assigns, collects and post-initializes Pearl Catch. */
public final class PearlCatchDefaults {
    private PearlCatchDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(PearlCatchFeature feature) {
        dev.echo.client.setting.KeybindSetting preparedLowPearlCatch = new dev.echo.client.setting.KeybindSetting(CatalogText.literal("Low Pearl Catch"), dev.echo.client.input.InputKey.NONE);
        dev.echo.client.setting.BooleanSetting preparedAutoAim = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Auto Aim"), true);
        dev.echo.client.setting.KeybindSetting preparedInstantPearlCatch = new dev.echo.client.setting.KeybindSetting(CatalogText.literal("Instant Pearl Catch"), dev.echo.client.input.InputKey.NONE);
        dev.echo.client.setting.FloatRangeSetting preparedHighDelay = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("High Delay"), 2.0f, 2.0f, 0.0f, 10.0f, 1.0f, CatalogText.literal(" ticks"));
        dev.echo.client.setting.BooleanSetting preparedSwitchBack = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Switch Back"), true);
        dev.echo.client.setting.BooleanSetting preparedRetry = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Retry"), true);
        dev.echo.client.setting.KeybindSetting preparedHighPearlCatch = new dev.echo.client.setting.KeybindSetting(CatalogText.literal("High Pearl Catch"), dev.echo.client.input.InputKey.NONE);
        dev.echo.client.setting.BooleanSetting preparedSilentAim = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Silent Aim"), true);

        PearlCatchFeature.assignBootstrapSettings(feature, preparedLowPearlCatch, preparedAutoAim, preparedInstantPearlCatch, preparedHighDelay, preparedSwitchBack, preparedRetry, preparedHighPearlCatch, preparedSilentAim);

        feature.settings.clear();
        dev.echo.client.features.impl.utility.PearlCatchFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
