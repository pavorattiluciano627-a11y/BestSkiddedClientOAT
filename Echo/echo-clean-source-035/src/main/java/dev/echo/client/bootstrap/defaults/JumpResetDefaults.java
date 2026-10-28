package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.movement.JumpResetFeature;

/** Constructs, assigns, collects and post-initializes Jump Reset. */
public final class JumpResetDefaults {
    private JumpResetDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(JumpResetFeature feature) {
        dev.echo.client.setting.MultiChoiceSetting preparedDamageSources = new dev.echo.client.setting.MultiChoiceSetting(CatalogText.literal("Damage Sources"), new java.lang.CharSequence[] {CatalogText.literal("Player"), CatalogText.literal("Mob"), CatalogText.literal("Projectile"), CatalogText.literal("Explosion"), CatalogText.literal("Fall"), CatalogText.literal("Fire"), CatalogText.literal("Magic"), CatalogText.literal("Other")});
        dev.echo.client.setting.FloatSetting preparedChance = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Chance"), 100.0f, 0.0f, 100.0f, 1.0f, CatalogText.literal("%"));
        dev.echo.client.setting.BooleanSetting preparedWebCheck = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Web Check"), true);
        dev.echo.client.setting.BooleanSetting preparedLiquidCheck = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Liquid Check"), true);

        JumpResetFeature.assignBootstrapSettings(feature, preparedDamageSources, preparedChance, preparedWebCheck, preparedLiquidCheck);

        feature.settings.clear();
        dev.echo.client.features.impl.movement.JumpResetFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
