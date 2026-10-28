package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.world.AutoToolFeature;

/** Constructs, assigns, collects and post-initializes Auto Tool. */
public final class AutoToolDefaults {
    private AutoToolDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(AutoToolFeature feature) {
        dev.echo.client.setting.BooleanSetting preparedRequireClick = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Require click"), true);
        dev.echo.client.setting.BooleanSetting preparedSwitchBack = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Switch back"), true);
        dev.echo.client.setting.BooleanSetting preparedRequireShift = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Require shift"), true);

        AutoToolFeature.assignBootstrapSettings(feature, preparedRequireClick, preparedSwitchBack, preparedRequireShift);

        feature.settings.clear();
        dev.echo.client.features.impl.world.AutoToolFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
