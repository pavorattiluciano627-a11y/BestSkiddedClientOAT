package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.combat.BacktrackFeature;

/** Constructs, assigns, collects and post-initializes Backtrack. */
public final class BacktrackDefaults {
    private BacktrackDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(BacktrackFeature feature) {
        dev.echo.client.setting.HotbarSlotsSetting preparedHotbarWhitelist = new dev.echo.client.setting.HotbarSlotsSetting(CatalogText.literal("Hotbar Whitelist"));
        dev.echo.client.setting.ColorSetting preparedOutlineColor = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Outline Color"), java.awt.Color.WHITE.getRGB(), true);
        dev.echo.client.setting.IntSetting preparedMaximumHurtTime = new dev.echo.client.setting.IntSetting(CatalogText.literal("Maximum Hurt Time"), 10, 0, 10, CatalogText.literal(" ticks"));
        dev.echo.client.setting.FloatRangeSetting preparedHoldTime = new dev.echo.client.setting.FloatRangeSetting(CatalogText.literal("Hold Time"), 100.0f, 100.0f, 0.0f, 500.0f, 5.0f, CatalogText.literal(" ms"));
        dev.echo.client.setting.ItemListSetting preparedItemWhitelist = new dev.echo.client.setting.ItemListSetting(CatalogText.literal("Item Whitelist"));
        dev.echo.client.setting.ColorSetting preparedFillColor = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Fill Color"), dev.echo.client.ui.ThemePalette.bootstrapForegroundColor().getRGB(), true);

        BacktrackFeature.assignBootstrapSettings(feature, preparedHotbarWhitelist, preparedOutlineColor, preparedMaximumHurtTime, preparedHoldTime, preparedItemWhitelist, preparedFillColor);

        feature.settings.clear();
        dev.echo.client.features.impl.combat.BacktrackFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
