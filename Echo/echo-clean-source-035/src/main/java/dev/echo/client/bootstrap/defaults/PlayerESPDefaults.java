package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.render.PlayerESPFeature;

/** Constructs, assigns, collects and post-initializes Player ESP. */
public final class PlayerESPDefaults {
    private PlayerESPDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(PlayerESPFeature feature) {
        dev.echo.client.setting.ChoiceSetting preparedHealthColor = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Health Color"), CatalogText.literal("Dynamic"), new java.lang.CharSequence[] {CatalogText.literal("Dynamic"), CatalogText.literal("Static")});
        dev.echo.client.setting.ColorSetting preparedColor = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Color"), 2139881472);
        dev.echo.client.setting.BooleanSetting preparedTeamColor = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Team Color"), false);
        dev.echo.client.setting.ColorSetting preparedFullHealthColor = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Full Health Color"), -13899678);
        dev.echo.client.setting.BooleanSetting preparedRenderSelf = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Render Self"), true);
        dev.echo.client.setting.BooleanSetting preparedShowName = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Show Name"), false);
        dev.echo.client.setting.ColorSetting preparedHealthBarColor = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Health Bar Color"), -13899678);
        dev.echo.client.setting.BooleanSetting preparedShowHeldItem = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Show Held Item"), false);
        dev.echo.client.setting.BooleanSetting preparedFill = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Fill"), false);
        dev.echo.client.setting.BooleanSetting preparedTracers = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Tracers"), true);
        dev.echo.client.setting.ColorSetting preparedLowHealthColor = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Low Health Color"), -1692114);
        dev.echo.client.setting.MultiChoiceSetting preparedModes = new dev.echo.client.setting.MultiChoiceSetting(CatalogText.literal("Modes"), new java.lang.CharSequence[] {CatalogText.literal("Box"), CatalogText.literal("2D")});
        dev.echo.client.setting.ChoiceSetting preparedHealthBar = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Health Bar"), CatalogText.literal("Left"), new java.lang.CharSequence[] {CatalogText.literal("None"), CatalogText.literal("Left"), CatalogText.literal("Right"), CatalogText.literal("Bottom")});

        PlayerESPFeature.assignBootstrapSettings(feature, preparedHealthColor, preparedColor, preparedTeamColor, preparedFullHealthColor, preparedRenderSelf, preparedShowName, preparedHealthBarColor, preparedShowHeldItem, preparedFill, preparedTracers, preparedLowHealthColor, preparedModes, preparedHealthBar);

        feature.settings.clear();
        dev.echo.client.features.impl.render.PlayerESPFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
