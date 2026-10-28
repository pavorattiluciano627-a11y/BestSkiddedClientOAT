package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.utility.AutoTotemFeature;

/** Constructs, assigns, collects and post-initializes Auto Totem. */
public final class AutoTotemDefaults {
    private AutoTotemDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(AutoTotemFeature feature) {
        dev.echo.client.setting.BooleanSetting preparedHealthSwap = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Health Swap"), false);
        dev.echo.client.setting.ChoiceSetting preparedRestoreItem = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Restore Item"), dev.echo.client.features.impl.utility.AutoTotemFeature.bootstrapRestoreItemOption(), new java.lang.CharSequence[] {dev.echo.client.features.impl.utility.AutoTotemFeature.bootstrapRestoreItemOption(), dev.echo.client.features.impl.utility.AutoTotemFeature.bootstrapRestoreItemOption2()});
        dev.echo.client.setting.IntSetting preparedSlotToRefillTotem = new dev.echo.client.setting.IntSetting(CatalogText.literal("Slot to Refill Totem"), 9, 1, 9);
        dev.echo.client.setting.IntSetting preparedRestoreAbove = new dev.echo.client.setting.IntSetting(CatalogText.literal("Restore Above"), 12, 1, 20, CatalogText.literal(" hp"));
        dev.echo.client.setting.IntSetting preparedEquipBelow = new dev.echo.client.setting.IntSetting(CatalogText.literal("Equip Below"), 8, 1, 19, CatalogText.literal(" hp"));
        dev.echo.client.setting.BooleanSetting preparedRefillHotbar = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Refill Hotbar"), false);

        AutoTotemFeature.assignBootstrapSettings(feature, preparedHealthSwap, preparedRestoreItem, preparedSlotToRefillTotem, preparedRestoreAbove, preparedEquipBelow, preparedRefillHotbar);

        feature.settings.clear();
        dev.echo.client.features.impl.utility.AutoTotemFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
