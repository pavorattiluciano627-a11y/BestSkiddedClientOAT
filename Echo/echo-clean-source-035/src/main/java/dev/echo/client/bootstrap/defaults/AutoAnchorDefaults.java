package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.combat.AutoAnchorFeature;

/** Constructs, assigns, collects and post-initializes Auto Anchor. */
public final class AutoAnchorDefaults {
    private AutoAnchorDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(AutoAnchorFeature feature) {
        dev.echo.client.setting.FloatSetting preparedMinDamage = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Min Damage"), 6.0f, 0.0f, 20.0f, 0.5f, CatalogText.literal(" dmg"));
        dev.echo.client.setting.BooleanSetting preparedCanPlaceLegit = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Can Place Legit"), true);
        dev.echo.client.setting.FloatRangeSetting preparedSafeBlockDelay = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("Safe block delay"), 0.0f, 1.0f, 0.0f, 10.0f, 1.0f, CatalogText.literal(" ticks"));
        dev.echo.client.setting.ColorSetting preparedOutlineColor = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Outline Color"), java.awt.Color.WHITE.getRGB(), true);
        dev.echo.client.setting.IntSetting preparedSafetyTimeout = new dev.echo.client.setting.IntSetting(CatalogText.literal("Safety Timeout"), 200, 0, 1000, CatalogText.literal(" ms"));
        dev.echo.client.setting.ChoiceSetting preparedMode = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Mode"), dev.echo.client.features.impl.combat.AutoAnchorFeature.bootstrapModeOption(), new java.lang.CharSequence[] {dev.echo.client.features.impl.combat.AutoAnchorFeature.bootstrapModeOption2(), dev.echo.client.features.impl.combat.AutoAnchorFeature.bootstrapModeOption()});
        dev.echo.client.setting.BooleanSetting preparedGlowstoneStart = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Glowstone Start"), false);
        dev.echo.client.setting.FloatRangeSetting preparedChargeDelay = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("Charge Delay"), 1.0f, 1.0f, 0.0f, 20.0f, 1.0f, CatalogText.literal(" ticks"));
        dev.echo.client.setting.BooleanSetting preparedRenderPosition = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Render Position"), true);
        dev.echo.client.setting.BooleanSetting preparedSafeAnchor = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Safe Anchor"), false);
        dev.echo.client.setting.IntSetting preparedExplodeSlot = new dev.echo.client.setting.IntSetting(CatalogText.literal("Explode Slot"), 1, 1, 9);
        dev.echo.client.setting.BooleanSetting preparedRepeat = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Repeat"), true);
        dev.echo.client.setting.ChoiceSetting preparedStyle = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Style"), dev.echo.client.features.impl.combat.AutoAnchorFeature.bootstrapStyleOption(), new java.lang.CharSequence[] {dev.echo.client.features.impl.combat.AutoAnchorFeature.bootstrapStyleOption(), dev.echo.client.features.impl.combat.AutoAnchorFeature.bootstrapStyleOption2()});
        dev.echo.client.setting.FloatRangeSetting preparedCharges = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("Charges"), 1.0f, 1.0f, 1.0f, 4.0f, 1.0f, CatalogText.literal(" charges"));
        dev.echo.client.setting.FloatRangeSetting preparedExplodeDelay = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("Explode Delay"), 1.0f, 1.0f, 0.0f, 20.0f, 1.0f, CatalogText.literal(" ticks"));
        dev.echo.client.setting.BooleanSetting preparedAutoAim = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Auto Aim"), true);
        dev.echo.client.setting.FloatSetting preparedRotationSpeed = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Rotation Speed"), 6.0f, 1.0f, 10.0f, 0.1f);
        dev.echo.client.setting.ColorSetting preparedFillColor = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Fill Color"), dev.echo.client.ui.ThemePalette.bootstrapForegroundColor().getRGB(), true);
        dev.echo.client.setting.KeybindSetting preparedActivateKey = new dev.echo.client.setting.KeybindSetting(CatalogText.literal("Activate Key"), dev.echo.client.input.InputKey.NONE);
        dev.echo.client.setting.BooleanSetting preparedSilentAim = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Silent Aim"), false);

        AutoAnchorFeature.assignBootstrapSettings(feature, preparedMinDamage, preparedCanPlaceLegit, preparedSafeBlockDelay, preparedOutlineColor, preparedSafetyTimeout, preparedMode, preparedGlowstoneStart, preparedChargeDelay, preparedRenderPosition, preparedSafeAnchor, preparedExplodeSlot, preparedRepeat, preparedStyle, preparedCharges, preparedExplodeDelay, preparedAutoAim, preparedRotationSpeed, preparedFillColor, preparedActivateKey, preparedSilentAim);

        feature.settings.clear();
        dev.echo.client.features.impl.combat.AutoAnchorFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
