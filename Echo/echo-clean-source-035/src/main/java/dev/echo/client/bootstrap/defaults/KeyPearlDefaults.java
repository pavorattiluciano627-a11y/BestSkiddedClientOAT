package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.utility.KeyPearlFeature;

/** Constructs, assigns, collects and post-initializes Key Pearl. */
public final class KeyPearlDefaults {
    private KeyPearlDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(KeyPearlFeature feature) {
        dev.echo.client.setting.BooleanSetting preparedSyncToClient = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Sync to Client"), true);
        dev.echo.client.setting.FloatRangeSetting preparedThrowDelay = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("Throw Delay"), 0.0f, 0.0f, 0.0f, 10.0f, 1.0f, CatalogText.literal(" ticks"));
        dev.echo.client.setting.KeybindSetting preparedActivateKey = new dev.echo.client.setting.KeybindSetting(CatalogText.literal("Activate Key"), dev.echo.client.input.InputKey.NONE);
        dev.echo.client.setting.BooleanSetting preparedSwapBack = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Swap Back"), true);
        dev.echo.client.setting.FloatRangeSetting preparedSwapDelay = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("Swap Delay"), 1.0f, 1.0f, 0.0f, 10.0f, 1.0f, CatalogText.literal(" ticks"));

        KeyPearlFeature.assignBootstrapSettings(feature, preparedSyncToClient, preparedThrowDelay, preparedActivateKey, preparedSwapBack, preparedSwapDelay);

        feature.settings.clear();
        dev.echo.client.features.impl.utility.KeyPearlFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
