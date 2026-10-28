package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.movement.KeepSprintFeature;

/** Constructs, assigns, collects and post-initializes Keep Sprint. */
public final class KeepSprintDefaults {
    private KeepSprintDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(KeepSprintFeature feature) {
        dev.echo.client.setting.MultiChoiceSetting preparedSprintCondition = new dev.echo.client.setting.MultiChoiceSetting(CatalogText.literal("Sprint Condition"), new java.lang.CharSequence[] {CatalogText.literal("Air"), CatalogText.literal("Ground")});
        dev.echo.client.setting.FloatSetting preparedSpeedKeptVelocity = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Speed Kept Velocity"), 0.6f, 0.6f, 1.0f, 0.1f);
        dev.echo.client.setting.BooleanSetting preparedShouldSprint = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Should Sprint"), true);

        KeepSprintFeature.assignBootstrapSettings(feature, preparedSprintCondition, preparedSpeedKeptVelocity, preparedShouldSprint);

        feature.settings.clear();
        dev.echo.client.features.impl.movement.KeepSprintFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
