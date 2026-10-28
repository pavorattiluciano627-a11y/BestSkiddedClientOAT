package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.movement.SprintResetFeature;

/** Constructs, assigns, collects and post-initializes Sprint Reset. */
public final class SprintResetDefaults {
    private SprintResetDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(SprintResetFeature feature) {
        dev.echo.client.setting.IntSetting preparedDurationBetweenReset = new dev.echo.client.setting.IntSetting(CatalogText.literal("Duration Between Reset"), 0, 0, 20, CatalogText.literal(" Ticks"));
        dev.echo.client.setting.IntSetting preparedDuration = new dev.echo.client.setting.IntSetting(CatalogText.literal("Duration"), 3, 1, 20, CatalogText.literal(" Ticks"));
        dev.echo.client.setting.BooleanSetting preparedGroundOnly = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Ground Only"), false);
        dev.echo.client.setting.IntSetting preparedReactionTime = new dev.echo.client.setting.IntSetting(CatalogText.literal("Reaction Time"), 0, 0, 20, CatalogText.literal(" Ticks"));

        SprintResetFeature.assignBootstrapSettings(feature, preparedDurationBetweenReset, preparedDuration, preparedGroundOnly, preparedReactionTime);

        feature.settings.clear();
        dev.echo.client.features.impl.movement.SprintResetFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
