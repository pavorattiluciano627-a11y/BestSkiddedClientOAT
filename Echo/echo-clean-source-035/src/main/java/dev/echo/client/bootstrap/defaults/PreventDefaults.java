package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.internals.PreventFeature;

/** Constructs, assigns, collects and post-initializes Prevent. */
public final class PreventDefaults {
    private PreventDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(PreventFeature feature) {
        dev.echo.client.setting.ItemListSetting preparedPreventWithItemList = new dev.echo.client.setting.ItemListSetting(CatalogText.literal("Prevent With"));
        dev.echo.client.setting.BooleanSetting preparedNoAirHits = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("No Air Hits"), false);
        dev.echo.client.setting.BooleanSetting preparedNoShieldWithBlocks = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("No Shield With Blocks"), false);
        dev.echo.client.setting.BooleanSetting preparedInHotbar = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("In Hotbar"), false);
        dev.echo.client.setting.BooleanSetting preparedUntotem = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Untotem"), false);
        dev.echo.client.setting.BooleanSetting preparedOnObsidian = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("On Obsidian"), true);
        dev.echo.client.setting.ItemListSetting preparedPreventWithItemList2 = new dev.echo.client.setting.ItemListSetting(CatalogText.literal("Prevent With"));
        dev.echo.client.setting.BooleanSetting preparedNoMining = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("No Mining"), false);
        dev.echo.client.setting.BooleanSetting preparedEChestOpen = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("EChest Open"), false);
        dev.echo.client.setting.BooleanSetting preparedInInventory = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("In Inventory"), true);
        dev.echo.client.setting.BooleanSetting preparedAnchorCharge = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Anchor Charge"), true);

        PreventFeature.assignBootstrapSettings(feature, preparedPreventWithItemList, preparedNoAirHits, preparedNoShieldWithBlocks, preparedInHotbar, preparedUntotem, preparedOnObsidian, preparedPreventWithItemList2, preparedNoMining, preparedEChestOpen, preparedInInventory, preparedAnchorCharge);

        feature.settings.clear();
        dev.echo.client.features.impl.internals.PreventFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
