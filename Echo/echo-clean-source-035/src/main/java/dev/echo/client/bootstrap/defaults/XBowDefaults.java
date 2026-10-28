package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.combat.XBowFeature;

/** Constructs, assigns, collects and post-initializes XBow. */
public final class XBowDefaults {
    private XBowDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(XBowFeature feature) {
        dev.echo.client.setting.MultiChoiceSetting preparedPlace = new dev.echo.client.setting.MultiChoiceSetting(CatalogText.literal("Place"), new java.lang.CharSequence[] {dev.echo.client.features.impl.combat.XBowFeature.bootstrapPlaceOption2(), dev.echo.client.features.impl.combat.XBowFeature.bootstrapPlaceOption4(), dev.echo.client.features.impl.combat.XBowFeature.bootstrapPlaceOption3(), dev.echo.client.features.impl.combat.XBowFeature.bootstrapPlaceOption()});
        dev.echo.client.setting.KeybindSetting preparedActivateKey = new dev.echo.client.setting.KeybindSetting(CatalogText.literal("Activate Key"), dev.echo.client.input.InputKey.NONE);
        dev.echo.client.setting.BooleanSetting preparedAutoAim = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Auto Aim"), true);
        dev.echo.client.setting.BooleanSetting preparedSilentRotations = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Silent Rotations"), true);

        XBowFeature.assignBootstrapSettings(feature, preparedPlace, preparedActivateKey, preparedAutoAim, preparedSilentRotations);

        feature.settings.clear();
        dev.echo.client.features.impl.combat.XBowFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
