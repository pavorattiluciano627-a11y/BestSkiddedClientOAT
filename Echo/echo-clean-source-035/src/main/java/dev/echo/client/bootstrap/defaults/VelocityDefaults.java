package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.movement.VelocityFeature;

/** Constructs, assigns, collects and post-initializes Velocity. */
public final class VelocityDefaults {
    private VelocityDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(VelocityFeature feature) {
        dev.echo.client.setting.FloatSetting preparedAmount = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Amount"), 0.0f, 0.0f, 1.0f, 0.01f);
        dev.echo.client.setting.ChoiceSetting preparedMode = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("Cancel"), new java.lang.CharSequence[] {CatalogText.literal("Cancel"), CatalogText.literal("MinemenClub")});
        dev.echo.client.setting.MultiChoiceSetting preparedSources = new dev.echo.client.setting.MultiChoiceSetting(CatalogText.literal("Sources"), new java.lang.CharSequence[] {CatalogText.literal("Hits"), CatalogText.literal("Explosions")});

        VelocityFeature.assignBootstrapSettings(feature, preparedAmount, preparedMode, preparedSources);

        feature.settings.clear();
        dev.echo.client.features.impl.movement.VelocityFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
