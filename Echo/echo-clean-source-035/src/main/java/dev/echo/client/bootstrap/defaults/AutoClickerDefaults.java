package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.combat.AutoClickerFeature;

/** Constructs, assigns, collects and post-initializes Auto Clicker. */
public final class AutoClickerDefaults {
    private AutoClickerDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(AutoClickerFeature feature) {
        dev.echo.client.setting.HotbarSlotsSetting preparedHotbarWhitelist = new dev.echo.client.setting.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));
        dev.echo.client.setting.BooleanSetting preparedBreakBlocks = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Break Blocks"), false);
        dev.echo.client.setting.ItemListSetting preparedItemWhitelist = new dev.echo.client.setting.ItemListSetting(CatalogText.literal("Item Whitelist"));
        dev.echo.client.setting.BooleanSetting preparedRequireClick = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Require Click"), true);
        dev.echo.client.setting.FloatRangeSetting preparedCps = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("CPS"), 9.0f, 13.0f, 1.0f, 20.0f, 0.5f);
        dev.echo.client.setting.ChoiceSetting preparedRandomization = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Randomization"), dev.echo.client.features.impl.combat.AutoClickerFeature.bootstrapRandomizationOption(), new java.lang.CharSequence[] {dev.echo.client.features.impl.combat.AutoClickerFeature.bootstrapRandomizationOption2(), dev.echo.client.features.impl.combat.AutoClickerFeature.bootstrapRandomizationOption()});

        AutoClickerFeature.assignBootstrapSettings(feature, preparedHotbarWhitelist, preparedBreakBlocks, preparedItemWhitelist, preparedRequireClick, preparedCps, preparedRandomization);

        feature.settings.clear();
        dev.echo.client.features.impl.combat.AutoClickerFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
