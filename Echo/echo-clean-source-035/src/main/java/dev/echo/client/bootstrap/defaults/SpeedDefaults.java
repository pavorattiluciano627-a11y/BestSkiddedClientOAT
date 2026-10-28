package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.movement.SpeedFeature;

/** Constructs, assigns, collects and post-initializes Speed. */
public final class SpeedDefaults {
    private SpeedDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(SpeedFeature feature) {
        dev.echo.client.setting.FloatSetting preparedStrafe = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Strafe"), 1.0f, 0.01f, 10.0f, 0.01f);
        dev.echo.client.setting.FloatSetting preparedBoatBoost = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Boat Boost"), 0.5f, 0.01f, 0.75f, 0.01f);
        dev.echo.client.setting.FloatSetting preparedMotion = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Motion"), 1.0f, 0.01f, 10.0f, 0.01f);
        dev.echo.client.setting.FloatSetting preparedBoost = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Boost"), 0.1f, 0.01f, 0.5f, 0.01f);
        dev.echo.client.setting.ChoiceSetting preparedMode = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("MinemenClub"), new java.lang.CharSequence[] {CatalogText.literal("MinemenClub"), CatalogText.literal("Legit"), CatalogText.literal("Grim Entity"), CatalogText.literal("Motion"), CatalogText.literal("Strafe")});

        SpeedFeature.assignBootstrapSettings(feature, preparedStrafe, preparedBoatBoost, preparedMotion, preparedBoost, preparedMode);

        feature.settings.clear();
        dev.echo.client.features.impl.movement.SpeedFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
