package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.render.ItemESPFeature;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ColorSetting;
import dev.echo.client.setting.ItemListSetting;
import dev.echo.client.setting.MultiChoiceSetting;

/** Original construction vector; the preview setting was already created by class initialization. */
public final class ItemESPDefaults {
    private ItemESPDefaults() {
    }

    public static void initialize(ItemESPFeature feature) {
        ItemListSetting preparedWhitelist = new ItemListSetting(CatalogText.literal("Whitelist"));
        BooleanSetting preparedFill = new BooleanSetting(CatalogText.literal("Fill"), false);
        MultiChoiceSetting preparedModes = new MultiChoiceSetting(CatalogText.literal("Modes"),
                new CharSequence[]{CatalogText.literal("Box"), CatalogText.literal("2D")});
        BooleanSetting preparedShowName = new BooleanSetting(CatalogText.literal("Show Name"), true);
        BooleanSetting preparedTracers = new BooleanSetting(CatalogText.literal("Tracers"), false);
        ColorSetting preparedColor = new ColorSetting(CatalogText.literal("Color"), 2145955883);
        ItemESPFeature.assignBootstrapSettings(feature, preparedWhitelist, preparedFill, preparedModes,
                preparedShowName, preparedTracers, preparedColor);
        feature.settings.clear();
        ItemESPFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
