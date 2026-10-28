package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.combat.AimAssistFeature;

/** Constructs, assigns, collects and post-initializes Aim Assist. */
public final class AimAssistDefaults {
    private AimAssistDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(AimAssistFeature feature) {
        dev.echo.client.setting.ChoiceSetting preparedFovMode = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("FOV Mode"), CatalogText.literal("Single"), new java.lang.CharSequence[] {CatalogText.literal("Single"), CatalogText.literal("Split")});
        dev.echo.client.setting.FloatSetting preparedRange = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Range"), 6.0f, 1.0f, 50.0f, 0.1f, CatalogText.literal(" blocks"));
        dev.echo.client.setting.FloatSetting preparedPitchFOV = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Pitch FOV"), 90.0f, 0.0f, 180.0f, 1.0f, CatalogText.literal(" deg"));
        dev.echo.client.setting.ItemListSetting preparedItemWhitelist = new dev.echo.client.setting.ItemListSetting(CatalogText.literal("Item Whitelist"));
        dev.echo.client.setting.BooleanSetting preparedRandom = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Random"), false);
        dev.echo.client.setting.BooleanSetting preparedSilentRotations = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Silent Rotations"), false);
        dev.echo.client.setting.FloatSetting preparedMultipoint = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Multipoint"), 50.0f, 0.0f, 100.0f, 1.0f, CatalogText.literal("%"));
        dev.echo.client.setting.BooleanSetting preparedOnClick = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("On Click"), false);
        dev.echo.client.setting.ChoiceSetting preparedTargetPriority = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Target Priority"), CatalogText.literal("Nearest"), new java.lang.CharSequence[] {CatalogText.literal("Nearest"), CatalogText.literal("FOV"), CatalogText.literal("HurtTick")});
        dev.echo.client.setting.BooleanSetting preparedThroughWalls = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Through Walls"), false);
        dev.echo.client.setting.FloatSetting preparedVerticalSpeed = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Vertical Speed"), 2.0f, 0.0f, 10.0f, 0.1f);
        dev.echo.client.setting.HotbarSlotsSetting preparedHotbarWhitelist = new dev.echo.client.setting.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));
        dev.echo.client.setting.FloatSetting preparedHorizontalSpeed = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Horizontal Speed"), 5.0f, 0.0f, 10.0f, 0.1f);
        dev.echo.client.setting.ChoiceSetting preparedAimMath = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Aim Math"), CatalogText.literal("Smooth"), new java.lang.CharSequence[] {CatalogText.literal("Smooth"), CatalogText.literal("Natural"), CatalogText.literal("Blatant")});
        dev.echo.client.setting.ChoiceSetting preparedAimVector = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Aim Vector"), CatalogText.literal("Straight"), new java.lang.CharSequence[] {CatalogText.literal("Straight"), CatalogText.literal("Closest")});
        dev.echo.client.setting.FloatSetting preparedFov = new dev.echo.client.setting.FloatSetting(CatalogText.literal("FOV"), 90.0f, 0.0f, 360.0f, 1.0f, CatalogText.literal(" deg"));
        dev.echo.client.setting.FloatSetting preparedYawFOV = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Yaw FOV"), 90.0f, 0.0f, 360.0f, 1.0f, CatalogText.literal(" deg"));

        AimAssistFeature.assignBootstrapSettings(feature, preparedFovMode, preparedRange, preparedPitchFOV, preparedItemWhitelist, preparedRandom, preparedSilentRotations, preparedMultipoint, preparedOnClick, preparedTargetPriority, preparedThroughWalls, preparedVerticalSpeed, preparedHotbarWhitelist, preparedHorizontalSpeed, preparedAimMath, preparedAimVector, preparedFov, preparedYawFOV);

        feature.settings.clear();
        dev.echo.client.features.impl.combat.AimAssistFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
