package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.combat.HitboxesFeature;

/** Constructs, assigns, collects and post-initializes Hitboxes. */
public final class HitboxesDefaults {
    private HitboxesDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(HitboxesFeature feature) {
        dev.echo.client.setting.BooleanSetting preparedStaticHitbox = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Static Hitbox"), false);
        dev.echo.client.setting.FloatSetting preparedExpandX = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Expand X"), 0.0f, 0.0f, 2.0f, 0.1f, CatalogText.literal(" blocks"));
        dev.echo.client.setting.BooleanSetting preparedUniform = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Uniform"), true);
        dev.echo.client.setting.FloatSetting preparedExpandYUp = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Expand Y Up"), 0.0f, 0.0f, 2.0f, 0.1f, CatalogText.literal(" blocks"));
        dev.echo.client.setting.FloatSetting preparedExpandZ = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Expand Z"), 0.0f, 0.0f, 2.0f, 0.1f, CatalogText.literal(" blocks"));
        dev.echo.client.setting.FloatSetting preparedExpandSize = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Expand Size"), 0.0f, 0.0f, 2.0f, 0.1f, CatalogText.literal(" blocks"));
        dev.echo.client.setting.FloatSetting preparedExpandYDown = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Expand Y Down"), 0.0f, 0.0f, 2.0f, 0.1f, CatalogText.literal(" blocks"));

        HitboxesFeature.assignBootstrapSettings(feature, preparedStaticHitbox, preparedExpandX, preparedUniform, preparedExpandYUp, preparedExpandZ, preparedExpandSize, preparedExpandYDown);

        feature.settings.clear();
        dev.echo.client.features.impl.combat.HitboxesFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
