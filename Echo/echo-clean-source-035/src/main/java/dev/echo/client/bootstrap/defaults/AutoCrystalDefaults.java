package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.combat.AutoCrystalFeature;

/** Constructs, assigns, collects and post-initializes Auto Crystal. */
public final class AutoCrystalDefaults {
    private AutoCrystalDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(AutoCrystalFeature feature) {
        dev.echo.client.setting.FloatRangeSetting preparedPlaceDelay = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("Place Delay"), 1.0f, 1.0f, 0.0f, 20.0f, 1.0f, CatalogText.literal(" ticks"));
        dev.echo.client.setting.FloatRangeSetting preparedPlaceObsidianDelay = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("Place Obsidian Delay"), 1.0f, 1.0f, 0.0f, 10.0f, 1.0f, CatalogText.literal(" ticks"));
        dev.echo.client.setting.BooleanSetting preparedSwitchBack = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Switch Back"), true);
        dev.echo.client.setting.BooleanSetting preparedFakePunch = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Fake Punch"), false);
        dev.echo.client.setting.FloatRangeSetting preparedBreakDelay = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("Break Delay"), 1.0f, 1.0f, 0.0f, 20.0f, 1.0f, CatalogText.literal(" ticks"));
        dev.echo.client.setting.FloatRangeSetting preparedSwitchDelay = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("Switch Delay"), 1.0f, 1.0f, 0.0f, 10.0f, 1.0f, CatalogText.literal(" ticks"));
        dev.echo.client.setting.BooleanSetting preparedPlaceObsidian = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Place Obsidian"), false);
        dev.echo.client.setting.HotbarSlotsSetting preparedHotbarWhitelist = new dev.echo.client.setting.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));
        dev.echo.client.setting.ItemListSetting preparedItemWhitelist = new dev.echo.client.setting.ItemListSetting(CatalogText.literal("Item Whitelist"));
        dev.echo.client.setting.HotbarSlotsSetting preparedReturnSlots = new dev.echo.client.setting.HotbarSlotsSetting(CatalogText.literal("Return Slots"));
        dev.echo.client.setting.IntSetting preparedFailChance = new dev.echo.client.setting.IntSetting(CatalogText.literal("Fail Chance"), 0, 0, 100, CatalogText.literal("%"));
        dev.echo.client.setting.ChoiceSetting preparedReturnTo = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Return To"), dev.echo.client.features.impl.combat.AutoCrystalFeature.bootstrapReturnToOption3(), new java.lang.CharSequence[] {dev.echo.client.features.impl.combat.AutoCrystalFeature.bootstrapReturnToOption3(), dev.echo.client.features.impl.combat.AutoCrystalFeature.bootstrapReturnToOption(), dev.echo.client.features.impl.combat.AutoCrystalFeature.bootstrapReturnToOption2()});
        dev.echo.client.setting.KeybindSetting preparedActivateKey = new dev.echo.client.setting.KeybindSetting(CatalogText.literal("Activate Key"), dev.echo.client.input.InputKey.MOUSE_RIGHT);
        dev.echo.client.setting.ItemListSetting preparedReturnItems = new dev.echo.client.setting.ItemListSetting(CatalogText.literal("Return Items"));
        dev.echo.client.setting.BooleanSetting preparedStopOnKill = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Stop on kill"), false);

        AutoCrystalFeature.assignBootstrapSettings(feature, preparedPlaceDelay, preparedPlaceObsidianDelay, preparedSwitchBack, preparedFakePunch, preparedBreakDelay, preparedSwitchDelay, preparedPlaceObsidian, preparedHotbarWhitelist, preparedItemWhitelist, preparedReturnSlots, preparedFailChance, preparedReturnTo, preparedActivateKey, preparedReturnItems, preparedStopOnKill);

        feature.settings.clear();
        dev.echo.client.features.impl.combat.AutoCrystalFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
