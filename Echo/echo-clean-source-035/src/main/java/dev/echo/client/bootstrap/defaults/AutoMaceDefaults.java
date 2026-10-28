package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.combat.AutoMaceFeature;

/** Constructs, assigns, collects and post-initializes Auto Mace. */
public final class AutoMaceDefaults {
    private AutoMaceDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(AutoMaceFeature feature) {
        dev.echo.client.setting.FloatRangeSetting preparedCps = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("CPS"), 6.0f, 12.0f, 1.0f, 20.0f, 0.5f);
        dev.echo.client.setting.BooleanSetting preparedWorkWithMaceSwap = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Work With Mace Swap"), false);
        dev.echo.client.setting.FloatSetting preparedMultipoint = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Multipoint"), 50.0f, 0.0f, 100.0f, 1.0f, CatalogText.literal("%"));
        dev.echo.client.setting.FloatSetting preparedPitchFOV = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Pitch FOV"), 90.0f, 0.0f, 180.0f, 1.0f, CatalogText.literal(" deg"));
        dev.echo.client.setting.ChoiceSetting preparedTargetPriority = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Target Priority"), CatalogText.literal("Nearest"), new java.lang.CharSequence[] {CatalogText.literal("Nearest"), CatalogText.literal("FOV"), CatalogText.literal("HurtTick")});
        dev.echo.client.setting.FloatSetting preparedRange = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Range"), 6.0f, 1.0f, 50.0f, 0.1f, CatalogText.literal(" blocks"));
        dev.echo.client.setting.BooleanSetting preparedReEquipElytra = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Re-Equip Elytra"), false);
        dev.echo.client.setting.BooleanSetting preparedSilentRotations = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Silent Rotations"), false);
        dev.echo.client.setting.FloatSetting preparedFallDistance = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Fall Distance"), 1.5f, 0.5f, 9.0f, 0.1f, CatalogText.literal(" blocks"));
        dev.echo.client.setting.BooleanSetting preparedAutomaticallyUnequipElytra = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Automatically Unequip Elytra"), false);
        dev.echo.client.setting.BooleanSetting preparedAutoAim = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Auto Aim"), false);
        dev.echo.client.setting.BooleanSetting preparedRandom = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Random"), false);
        dev.echo.client.setting.FloatSetting preparedVerticalSpeed = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Vertical Speed"), 2.0f, 0.0f, 10.0f, 0.1f);
        dev.echo.client.setting.BooleanSetting preparedThroughWalls = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Through Walls"), false);
        dev.echo.client.setting.FloatSetting preparedYawFOV = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Yaw FOV"), 90.0f, 0.0f, 360.0f, 1.0f, CatalogText.literal(" deg"));
        dev.echo.client.setting.ChoiceSetting preparedUnequipTiming = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Unequip Timing"), CatalogText.literal("Late"), new java.lang.CharSequence[] {CatalogText.literal("Normal"), CatalogText.literal("Late")});
        dev.echo.client.setting.FloatSetting preparedHorizontalSpeed = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Horizontal Speed"), 2.0f, 0.0f, 10.0f, 0.1f);
        dev.echo.client.setting.FloatSetting preparedLateWindow = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Late Window"), 6.0f, 5.0f, 12.0f, 1.0f, CatalogText.literal(" ticks"));
        dev.echo.client.setting.ChoiceSetting preparedAimMath = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Aim Math"), CatalogText.literal("Smooth"), new java.lang.CharSequence[] {CatalogText.literal("Smooth"), CatalogText.literal("Natural"), CatalogText.literal("Blatant")});
        dev.echo.client.setting.ChoiceSetting preparedAimVector = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Aim Vector"), CatalogText.literal("Straight"), new java.lang.CharSequence[] {CatalogText.literal("Straight"), CatalogText.literal("Closest")});

        AutoMaceFeature.assignBootstrapSettings(feature, preparedCps, preparedWorkWithMaceSwap, preparedMultipoint, preparedPitchFOV, preparedTargetPriority, preparedRange, preparedReEquipElytra, preparedSilentRotations, preparedFallDistance, preparedAutomaticallyUnequipElytra, preparedAutoAim, preparedRandom, preparedVerticalSpeed, preparedThroughWalls, preparedYawFOV, preparedUnequipTiming, preparedHorizontalSpeed, preparedLateWindow, preparedAimMath, preparedAimVector);

        feature.settings.clear();
        dev.echo.client.features.impl.combat.AutoMaceFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
