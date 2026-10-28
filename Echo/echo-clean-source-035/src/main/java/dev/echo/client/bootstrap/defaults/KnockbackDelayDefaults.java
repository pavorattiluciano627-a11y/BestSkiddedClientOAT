package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.combat.KnockbackDelayFeature;

/** Constructs, assigns, collects and post-initializes Knockback Delay. */
public final class KnockbackDelayDefaults {
    private KnockbackDelayDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(KnockbackDelayFeature feature) {
        dev.echo.client.setting.FloatRangeSetting preparedGroundDelay = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("Ground Delay"), 50.0f, 150.0f, 0.0f, 5000.0f, 1.0f, dev.echo.client.features.impl.combat.KnockbackDelayFeature.bootstrapGroundDelayOption());
        dev.echo.client.setting.FloatRangeSetting preparedAirDelay = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("Air Delay"), 100.0f, 250.0f, 0.0f, 5000.0f, 1.0f, dev.echo.client.features.impl.combat.KnockbackDelayFeature.bootstrapGroundDelayOption());
        dev.echo.client.setting.IntSetting preparedChance = new dev.echo.client.setting.IntSetting(CatalogText.literal("Chance"), 100, 0, 100, dev.echo.client.features.impl.combat.KnockbackDelayFeature.bootstrapChanceOption());

        KnockbackDelayFeature.assignBootstrapSettings(feature, preparedGroundDelay, preparedAirDelay, preparedChance);

        feature.settings.clear();
        dev.echo.client.features.impl.combat.KnockbackDelayFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
