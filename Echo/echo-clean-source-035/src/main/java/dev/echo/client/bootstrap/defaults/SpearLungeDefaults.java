package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.combat.SpearLungeFeature;

/** Constructs, assigns, collects and post-initializes Spear Lunge. */
public final class SpearLungeDefaults {
    private SpearLungeDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(SpearLungeFeature feature) {
        dev.echo.client.setting.ChoiceSetting preparedCooldown = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Cooldown"), CatalogText.literal("Vanilla"), new java.lang.CharSequence[] {CatalogText.literal("Vanilla"), CatalogText.literal("Paper")});
        dev.echo.client.setting.KeybindSetting preparedActivateKey = new dev.echo.client.setting.KeybindSetting(CatalogText.literal("Activate Key"), dev.echo.client.input.InputKey.NONE);
        dev.echo.client.setting.ChoiceSetting preparedMode = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Mode"), CatalogText.literal("Attack"), new java.lang.CharSequence[] {CatalogText.literal("Attack"), CatalogText.literal("Keypress")});
        dev.echo.client.setting.ItemListSetting preparedReturnItems = new dev.echo.client.setting.ItemListSetting(CatalogText.literal("Return Items"));
        dev.echo.client.setting.IntSetting preparedCooldownPercent = new dev.echo.client.setting.IntSetting(CatalogText.literal("Cooldown %"), 85, 0, 100);
        dev.echo.client.setting.HotbarSlotsSetting preparedHotbarWhitelist = new dev.echo.client.setting.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));
        dev.echo.client.setting.ItemListSetting preparedItemWhitelist = new dev.echo.client.setting.ItemListSetting(CatalogText.literal("Item Whitelist"));
        dev.echo.client.setting.BooleanSetting preparedSilent = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Silent"), false);
        dev.echo.client.setting.HotbarSlotsSetting preparedReturnSlots = new dev.echo.client.setting.HotbarSlotsSetting(CatalogText.literal("Return Slots"));
        dev.echo.client.setting.ChoiceSetting preparedReturnTo = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Return To"), CatalogText.literal("Original"), new java.lang.CharSequence[] {CatalogText.literal("Original"), CatalogText.literal("Slot"), CatalogText.literal("Item")});

        SpearLungeFeature.assignBootstrapSettings(feature, preparedCooldown, preparedActivateKey, preparedMode, preparedReturnItems, preparedCooldownPercent, preparedHotbarWhitelist, preparedItemWhitelist, preparedSilent, preparedReturnSlots, preparedReturnTo);

        feature.settings.clear();
        dev.echo.client.features.impl.combat.SpearLungeFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
