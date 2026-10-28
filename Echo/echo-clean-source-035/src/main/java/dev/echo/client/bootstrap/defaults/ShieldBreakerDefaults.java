package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.combat.ShieldBreakerFeature;

/** Constructs, assigns, collects and post-initializes Shield Breaker. */
public final class ShieldBreakerDefaults {
    private ShieldBreakerDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(ShieldBreakerFeature feature) {
        dev.echo.client.setting.BooleanSetting preparedUnblockOwnShield = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Unblock Own Shield"), true);
        dev.echo.client.setting.HotbarSlotsSetting preparedHotbarWhitelist = new dev.echo.client.setting.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));
        dev.echo.client.setting.HotbarSlotsSetting preparedReturnSlots = new dev.echo.client.setting.HotbarSlotsSetting(CatalogText.literal("Return Slots"));
        dev.echo.client.setting.BooleanSetting preparedSwapBack = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Swap Back"), false);
        dev.echo.client.setting.ChoiceSetting preparedReturnTo = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Return To"), dev.echo.client.features.impl.combat.ShieldBreakerFeature.bootstrapReturnToOption3(), new java.lang.CharSequence[] {dev.echo.client.features.impl.combat.ShieldBreakerFeature.bootstrapReturnToOption3(), dev.echo.client.features.impl.combat.ShieldBreakerFeature.bootstrapReturnToOption2(), dev.echo.client.features.impl.combat.ShieldBreakerFeature.bootstrapReturnToOption()});
        dev.echo.client.setting.ChoiceSetting preparedMode = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Mode"), dev.echo.client.features.impl.combat.ShieldBreakerFeature.bootstrapModeOption2(), new java.lang.CharSequence[] {dev.echo.client.features.impl.combat.ShieldBreakerFeature.bootstrapModeOption2(), dev.echo.client.features.impl.combat.ShieldBreakerFeature.bootstrapModeOption()});
        dev.echo.client.setting.FloatRangeSetting preparedAttackDelay = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("Attack Delay"), 0.0f, 0.0f, 0.0f, 500.0f, 1.0f, CatalogText.literal("ms"));
        dev.echo.client.setting.BooleanSetting preparedAutoSwap = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Auto Swap"), true);
        dev.echo.client.setting.FloatRangeSetting preparedReactionDelay = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("Reaction Delay"), 0.0f, 0.0f, 0.0f, 500.0f, 1.0f, CatalogText.literal("ms"));
        dev.echo.client.setting.ItemListSetting preparedReturnItems = new dev.echo.client.setting.ItemListSetting(CatalogText.literal("Return Items"));
        dev.echo.client.setting.BooleanSetting preparedStun = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Stun"), false);
        dev.echo.client.setting.FloatRangeSetting preparedSwapBackDelay = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("Swap Back Delay"), 1.0f, 1.0f, 0.0f, 20.0f, 1.0f, CatalogText.literal(" ticks"));
        dev.echo.client.setting.ItemListSetting preparedItemWhitelist = new dev.echo.client.setting.ItemListSetting(CatalogText.literal("Item Whitelist"));

        ShieldBreakerFeature.assignBootstrapSettings(feature, preparedUnblockOwnShield, preparedHotbarWhitelist, preparedReturnSlots, preparedSwapBack, preparedReturnTo, preparedMode, preparedAttackDelay, preparedAutoSwap, preparedReactionDelay, preparedReturnItems, preparedStun, preparedSwapBackDelay, preparedItemWhitelist);

        feature.settings.clear();
        dev.echo.client.features.impl.combat.ShieldBreakerFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
