package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.render.StorageESPFeature;

/** Constructs, assigns, collects and post-initializes Storage ESP. */
public final class StorageESPDefaults {
    private StorageESPDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(StorageESPFeature feature) {
        dev.echo.client.setting.ColorSetting preparedOtherStorageColor = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Other Storage Color"), 128, 128, 128, 200, true);
        dev.echo.client.setting.ColorSetting preparedShulkerColor = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Shulker Color"), 147, 112, 219, 200, true);
        dev.echo.client.setting.ColorSetting preparedBarrelColor = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Barrel Color"), 139, 69, 19, 200, true);
        dev.echo.client.setting.BooleanSetting preparedOutline = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Outline"), true);
        dev.echo.client.setting.ColorSetting preparedEnderChestColor = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Ender Chest Color"), 0, 0, 255, 200, true);
        dev.echo.client.setting.BooleanSetting preparedTracers = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Tracers"), false);
        dev.echo.client.setting.ColorSetting preparedChestColor = new dev.echo.client.setting.ColorSetting(CatalogText.literal("Chest Color"), 255, 165, 0, 200, true);

        StorageESPFeature.assignBootstrapSettings(feature, preparedOtherStorageColor, preparedShulkerColor, preparedBarrelColor, preparedOutline, preparedEnderChestColor, preparedTracers, preparedChestColor);

        feature.settings.clear();
        dev.echo.client.features.impl.render.StorageESPFeature.appendBootstrapSettings(feature);
        feature.postInitialize();
    }
}
