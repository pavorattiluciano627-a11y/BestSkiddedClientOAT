package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.utility.TotemHandFeature;

/** Constructs, assigns, collects and post-initializes Totem Hand. */
public final class TotemHandDefaults {
    private TotemHandDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(TotemHandFeature feature) {
        dev.echo.client.setting.BooleanSetting preparedSwapBackOnClose = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Swap back on close"), false);
        dev.echo.client.setting.BooleanSetting preparedAnchors = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Anchors"), true);
        dev.echo.client.setting.IntSetting preparedInventoryChance = new dev.echo.client.setting.IntSetting(CatalogText.literal("Inventory chance"), 100, 0, 100, CatalogText.literal("%"));
        dev.echo.client.setting.BooleanSetting preparedPredictive = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Predictive"), false);
        dev.echo.client.setting.BooleanSetting preparedKillable = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Killable"), false);
        dev.echo.client.setting.IntSetting preparedMinDamage = new dev.echo.client.setting.IntSetting(CatalogText.literal("Min damage"), 10, 1, 30, CatalogText.literal(" dmg"));
        dev.echo.client.setting.BooleanSetting preparedCrystals = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Crystals"), true);
        dev.echo.client.setting.BooleanSetting preparedOnPop = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("On pop"), true);
        dev.echo.client.setting.BooleanSetting preparedSwapBackOnSafe = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Swap back on safe"), true);
        dev.echo.client.setting.BooleanSetting preparedOnInventoryOpen = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("On inventory open"), false);

        TotemHandFeature.assignBootstrapSettings(feature, preparedSwapBackOnClose, preparedAnchors, preparedInventoryChance, preparedPredictive, preparedKillable, preparedMinDamage, preparedCrystals, preparedOnPop, preparedSwapBackOnSafe, preparedOnInventoryOpen);

        feature.settings.clear();
        dev.echo.client.features.impl.utility.TotemHandFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
