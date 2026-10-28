package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.utility.ElytraSwapFeature;

/** Constructs, assigns, collects and post-initializes Elytra Swap. */
public final class ElytraSwapDefaults {
    private ElytraSwapDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(ElytraSwapFeature feature) {
        dev.echo.client.setting.BooleanSetting preparedSwitchBack = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Switch Back"), true);
        dev.echo.client.setting.KeybindSetting preparedActivateKey = new dev.echo.client.setting.KeybindSetting(CatalogText.literal("Activate Key"), dev.echo.client.input.InputKey.NONE);

        ElytraSwapFeature.assignBootstrapSettings(feature, preparedSwitchBack, preparedActivateKey);

        feature.settings.clear();
        dev.echo.client.features.impl.utility.ElytraSwapFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
