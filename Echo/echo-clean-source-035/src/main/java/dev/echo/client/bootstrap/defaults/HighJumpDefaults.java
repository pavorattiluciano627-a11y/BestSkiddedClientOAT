package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.movement.HighJumpFeature;

/** Constructs, assigns, collects and post-initializes High Jump. */
public final class HighJumpDefaults {
    private HighJumpDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(HighJumpFeature feature) {
        dev.echo.client.setting.ChoiceSetting preparedMode = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("Normal"), new java.lang.CharSequence[] {CatalogText.literal("Normal"), CatalogText.literal("Shulker")});
        dev.echo.client.setting.IntSetting preparedTicks = new dev.echo.client.setting.IntSetting(CatalogText.literal("Ticks"), 6, 1, 20, CatalogText.literal(" ticks"));
        dev.echo.client.setting.FloatSetting preparedLift = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Lift"), 0.51f, 0.1f, 1.85f, 0.01f);

        HighJumpFeature.assignBootstrapSettings(feature, preparedMode, preparedTicks, preparedLift);

        feature.settings.clear();
        dev.echo.client.features.impl.movement.HighJumpFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
