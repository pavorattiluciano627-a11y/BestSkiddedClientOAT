package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.movement.DiveBombFeature;

/** Constructs, assigns, collects and post-initializes Dive Bomb. */
public final class DiveBombDefaults {
    private DiveBombDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(DiveBombFeature feature) {
        dev.echo.client.setting.IntSetting preparedTickDelay = new dev.echo.client.setting.IntSetting(CatalogText.literal("Tick Delay"), 2, 0, 6, CatalogText.literal(" ticks"));
        dev.echo.client.setting.BooleanSetting preparedAutoJump = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Auto Jump"), true);
        dev.echo.client.setting.KeybindSetting preparedInstantDiveBomb = new dev.echo.client.setting.KeybindSetting(CatalogText.literal("Instant Dive Bomb"), dev.echo.client.input.InputKey.NONE);
        dev.echo.client.setting.BooleanSetting preparedSilentAim = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Silent Aim"), true);
        dev.echo.client.setting.KeybindSetting preparedDiveBomb = new dev.echo.client.setting.KeybindSetting(CatalogText.literal("Dive Bomb"), dev.echo.client.input.InputKey.NONE);
        dev.echo.client.setting.BooleanSetting preparedSwitchback = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Switchback"), true);

        DiveBombFeature.assignBootstrapSettings(feature, preparedTickDelay, preparedAutoJump, preparedInstantDiveBomb, preparedSilentAim, preparedDiveBomb, preparedSwitchback);

        feature.settings.clear();
        dev.echo.client.features.impl.movement.DiveBombFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
