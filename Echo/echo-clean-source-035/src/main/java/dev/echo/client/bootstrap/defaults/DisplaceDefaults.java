package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.combat.DisplaceFeature;

/** Constructs, assigns, collects and post-initializes Displace. */
public final class DisplaceDefaults {
    private DisplaceDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(DisplaceFeature feature) {
        dev.echo.client.setting.FloatSetting preparedYawOffset = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Yaw Offset"), 90.0f, 0.0f, 180.0f, 5.0f, CatalogText.literal(" deg"));
        dev.echo.client.setting.ChoiceSetting preparedDirection = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Direction"), dev.echo.client.features.impl.combat.DisplaceFeature.bootstrapDirectionOption(), new java.lang.CharSequence[] {dev.echo.client.features.impl.combat.DisplaceFeature.bootstrapDirectionOption3(), dev.echo.client.features.impl.combat.DisplaceFeature.bootstrapDirectionOption2(), dev.echo.client.features.impl.combat.DisplaceFeature.bootstrapDirectionOption()});
        dev.echo.client.setting.BooleanSetting preparedDynamic = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Dynamic"), false);
        dev.echo.client.setting.BooleanSetting preparedKnockbackEnchant = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Knockback Enchant"), true);

        DisplaceFeature.assignBootstrapSettings(feature, preparedYawOffset, preparedDirection, preparedDynamic, preparedKnockbackEnchant);

        feature.settings.clear();
        dev.echo.client.features.impl.combat.DisplaceFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
