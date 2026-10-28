package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.combat.SpearReachFeature;

/** Constructs, assigns, collects and post-initializes Spear Reach. */
public final class SpearReachDefaults {
    private SpearReachDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(SpearReachFeature feature) {
        dev.echo.client.setting.BooleanSetting preparedPaperMCSupport = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("PaperMC support"), false);
        dev.echo.client.setting.HotbarSlotsSetting preparedHotbarWhitelist = new dev.echo.client.setting.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));
        dev.echo.client.setting.ItemListSetting preparedItemWhitelist = new dev.echo.client.setting.ItemListSetting(CatalogText.literal("Item Whitelist"));

        SpearReachFeature.assignBootstrapSettings(feature, preparedPaperMCSupport, preparedHotbarWhitelist, preparedItemWhitelist);

        feature.settings.clear();
        dev.echo.client.features.impl.combat.SpearReachFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
