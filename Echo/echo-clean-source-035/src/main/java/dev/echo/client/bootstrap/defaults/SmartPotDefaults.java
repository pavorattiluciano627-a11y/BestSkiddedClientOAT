package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.utility.SmartPotFeature;

/** Constructs, assigns, collects and post-initializes Smart Pot. */
public final class SmartPotDefaults {
    private SmartPotDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(SmartPotFeature feature) {
        dev.echo.client.setting.BooleanSetting preparedAutoBuff = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Auto Buff"), true);
        dev.echo.client.setting.FloatRangeSetting preparedThrowDelay = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("Throw Delay"), 150.0f, 250.0f, 0.0f, 450.0f, 5.0f);
        dev.echo.client.setting.BooleanSetting preparedThrowPotion = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Throw Potion"), false);
        dev.echo.client.setting.FloatRangeSetting preparedSwapBackDelay = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("Swap Back Delay"), 50.0f, 100.0f, 0.0f, 500.0f, 5.0f);
        dev.echo.client.setting.BooleanSetting preparedSwapBack = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Swap Back"), false);
        dev.echo.client.setting.BooleanSetting preparedSilentAim = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Silent Aim"), true);
        dev.echo.client.setting.BooleanSetting preparedAngleCheck = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Angle Check"), true);
        dev.echo.client.setting.FloatRangeSetting preparedFov = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("FOV"), 0.0f, 20.0f, 0.0f, 90.0f, 1.0f);
        dev.echo.client.setting.FloatRangeSetting preparedMinHealth = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("Min Health"), 16.0f, 16.0f, 0.0f, 20.0f, 1.0f);
        dev.echo.client.setting.BooleanSetting preparedAutoAim = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Auto Aim"), false);

        SmartPotFeature.assignBootstrapSettings(feature, preparedAutoBuff, preparedThrowDelay, preparedThrowPotion, preparedSwapBackDelay, preparedSwapBack, preparedSilentAim, preparedAngleCheck, preparedFov, preparedMinHealth, preparedAutoAim);

        feature.settings.clear();
        dev.echo.client.features.impl.utility.SmartPotFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
