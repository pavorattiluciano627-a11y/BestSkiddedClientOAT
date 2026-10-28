package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.internals.TargetsFeature;

/** Constructs, assigns, collects and post-initializes Targets. */
public final class TargetsDefaults {
    private TargetsDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(TargetsFeature feature) {
        dev.echo.client.setting.BooleanSetting preparedAntiBot = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("AntiBot"), false);
        dev.echo.client.setting.FloatSetting preparedKeepTargetRange = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Keep Target Range"), 16.0f, 1.0f, 50.0f, 0.5f, CatalogText.literal(" blocks"));
        dev.echo.client.setting.BooleanSetting preparedIgnoreFriends = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Ignore Friends"), true);
        dev.echo.client.setting.BooleanSetting preparedLastAttacked = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Last Attacked"), false);
        dev.echo.client.setting.IntSetting preparedShieldTicks = new dev.echo.client.setting.IntSetting(CatalogText.literal("Shield Ticks"), 5, 1, 10, CatalogText.literal(" ticks"));
        dev.echo.client.setting.MultiChoiceSetting preparedAntiBotChecks = new dev.echo.client.setting.MultiChoiceSetting(CatalogText.literal("AntiBot Checks"), new java.lang.CharSequence[] {CatalogText.literal("Name"), CatalogText.literal("Movement"), CatalogText.literal("Tab List"), CatalogText.literal("Scale"), CatalogText.literal("Bliss"), CatalogText.literal("Zeta / MMC")});
        dev.echo.client.setting.KeybindSetting preparedManualTargetSelect = new dev.echo.client.setting.KeybindSetting(CatalogText.literal("Manual Target Select"), dev.echo.client.input.InputKey.NONE);
        dev.echo.client.setting.ChoiceSetting preparedShieldDetection = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Shield Detection"), dev.echo.client.features.internals.TargetsFeature.bootstrapShieldDetectionOption(), new java.lang.CharSequence[] {dev.echo.client.features.internals.TargetsFeature.bootstrapShieldDetectionOption(), dev.echo.client.features.internals.TargetsFeature.bootstrapShieldDetectionOption2()});
        dev.echo.client.setting.BooleanSetting preparedIgnoreTeams = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Ignore Teams"), false);
        dev.echo.client.setting.MultiChoiceSetting preparedEntityTypes = new dev.echo.client.setting.MultiChoiceSetting(CatalogText.literal("Entity Types"), new java.lang.CharSequence[] {CatalogText.literal("Players"), CatalogText.literal("Hostile"), CatalogText.literal("Passive"), CatalogText.literal("Neutral"), CatalogText.literal("Armor Stands"), CatalogText.literal("Invisible"), CatalogText.literal("Naked")});
        dev.echo.client.setting.MultiChoiceSetting preparedTeamChecks = new dev.echo.client.setting.MultiChoiceSetting(CatalogText.literal("Team Checks"), new java.lang.CharSequence[] {CatalogText.literal("Color"), CatalogText.literal("Name"), CatalogText.literal("Scoreboard")});
        dev.echo.client.setting.BooleanSetting preparedKeepTarget = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Keep Target"), true);
        dev.echo.client.setting.KeybindSetting preparedFriendBind = new dev.echo.client.setting.KeybindSetting(CatalogText.literal("Friend Bind"), dev.echo.client.input.InputKey.NONE);
        dev.echo.client.setting.BooleanSetting preparedPingCompensation = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Ping Compensation"), false);

        TargetsFeature.assignBootstrapSettings(feature, preparedAntiBot, preparedKeepTargetRange, preparedIgnoreFriends, preparedLastAttacked, preparedShieldTicks, preparedAntiBotChecks, preparedManualTargetSelect, preparedShieldDetection, preparedIgnoreTeams, preparedEntityTypes, preparedTeamChecks, preparedKeepTarget, preparedFriendBind, preparedPingCompensation);

        feature.settings.clear();
        dev.echo.client.features.internals.TargetsFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
