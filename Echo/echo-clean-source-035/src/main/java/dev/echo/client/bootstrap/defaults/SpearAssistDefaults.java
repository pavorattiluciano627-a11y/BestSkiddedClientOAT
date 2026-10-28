package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.combat.SpearAssistFeature;

/** Constructs, assigns, collects and post-initializes Spear Assist. */
public final class SpearAssistDefaults {
    private SpearAssistDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(SpearAssistFeature feature) {
        dev.echo.client.setting.ChoiceSetting preparedAimMath = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Aim Math"), CatalogText.literal("Smooth"), new java.lang.CharSequence[] {CatalogText.literal("Smooth"), CatalogText.literal("Natural"), CatalogText.literal("Blatant")});
        dev.echo.client.setting.BooleanSetting preparedThroughWalls = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Through Walls"), false);
        dev.echo.client.setting.ChoiceSetting preparedAimVector = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Aim Vector"), CatalogText.literal("Straight"), new java.lang.CharSequence[] {CatalogText.literal("Straight"), CatalogText.literal("Closest")});
        dev.echo.client.setting.FloatSetting preparedMultipoint = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Multipoint"), 50.0f, 0.0f, 100.0f, 1.0f, CatalogText.literal("%"));
        dev.echo.client.setting.FloatSetting preparedVerticalSpeed = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Vertical Speed"), 2.0f, 0.0f, 10.0f, 0.1f);
        dev.echo.client.setting.ChoiceSetting preparedTargetPriority = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Target Priority"), CatalogText.literal("Nearest"), new java.lang.CharSequence[] {CatalogText.literal("Nearest"), CatalogText.literal("FOV"), CatalogText.literal("HurtTick")});
        dev.echo.client.setting.FloatSetting preparedRange = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Range"), 28.0f, 1.0f, 128.0f, 0.1f, CatalogText.literal(" blocks"));
        dev.echo.client.setting.FloatSetting preparedHorizontalSpeed = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Horizontal Speed"), 2.0f, 0.0f, 10.0f, 0.1f);
        dev.echo.client.setting.FloatSetting preparedFov = new dev.echo.client.setting.FloatSetting(CatalogText.literal("FOV"), 90.0f, 0.0f, 360.0f, 1.0f, CatalogText.literal(" deg"));
        dev.echo.client.setting.BooleanSetting preparedRandom = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Random"), false);
        dev.echo.client.setting.BooleanSetting preparedSilentRotations = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Silent Rotations"), false);

        SpearAssistFeature.assignBootstrapSettings(feature, preparedAimMath, preparedThroughWalls, preparedAimVector, preparedMultipoint, preparedVerticalSpeed, preparedTargetPriority, preparedRange, preparedHorizontalSpeed, preparedFov, preparedRandom, preparedSilentRotations);

        feature.settings.clear();
        dev.echo.client.features.impl.combat.SpearAssistFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
