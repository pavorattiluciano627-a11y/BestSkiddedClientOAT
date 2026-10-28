package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.combat.MaceSwapFeature;

/** Constructs, assigns, collects and post-initializes Mace Swap. */
public final class MaceSwapDefaults {
    private MaceSwapDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(MaceSwapFeature feature) {
        dev.echo.client.setting.BooleanSetting preparedSpearDTap = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Spear D-tap"), true);
        dev.echo.client.setting.HotbarSlotsSetting preparedHotbarWhitelist = new dev.echo.client.setting.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));
        dev.echo.client.setting.FloatRangeSetting preparedStunSwitchDelay = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("Stun Switch Delay"), 1.0f, 1.0f, 1.0f, 20.0f, 1.0f, CatalogText.literal(" Ticks"));
        dev.echo.client.setting.ItemListSetting preparedReturnItems = new dev.echo.client.setting.ItemListSetting(CatalogText.literal("Return Items"));
        dev.echo.client.setting.FloatSetting preparedMinimumDamage = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Minimum Damage"), 10.0f, 0.0f, 35.0f, 1.0f, CatalogText.literal(" Dmg"));
        dev.echo.client.setting.ChoiceSetting preparedReturnTo = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Return To"), CatalogText.literal("Original"), new java.lang.CharSequence[] {CatalogText.literal("Original"), CatalogText.literal("Slot"), CatalogText.literal("Item")});
        dev.echo.client.setting.HotbarSlotsSetting preparedReturnSlots = new dev.echo.client.setting.HotbarSlotsSetting(CatalogText.literal("Return Slots"));
        dev.echo.client.setting.ItemListSetting preparedItemWhitelist = new dev.echo.client.setting.ItemListSetting(CatalogText.literal("Item Whitelist"));
        dev.echo.client.setting.BooleanSetting preparedStunSlam = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Stun Slam"), true);
        dev.echo.client.setting.FloatRangeSetting preparedDelay = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("Delay"), 1.0f, 1.0f, 0.0f, 20.0f, 1.0f, CatalogText.literal(" Ticks"));

        MaceSwapFeature.assignBootstrapSettings(feature, preparedSpearDTap, preparedHotbarWhitelist, preparedStunSwitchDelay, preparedReturnItems, preparedMinimumDamage, preparedReturnTo, preparedReturnSlots, preparedItemWhitelist, preparedStunSlam, preparedDelay);

        feature.settings.clear();
        dev.echo.client.features.impl.combat.MaceSwapFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
