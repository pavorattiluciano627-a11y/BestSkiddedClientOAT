package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.movement.NoSlowFeature;

/** Constructs, assigns, collects and post-initializes No Slow. */
public final class NoSlowDefaults {
    private NoSlowDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(NoSlowFeature feature) {
        dev.echo.client.setting.IntSetting preparedDelayTicks = new dev.echo.client.setting.IntSetting(CatalogText.literal("Delay Ticks"), 2, 0, 10);
        dev.echo.client.setting.ChoiceSetting preparedMode = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("vanilla"), new java.lang.CharSequence[] {CatalogText.literal("vanilla"), CatalogText.literal("Grim")});
        dev.echo.client.setting.BooleanSetting preparedEnableSneak = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Enable Sneak"), false);

        NoSlowFeature.assignBootstrapSettings(feature, preparedDelayTicks, preparedMode, preparedEnableSneak);

        feature.settings.clear();
        dev.echo.client.features.impl.movement.NoSlowFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
