package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.movement.KeyChargeFeature;

/** Constructs, assigns, collects and post-initializes Key Charge. */
public final class KeyChargeDefaults {
    private KeyChargeDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(KeyChargeFeature feature) {
        dev.echo.client.setting.BooleanSetting preparedSwitchback = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Switchback"), true);
        dev.echo.client.setting.KeybindSetting preparedActivateKey = new dev.echo.client.setting.KeybindSetting(CatalogText.literal("Activate Key"), dev.echo.client.input.InputKey.NONE);
        dev.echo.client.setting.BooleanSetting preparedSilentSwap = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Silent Swap"), true);
        dev.echo.client.setting.BooleanSetting preparedAutoJump = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Auto Jump"), true);
        dev.echo.client.setting.BooleanSetting preparedSilentAim = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Silent Aim"), true);
        dev.echo.client.setting.BooleanSetting preparedAutoAim = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Auto Aim"), true);

        KeyChargeFeature.assignBootstrapSettings(feature, preparedSwitchback, preparedActivateKey, preparedSilentSwap, preparedAutoJump, preparedSilentAim, preparedAutoAim);

        feature.settings.clear();
        dev.echo.client.features.impl.movement.KeyChargeFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
