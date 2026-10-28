package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.combat.TriggerBotFeature;

/** Constructs, assigns, collects and post-initializes Trigger Bot. */
public final class TriggerBotDefaults {
    private TriggerBotDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(TriggerBotFeature feature) {
        dev.echo.client.setting.FloatRangeSetting preparedCooldown = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("Cooldown"), 90.0f, 95.0f, 0.0f, 100.0f, 1.0f);
        dev.echo.client.setting.ChoiceSetting preparedCritMode = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Crit Mode"), CatalogText.literal("pCrit"), new java.lang.CharSequence[] {CatalogText.literal("pCrit"), CatalogText.literal("Normal"), CatalogText.literal("None")});
        dev.echo.client.setting.FloatRangeSetting preparedReactionDelay = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("Reaction Delay"), 0.0f, 0.0f, 0.0f, 500.0f, 1.0f);
        dev.echo.client.setting.BooleanSetting preparedCritOnlyOnSpace = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Crit only on space"), false);
        dev.echo.client.setting.FloatRangeSetting preparedAttackDelay = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("Attack Delay"), 0.0f, 0.0f, 0.0f, 800.0f, 1.0f);
        dev.echo.client.setting.ItemListSetting preparedItemWhitelist = new dev.echo.client.setting.ItemListSetting(CatalogText.literal("Item Whitelist"));
        dev.echo.client.setting.FloatSetting preparedAttackDistance = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Attack Distance"), 0.0f, 0.0f, 6.0f, 0.01f, CatalogText.literal(" blocks"));
        dev.echo.client.setting.BooleanSetting preparedNoSweepOrPickHits = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("No Sweep or Pick Hits"), false);
        dev.echo.client.setting.HotbarSlotsSetting preparedHotbarWhitelist = new dev.echo.client.setting.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));
        dev.echo.client.setting.BooleanSetting preparedHurtTimeCheck = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("HurtTime Check"), true);
        dev.echo.client.setting.BooleanSetting preparedAttackShields = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Attack Shields"), true);
        dev.echo.client.setting.FloatRangeSetting preparedCritRange = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("Crit Range"), 0.0f, 6.0f, 0.0f, 6.0f, 0.1f);
        dev.echo.client.setting.FloatRangeSetting preparedCritCooldown = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("Crit Cooldown"), 90.0f, 100.0f, 0.0f, 100.0f, 1.0f);

        TriggerBotFeature.assignBootstrapSettings(feature, preparedCooldown, preparedCritMode, preparedReactionDelay, preparedCritOnlyOnSpace, preparedAttackDelay, preparedItemWhitelist, preparedAttackDistance, preparedNoSweepOrPickHits, preparedHotbarWhitelist, preparedHurtTimeCheck, preparedAttackShields, preparedCritRange, preparedCritCooldown);

        feature.settings.clear();
        dev.echo.client.features.impl.combat.TriggerBotFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
